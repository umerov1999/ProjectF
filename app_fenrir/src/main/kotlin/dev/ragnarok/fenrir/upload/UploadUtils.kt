package dev.ragnarok.fenrir.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.IOUtils.closeStreamQuietly
import dev.ragnarok.fenrir.util.IOUtils.recycleBitmapQuietly
import java.io.*
import kotlin.math.roundToInt

object UploadUtils {
    fun copyExif(
        originalExif: ExifInterface,
        width: Int,
        height: Int,
        imageOutputPath: File
    ) {
        val attributes = arrayOf(
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_WHITE_BALANCE
        )
        try {
            val newExif = ExifInterface(imageOutputPath)
            var value: String?
            for (attribute in attributes) {
                value = originalExif.getAttribute(attribute)
                if (value.nonNullNoEmpty()) {
                    newExif.setAttribute(attribute, value)
                }
            }
            newExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            newExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, "0")
            newExif.saveAttributes()
        } catch (e: IOException) {
            Log.d("Exif upload resize", e.message ?: return)
        }
    }

    fun transformBitmap(pBitmap: Bitmap, transformMatrix: Matrix): Bitmap {
        var bitmap = pBitmap
        try {
            val converted = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                transformMatrix,
                true
            )
            if (!bitmap.sameAs(converted)) {
                bitmap = converted
            }
        } catch (error: OutOfMemoryError) {
            Log.e("Exif upload resize", "transformBitmap: ", error)
        }
        return bitmap
    }

    @Throws(IOException::class)
    fun createStream(context: Context, uri: Uri?): InputStream? {
        val originalStream: InputStream?
        val filef = File((uri ?: return null).path ?: return null)
        originalStream = if (filef.isFile) {
            FileInputStream(filef)
        } else {
            context.contentResolver.openInputStream(uri)
        }
        return originalStream
    }

    @Throws(IOException::class)
    fun openStream(context: Context, uri: Uri?, size: Int): InputStream? {
        val originalStream: InputStream?
        val filef = File((uri ?: return null).path ?: return null)
        originalStream = if (filef.isFile) {
            FileInputStream(filef)
        } else {
            context.contentResolver.openInputStream(uri)
        }
        if (size == Upload.IMAGE_SIZE_FULL || size == Upload.IMAGE_SIZE_CROPPING) {
            return originalStream
        }
        var bitmap = BitmapFactory.decodeStream(originalStream)
        var originalExif: ExifInterface? = null
        val matrix = Matrix()
        var bApply = false
        try {
            originalExif = ExifInterface(createStream(context, uri) ?: return null)
            if (originalExif.rotationDegrees != 0) {
                matrix.preRotate(originalExif.rotationDegrees.toFloat())
                bApply = true
            }
        } catch (ignored: Exception) {
        }
        if (bApply) {
            bitmap = transformBitmap(bitmap, matrix)
        }
        val tempFile = File(context.externalCacheDir.toString() + File.separator + "scale.jpg")
        var target: Bitmap? = null
        return try {
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    throw IOException("Unable to delete old image file")
                }
            }
            if (!tempFile.createNewFile()) {
                throw IOException("Unable to create new file")
            }
            val ostream = FileOutputStream(tempFile)
            target = scaleDown(bitmap, size.toFloat(), true)
            target.compress(Bitmap.CompressFormat.JPEG, 100, ostream)
            ostream.flush()
            ostream.close()
            if (originalExif != null) {
                copyExif(originalExif, bitmap.width, bitmap.height, tempFile)
            }
            FileInputStream(tempFile)
        } finally {
            recycleBitmapQuietly(bitmap)
            recycleBitmapQuietly(target)
            closeStreamQuietly(originalStream)
        }
    }


    fun createIntents(
        accountId: Int, destination: UploadDestination, photos: List<LocalPhoto>, size: Int,
        autoCommit: Boolean
    ): List<UploadIntent> {
        val intents: MutableList<UploadIntent> = ArrayList(photos.size)
        for (photo in photos) {
            intents.add(
                UploadIntent(accountId, destination)
                    .setSize(size)
                    .setAutoCommit(autoCommit)
                    .setFileId(photo.getImageId())
                    .setFileUri(photo.getFullImageUri())
            )
        }
        return intents
    }

    fun createVideoIntents(
        accountId: Int, destination: UploadDestination, path: String?,
        autoCommit: Boolean
    ): List<UploadIntent> {
        val intent = UploadIntent(accountId, destination).setAutoCommit(autoCommit).setFileUri(
            Uri.parse(path)
        )
        return listOf(intent)
    }

    fun createIntents(
        accountId: Int, destination: UploadDestination, file: String?, size: Int,
        autoCommit: Boolean
    ): List<UploadIntent> {
        val intents: MutableList<UploadIntent> = ArrayList()
        intents.add(
            UploadIntent(accountId, destination)
                .setSize(size)
                .setAutoCommit(autoCommit)
                .setFileUri(Uri.parse(file))
        )
        return intents
    }

    fun findFileName(context: Context, uri: Uri?): String? {
        uri ?: return null
        var fileName = uri.lastPathSegment
        try {
            val scheme = uri.scheme
            if (scheme == "file") {
                fileName = uri.lastPathSegment
            } else if (scheme == "content") {
                val proj = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                val cursor = context.contentResolver.query(uri, proj, null, null, null)
                if (cursor != null && cursor.count != 0) {
                    val columnIndex =
                        MediaStore.MediaColumns.DISPLAY_NAME
                    cursor.moveToFirst()
                    fileName = cursor.getString(columnIndex)
                }
                cursor?.close()
            }
        } catch (ignored: Exception) {
        }
        return fileName
    }

    fun scaleDown(realImage: Bitmap, maxImageSize: Float, filter: Boolean): Bitmap {
        if (realImage.height < maxImageSize && realImage.width < maxImageSize) {
            return realImage
        }
        val ratio = (maxImageSize / realImage.width).coerceAtMost(maxImageSize / realImage.height)
        val width = (ratio * realImage.width).roundToInt()
        val height = (ratio * realImage.height).roundToInt()
        return Bitmap.createScaledBitmap(realImage, width, height, filter)
    }
}