package me.minetsh.imaging.core.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by felix on 2017/12/26 下午3:07.
 */
class IMGFileDecoder(private val mContext: Context, uri: Uri) : IMGDecoder(
    uri
) {
    override fun decode(options: BitmapFactory.Options?): Bitmap? {
        val path = uri.path
        if (path.isNullOrEmpty()) {
            return null
        }
        try {
            val originalStream: InputStream?
            val filef = File(path)
            originalStream = if (filef.isFile) {
                FileInputStream(filef)
            } else {
                mContext.contentResolver.openInputStream(uri)
            }
            return BitmapFactory.decodeStream(originalStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
