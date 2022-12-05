package dev.ragnarok.fenrir.util

import android.database.Cursor
import android.graphics.Bitmap
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object IOUtils {

    fun recycleBitmapQuietly(bitmap: Bitmap?) {
        bitmap?.recycle()
    }


    fun closeStreamQuietly(streamToClose: InputStream?) {
        if (streamToClose == null) return
        try {
            streamToClose.close()
        } catch (ignored: IOException) {
        }
    }


    fun closeStreamQuietly(streamToClose: OutputStream?) {
        if (streamToClose == null) return
        try {
            streamToClose.close()
        } catch (ignored: IOException) {
        }
    }


    fun closeCursorQuietly(cursor: Cursor?) {
        if (cursor == null) return
        try {
            cursor.close()
        } catch (ignored: Exception) {
        }
    }
}