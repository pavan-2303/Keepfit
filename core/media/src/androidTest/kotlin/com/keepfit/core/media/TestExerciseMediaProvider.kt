package com.keepfit.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class TestExerciseMediaProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri): String? = when (uri.lastPathSegment) {
        "demo.gif" -> "image/gif"
        "demo.txt" -> "text/plain"
        "photo.jpg" -> "image/jpeg"
        else -> null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = File(requireContext().cacheDir, requireNotNull(uri.lastPathSegment))
        if (uri.lastPathSegment == "photo.jpg") {
            Bitmap.createBitmap(3200, 1600, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(Color.rgb(40, 120, 180))
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                bitmap.recycle()
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "12/1,20/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "77/1,35/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
                setAttribute(ExifInterface.TAG_MODEL, "Private test phone")
                saveAttributes()
            }
        } else {
            file.writeBytes("GIF89a".toByteArray())
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
