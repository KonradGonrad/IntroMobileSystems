package com.example.myapplication.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Saves photos from the camera or gallery inside the app's private storage.
 * Photos are made smaller first (max 1280 px) so big camera files
 * don't slow down the feed (risk from M1).
 */
class PhotoStorage(private val context: Context) {

    /** Temporary file the camera app writes the picture into. */
    fun createCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Returns the path of the saved JPEG, or null if the picture can't be read. */
    suspend fun savePhoto(source: Uri): String? = withContext(Dispatchers.IO) {
        try {
        val resolver = context.contentResolver

        // 1. read only the size of the picture
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        // 2. decode a smaller version of it
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoUtils.calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_SIZE)
        }
        var bitmap = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        // 3. scale it down to max 1280 px on the longer side
        val (newWidth, newHeight) = PhotoUtils.scaledSize(bitmap.width, bitmap.height, MAX_SIZE)
        if (newWidth != bitmap.width) {
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // 4. camera photos can be saved sideways, the EXIF tag says how to rotate them
        val orientation = resolver.openInputStream(source)?.use {
            try { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: IOException) { ExifInterface.ORIENTATION_NORMAL }
        } ?: ExifInterface.ORIENTATION_NORMAL
        val (rotation, mirrored) = PhotoUtils.orientationTransform(orientation)
        if (rotation != 0 || mirrored) {
            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                if(mirrored) postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        // 5. save as JPEG in the app folder
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        file.absolutePath
        } catch (_: IOException) { null }
        catch (_: SecurityException) { null }
        catch (_: IllegalArgumentException) { null }
    }

    private companion object {
        const val MAX_SIZE = 1280
    }
}
