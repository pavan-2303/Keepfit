package com.keepfit.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID

data class ImportedTransformationPhoto(
    val id: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
)

class TransformationPhotoStore(
    private val context: Context,
) {
    fun import(cycleId: String, uri: Uri): ImportedTransformationPhoto {
        val resolver = context.contentResolver
        val mimeType = requireNotNull(resolver.getType(uri)) {
            "The selected photo type could not be detected."
        }
        require(mimeType in supportedMimeTypes) {
            "Choose a JPEG, PNG, or WebP photo."
        }

        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> error("Unsupported photo type.")
        }
        val id = UUID.randomUUID().toString()
        val relativePath = "media/transformation/$cycleId/$id.$extension"
        val destination = File(context.filesDir, relativePath)
        val source = File.createTempFile("transformation-import-", ".$extension", context.cacheDir)
        destination.parentFile?.mkdirs()
        val processedBitmaps = mutableListOf<Bitmap>()

        try {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "The selected photo could not be opened." }
                source.outputStream().use(input::copyTo)
            }
            val orientation = runCatching {
                ExifInterface(source.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val decoded = decodeBounded(source).also(processedBitmaps::add)
            val oriented = decoded.applyOrientation(orientation).also(processedBitmaps::add)
            val bounded = oriented.scaleToMaximumEdge(MAXIMUM_EDGE_PIXELS).also(processedBitmaps::add)
            destination.outputStream().use { output ->
                require(bounded.compress(mimeType.compressFormat(), compressionQuality(mimeType), output)) {
                    "The selected photo could not be processed."
                }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            processedBitmaps
                .distinctBy { System.identityHashCode(it) }
                .forEach(Bitmap::recycle)
            source.delete()
        }

        return ImportedTransformationPhoto(
            id = id,
            relativePath = relativePath,
            mimeType = mimeType,
            sizeBytes = destination.length(),
        )
    }

    fun resolve(relativePath: String): String? {
        val root = context.filesDir.canonicalFile
        val candidate = runCatching { File(root, relativePath).canonicalFile }.getOrNull() ?: return null
        if (!candidate.toPath().startsWith(root.toPath()) || !candidate.isFile) return null
        return candidate.absolutePath
    }

    fun delete(relativePath: String) {
        File(context.filesDir, relativePath).delete()
    }

    private companion object {
        const val MAXIMUM_EDGE_PIXELS = 2048
        val supportedMimeTypes = setOf("image/jpeg", "image/png", "image/webp")
    }
}

private fun decodeBounded(source: File): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected photo is not a readable image." }
    var sampleSize = 1
    while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 4096) {
        sampleSize *= 2
    }
    return requireNotNull(
        BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ),
    ) { "The selected photo is not a readable image." }
}

private fun Bitmap.applyOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.scaleToMaximumEdge(maximumEdge: Int): Bitmap {
    val currentMaximum = maxOf(width, height)
    if (currentMaximum <= maximumEdge) return this
    val scale = maximumEdge.toFloat() / currentMaximum
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private fun String.compressFormat(): Bitmap.CompressFormat = when (this) {
    "image/jpeg" -> Bitmap.CompressFormat.JPEG
    "image/png" -> Bitmap.CompressFormat.PNG
    "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSY
    else -> error("Unsupported photo type.")
}

private fun compressionQuality(mimeType: String): Int = if (mimeType == "image/png") 100 else 92
