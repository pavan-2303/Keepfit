package com.keepfit.core.media

import android.net.Uri
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseMediaStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val store = ExerciseMediaStore(context)

    @Test
    fun importCopiesSupportedMediaIntoPrivateStorage() {
        val imported = store.import(Uri.parse("content://com.keepfit.core.media.test/demo.gif"))
        val destination = File(context.filesDir, imported.relativePath)

        assertEquals("GIF", imported.mediaType)
        assertEquals("image/gif", imported.mimeType)
        assertEquals(6, imported.sizeBytes)
        assertTrue(destination.exists())
        assertEquals("GIF89a", destination.readText())

        destination.delete()
    }

    @Test(expected = IllegalArgumentException::class)
    fun importRejectsUnsupportedMedia() {
        store.import(Uri.parse("content://com.keepfit.core.media.test/demo.txt"))
    }

    @Test
    fun missingRestoredMediaResolvesAsUnavailableWithoutDeletingMetadata() {
        assertNull(store.resolve("media/exercises/missing.gif"))
        assertNull(TransformationPhotoStore(context).resolve("media/transformation/cycle/missing.jpg"))
        assertNull(TransformationPhotoStore(context).resolve("../outside.jpg"))
    }

    @Test
    fun transformationImportOrientsBoundsAndRemovesSourceMetadata() {
        val imported = TransformationPhotoStore(context).import(
            cycleId = "privacy-cycle",
            uri = Uri.parse("content://com.keepfit.core.media.test/photo.jpg"),
        )
        val destination = File(context.filesDir, imported.relativePath)
        val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        BitmapFactory.decodeFile(destination.absolutePath, bounds)
        val exif = ExifInterface(destination.absolutePath)

        assertTrue(bounds.outWidth <= 2048)
        assertTrue(bounds.outHeight <= 2048)
        assertTrue(bounds.outHeight > bounds.outWidth)
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_MODEL))
        assertTrue(
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) in
                setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL),
        )

        destination.delete()
    }
}
