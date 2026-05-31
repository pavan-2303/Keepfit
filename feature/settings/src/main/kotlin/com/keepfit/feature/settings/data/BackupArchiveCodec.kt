package com.keepfit.feature.settings.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupArchiveCodec(
    private val gson: Gson = Gson(),
) {
    internal fun writeEncryptedArchive(
        passphrase: String,
        source: BackupArchiveSource,
        workingDirectory: File,
        outputStream: OutputStream,
    ) {
        val tempZip = File(workingDirectory, "keepfit-backup.zip")
        val settingsBytes = gson.toJson(source.settingsSnapshot).toByteArray(StandardCharsets.UTF_8)
        val mediaFiles = source.mediaRootDirectory
            ?.takeIf(File::exists)
            ?.walkTopDown()
            ?.filter(File::isFile)
            ?.toList()
            .orEmpty()
        val manifestFiles = buildList {
            add(source.databaseFile.toManifestEntry(DATABASE_ENTRY))
            add(bytesToManifestEntry(SETTINGS_ENTRY, settingsBytes))
            mediaFiles.forEach { file ->
                val relativePath = file.relativeTo(source.mediaRootDirectory!!).invariantSeparatorsPath
                add(file.toManifestEntry("$MEDIA_DIRECTORY/$relativePath"))
            }
        }
        val manifest = BackupManifest(
            formatVersion = BACKUP_MANIFEST_VERSION,
            exportedAtUtcEpochMillis = System.currentTimeMillis(),
            databaseSchemaVersion = source.databaseSchemaVersion,
            recordCounts = source.recordCounts.toSortedMap(),
            mediaSizeBytes = mediaFiles.sumOf(File::length),
            files = manifestFiles.sortedBy { it.relativePath },
        )

        ZipOutputStream(tempZip.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(gson.toJson(manifest).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            source.databaseFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            zip.write(settingsBytes)
            zip.closeEntry()

            mediaFiles.sortedBy { it.invariantSeparatorsPath }.forEach { file ->
                val relativePath = file.relativeTo(source.mediaRootDirectory!!).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry("$MEDIA_DIRECTORY/$relativePath"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        encryptZip(tempZip, passphrase, outputStream)
        tempZip.delete()
    }

    internal fun extractValidatedArchive(
        passphrase: String,
        inputStream: InputStream,
        workingDirectory: File,
    ): ExtractedBackup {
        val tempZip = File(workingDirectory, "keepfit-backup.zip")
        val extractedDirectory = File(workingDirectory, "archive")

        decryptToZip(inputStream, passphrase, tempZip)
        unzip(tempZip, extractedDirectory)
        tempZip.delete()

        val manifestFile = File(extractedDirectory, MANIFEST_ENTRY)
        val databaseFile = File(extractedDirectory, DATABASE_ENTRY)
        val settingsFile = File(extractedDirectory, SETTINGS_ENTRY)

        require(manifestFile.exists()) { "The backup manifest is missing." }
        require(databaseFile.exists()) { "The backup database is missing." }
        require(settingsFile.exists()) { "The backup settings snapshot is missing." }

        val manifest = try {
            gson.fromJson(manifestFile.readText(StandardCharsets.UTF_8), BackupManifest::class.java)
        } catch (error: JsonSyntaxException) {
            throw IllegalArgumentException("The backup manifest is invalid.", error)
        }
        require(manifest.formatVersion == BACKUP_MANIFEST_VERSION) {
            "This backup format version is not supported."
        }

        val settingsSnapshot = try {
            gson.fromJson(settingsFile.readText(StandardCharsets.UTF_8), BackupSettingsSnapshot::class.java)
        } catch (error: JsonSyntaxException) {
            throw IllegalArgumentException("The backup settings snapshot is invalid.", error)
        }

        val expectedPaths = buildSet {
            add(DATABASE_ENTRY)
            add(SETTINGS_ENTRY)
            manifest.files.forEach { add(it.relativePath) }
        }

        val actualFiles = extractedDirectory.walkTopDown()
            .filter(File::isFile)
            .map { it.relativeTo(extractedDirectory).invariantSeparatorsPath }
            .filter { it != MANIFEST_ENTRY }
            .toSet()
        require(actualFiles == expectedPaths) {
            "The backup contents do not match the manifest."
        }

        manifest.files.forEach { entry ->
            val file = File(extractedDirectory, entry.relativePath)
            require(file.exists()) { "The backup file ${entry.relativePath} is missing." }
            require(file.length() == entry.sizeBytes) {
                "The backup file ${entry.relativePath} has the wrong size."
            }
            require(file.sha256() == entry.sha256) {
                "The backup file ${entry.relativePath} failed checksum validation."
            }
        }

        return ExtractedBackup(
            rootDirectory = extractedDirectory,
            manifest = manifest,
            settingsSnapshot = settingsSnapshot,
        )
    }

    private fun encryptZip(tempZip: File, passphrase: String, outputStream: OutputStream) {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveSecretKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))

        DataOutputStream(outputStream.buffered()).use { dataOutput ->
            dataOutput.write(MAGIC_BYTES)
            dataOutput.writeInt(ENCRYPTION_VERSION)
            dataOutput.writeInt(PBKDF2_ITERATIONS)
            dataOutput.write(salt)
            dataOutput.write(iv)
            javax.crypto.CipherOutputStream(dataOutput, cipher).use { cipherOutput ->
                tempZip.inputStream().buffered().use { input -> input.copyTo(cipherOutput) }
            }
        }
    }

    private fun decryptToZip(
        inputStream: InputStream,
        passphrase: String,
        destinationZip: File,
    ) {
        DataInputStream(inputStream.buffered()).use { dataInput ->
            val magic = ByteArray(MAGIC_BYTES.size)
            dataInput.readFully(magic)
            require(magic.contentEquals(MAGIC_BYTES)) { "The selected file is not a Keepfit backup." }
            val encryptionVersion = dataInput.readInt()
            require(encryptionVersion == ENCRYPTION_VERSION) { "This backup encryption version is not supported." }
            val iterations = dataInput.readInt()
            val salt = ByteArray(SALT_BYTES).also(dataInput::readFully)
            val iv = ByteArray(IV_BYTES).also(dataInput::readFully)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                deriveSecretKey(passphrase, salt, iterations),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )

            try {
                javax.crypto.CipherInputStream(dataInput, cipher).use { cipherInput ->
                    destinationZip.outputStream().buffered().use { output -> cipherInput.copyTo(output) }
                }
            } catch (error: Exception) {
                destinationZip.delete()
                throw IllegalArgumentException(
                    "The backup passphrase is incorrect or the file is corrupted.",
                    error,
                )
            }
        }
    }

    private fun unzip(zipFile: File, destinationDirectory: File) {
        destinationDirectory.mkdirs()
        val destinationRoot = destinationDirectory.canonicalFile

        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val outputFile = File(destinationDirectory, entry.name)
                val canonicalOutput = outputFile.canonicalFile
                require(
                    canonicalOutput.path == destinationRoot.path ||
                        canonicalOutput.path.startsWith(destinationRoot.path + File.separator),
                ) {
                    "The backup contains an invalid path."
                }

                if (entry.isDirectory) {
                    canonicalOutput.mkdirs()
                } else {
                    canonicalOutput.parentFile?.mkdirs()
                    canonicalOutput.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun deriveSecretKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS,
    ): SecretKeySpec {
        val keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = keyFactory.generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)).encoded
        return SecretKeySpec(keyBytes, SECRET_KEY_ALGORITHM)
    }

    private fun File.toManifestEntry(relativePath: String): BackupFileManifestEntry =
        BackupFileManifestEntry(
            relativePath = relativePath,
            sha256 = sha256(),
            sizeBytes = length(),
        )

    private fun bytesToManifestEntry(relativePath: String, bytes: ByteArray): BackupFileManifestEntry =
        BackupFileManifestEntry(
            relativePath = relativePath,
            sha256 = bytes.sha256(),
            sizeBytes = bytes.size.toLong(),
        )

    companion object {
        internal const val BACKUP_MANIFEST_VERSION = 1
        internal const val MANIFEST_ENTRY = "manifest.json"
        internal const val DATABASE_ENTRY = "database.sqlite"
        internal const val SETTINGS_ENTRY = "settings.json"
        internal const val MEDIA_DIRECTORY = "media"

        private const val ENCRYPTION_VERSION = 1
        private const val SECRET_KEY_ALGORITHM = "AES"
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val PBKDF2_ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private val MAGIC_BYTES = "KFITBAK1".toByteArray(StandardCharsets.US_ASCII)
        private val secureRandom = SecureRandom()
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString(separator = "") { "%02x".format(it) }
}

private fun ByteArray.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(this)
    return digest.digest().joinToString(separator = "") { "%02x".format(it) }
}
