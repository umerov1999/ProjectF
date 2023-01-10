package dev.ragnarok.fenrir.picasso.transforms

import android.graphics.*
import android.os.Build
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation

class MonochromeTransformation : Transformation {
    override fun key(): String {
        return "$TAG()"
    }

    fun transform(source: Bitmap): Bitmap? {
        val bitmapWidth = source.width
        val bitmapHeight = source.height
        val isHardware =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && source.config == Bitmap.Config.HARDWARE

        var output: Bitmap? = null
        val canvas: Canvas
        var obj: Picture? = null
        if (isHardware) {
            obj = Picture()
            canvas = obj.beginRecording(bitmapWidth, bitmapHeight)
        } else {
            output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, source.config)
            canvas = Canvas(output)
        }
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val colorFilter = ColorMatrixColorFilter(matrix)
        paint.colorFilter = colorFilter
        paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        canvas.drawRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), paint)
        source.recycle()
        if (isHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            obj?.endRecording()
            output =
                obj?.let { Bitmap.createBitmap(it, it.width, it.height, Bitmap.Config.HARDWARE) }
        }
        return output
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
