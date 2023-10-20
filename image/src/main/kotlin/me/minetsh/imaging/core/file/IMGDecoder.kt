package me.minetsh.imaging.core.file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Created by felix on 2017/12/26 下午2:54.
 */
abstract class IMGDecoder(var uri: Uri) {

    fun decode(): Bitmap? {
        return decode(null)
    }

    abstract fun decode(options: BitmapFactory.Options?): Bitmap?
}
