package dev.ragnarok.fenrir.picasso.transforms

import android.graphics.Bitmap
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation

class CropTransformation(val x: Int, val y: Int, val w: Int, val h: Int) : Transformation {
    override fun key(): String {
        return "$TAG($x, $y, $w, $h)"
    }

    private fun crop(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val st = Bitmap.createBitmap(bitmap, x, y, w, h)
            bitmap.recycle()
            return st
        }
        return null
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else crop(source)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            crop(source.bitmap)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = CropTransformation::class.java.simpleName
    }
}
