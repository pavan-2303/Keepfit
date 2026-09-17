package com.keepfit.core.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

data class ImportedExerciseMedia(
    val id: String,
    val relativePath: String,
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long,
)

class ExerciseMediaStore(
    private val context: Context,
) {
    fun import(uri: Uri): ImportedExerciseMedia {
        val resolver = context.contentResolver
        val mimeType = requireNotNull(resolver.getType(uri)) {
            "The selected file type could not be detected."
        }
        require(mimeType in supportedMimeTypes) {
            "Choose an MP4, WebM, or GIF demonstration."
        }

        val extension = when (mimeType) {
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "image/gif" -> "gif"
            else -> error("Unsupported media type.")
        }
        val id = UUID.randomUUID().toString()
        val relativePath = "media/exercises/$id.$extension"
        val destination = File(context.filesDir, relativePath)
        destination.parentFile?.mkdirs()

        try {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "The selected file could not be opened." }
                destination.outputStream().use(input::copyTo)
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        }

        return ImportedExerciseMedia(
            id = id,
            relativePath = relativePath,
            mediaType = if (mimeType == "image/gif") "GIF" else "VIDEO",
            mimeType = mimeType,
            sizeBytes = destination.length(),
        )
    }

    fun delete(relativePath: String) {
        File(context.filesDir, relativePath).delete()
    }

    fun resolve(relativePath: String): Uri? {
        val root = context.filesDir.canonicalFile
        val candidate = runCatching { File(root, relativePath).canonicalFile }.getOrNull() ?: return null
        if (!candidate.toPath().startsWith(root.toPath()) || !candidate.isFile) return null
        return Uri.fromFile(candidate)
    }

    private companion object {
        val supportedMimeTypes = setOf("video/mp4", "video/webm", "image/gif")
    }
}
