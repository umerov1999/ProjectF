package me.minetsh.imaging.core.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.IOException

/**
 * Created by felix on 2017/12/26 下午2:57.
 */
class IMGAssetFileDecoder(private val mContext: Context, uri: Uri) : IMGDecoder(uri) {
    override fun decode(options: BitmapFactory.Options?): Bitmap? {
        var path = uri.path
        if (path.isNullOrEmpty()) {
            return null
        }
        path = path.substring(1)
        try {
            val iStream = mContext.assets.open(path)
            return BitmapFactory.decodeStream(iStream, null, options)
        } catch (_: IOException) {
        }
        return null
    }
}
