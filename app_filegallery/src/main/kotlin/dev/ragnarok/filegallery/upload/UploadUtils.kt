package dev.ragnarok.filegallery.upload

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import dev.ragnarok.filegallery.getString
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.roundToInt

object UploadUtils {

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

    fun createIntents(
        destination: UploadDestination, file: String?, size: Int,
        autoCommit: Boolean
    ): List<UploadIntent> {
        val intents: MutableList<UploadIntent> = ArrayList()
        intents.add(
            UploadIntent(destination)
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