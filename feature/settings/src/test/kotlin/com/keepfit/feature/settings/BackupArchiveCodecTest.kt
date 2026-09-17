package com.keepfit.feature.settings

import com.keepfit.feature.settings.data.BackupArchiveCodec
import com.keepfit.feature.settings.data.BackupArchiveSource
import com.keepfit.feature.settings.data.BackupSettingsSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveCodecTest {
    private val codec = BackupArchiveCodec()

    @Test
    fun roundTripsEncryptedArchive() {
        val workingDirectory = createTempDirectory("keepfit-backup-test").toFile()
        try {
            val databaseFile = File(workingDirectory, "database.sqlite").apply {
                writeText("sqlite-payload")
            }
            val mediaRoot = File(workingDirectory, "media").apply { mkdirs() }
            File(mediaRoot, "exercises/demo.gif").apply {
                parentFile?.mkdirs()
                writeText("gif-payload")
            }
            val output = ByteArrayOutputStream()

            codec.writeEncryptedArchive(
                passphrase = "long-secret",
                source = BackupArchiveSource(
                    databaseFile = databaseFile,
                    settingsSnapshot = sampleSettings(),
                    mediaRootDirectory = mediaRoot,
                    databaseSchemaVersion = 4,
                    recordCounts = linkedMapOf("foods" to 2, "workoutSessions" to 5),
                ),
                workingDirectory = File(workingDirectory, "export").apply { mkdirs() },
                outputStream = output,
            )

            val extracted = codec.extractValidatedArchive(
                passphrase = "long-secret",
                inputStream = ByteArrayInputStream(output.toByteArray()),
                workingDirectory = File(workingDirectory, "restore").apply { mkdirs() },
            )

            assertEquals(4, extracted.manifest.databaseSchemaVersion)
            assertEquals(2, extracted.manifest.recordCounts["foods"])
            assertEquals("sqlite-payload", extracted.databaseFile.readText())
            assertEquals("gif-payload", File(extracted.mediaDirectory, "exercises/demo.gif").readText())
            assertEquals("KG", extracted.settingsSnapshot.weightUnit)
            assertTrue(extracted.settingsSnapshot.reduceMotion)
            assertTrue(extracted.manifest.files.any { it.relativePath == "media/exercises/demo.gif" })
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    @Test
    fun rejectsWrongPassphrase() {
        val workingDirectory = createTempDirectory("keepfit-backup-test").toFile()
        try {
            val databaseFile = File(workingDirectory, "database.sqlite").apply {
                writeText("sqlite-payload")
            }
            val output = ByteArrayOutputStream()

            codec.writeEncryptedArchive(
                passphrase = "long-secret",
                source = BackupArchiveSource(
                    databaseFile = databaseFile,
                    settingsSnapshot = sampleSettings(),
                    mediaRootDirectory = null,
                    databaseSchemaVersion = 4,
                    recordCounts = linkedMapOf("foods" to 2),
                ),
                workingDirectory = File(workingDirectory, "export").apply { mkdirs() },
                outputStream = output,
            )

            val result = runCatching {
                codec.extractValidatedArchive(
                    passphrase = "wrong-secret",
                    inputStream = ByteArrayInputStream(output.toByteArray()),
                    workingDirectory = File(workingDirectory, "restore").apply { mkdirs() },
                )
            }

            assertEquals(
                "The backup passphrase is incorrect or the file is corrupted.",
                result.exceptionOrNull()?.message,
            )
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    @Test
    fun rejectsTruncatedArchiveWithClearMessage() {
        val workingDirectory = createTempDirectory("keepfit-backup-test").toFile()
        try {
            val result = runCatching {
                codec.extractValidatedArchive(
                    passphrase = "long-secret",
                    inputStream = ByteArrayInputStream(byteArrayOf()),
                    workingDirectory = File(workingDirectory, "restore").apply { mkdirs() },
                )
            }

            assertEquals(
                "The backup file is incomplete or corrupted.",
                result.exceptionOrNull()?.message,
            )
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    private fun sampleSettings() = BackupSettingsSnapshot(
        weightUnit = "KG",
        measurementUnit = "CM",
        restTimerSeconds = 90,
        weeklyReviewPaused = true,
        reduceMotion = true,
        workoutReminderEnabled = true,
        workoutReminderHour = 18,
        workoutReminderMinute = 30,
        transformationReminderEnabled = true,
        transformationReminderDayOfWeek = 7,
        transformationReminderHour = 9,
        transformationReminderMinute = 0,
    )
}
