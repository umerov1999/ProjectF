package dev.ragnarok.filegallery.picasso.transforms

import android.graphics.*
import android.os.Build
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation

class MonochromeTransformation : Transformation {
    override fun key(): String {
        return "$TAG()"
    }

    fun transform(source: Bitmap): Bitmap? {
        var bitmap = source
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.config == Bitmap.Config.HARDWARE) {
            val tmpBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            bitmap = tmpBitmap
            if (bitmap == null) {
                return null
            }
        }
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)

        val bitmapCopy = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmapCopy)
        val paint = Paint()
        val colorFilter = ColorMatrixColorFilter(matrix)
        paint.colorFilter = colorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        return bitmapCopy
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else transform(source)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            transform(source.bitmap)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = MonochromeTransformation::class.java.simpleName
    }
}
