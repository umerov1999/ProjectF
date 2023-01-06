package dev.ragnarok.fenrir.picasso.transforms.stroke

import android.graphics.*
import android.os.Build
import androidx.annotation.ColorInt

object ImageWithStrokeHelper {
    fun getRoundedBitmap(
        @ColorInt strokeFirst: Int,
        workBitmap: Bitmap?
    ): Bitmap? {
        workBitmap ?: return null
        var bitmap = workBitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.config == Bitmap.Config.HARDWARE) {
            val tmpBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            bitmap = tmpBitmap
            if (bitmap == null) {
                return null
            }
        }
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawOval(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)

        paint.style = Paint.Style.STROKE
        val pth = (bitmap.width + bitmap.height).toFloat() / 2
        var rdd = 0.066f * pth
        paint.strokeWidth = rdd
        paint.shader = null
        paint.color = Color.TRANSPARENT
        paint.alpha = 0
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawOval(
            rdd / 2,
            rdd / 2,
            (bitmap.width - rdd / 2),
            (bitmap.height - rdd / 2),
            paint
        )

        rdd = 0.040f * pth
        paint.strokeWidth = rdd
        paint.color = strokeFirst
        paint.alpha = 255
        paint.xfermode = null
        canvas.drawOval(
            rdd / 2,
            rdd / 2,
            (bitmap.width - rdd / 2),
            (bitmap.height - rdd / 2),
            paint
        )

        if (bitmap != output) {
            bitmap.recycle()
        }
        return output
    }

    fun getEllipseBitmap(
        @ColorInt strokeFirst: Int,
        workBitmap: Bitmap?,
        angle: Float
    ): Bitmap? {
        workBitmap ?: return null
        var bitmap = workBitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.config == Bitmap.Config.HARDWARE) {
            val tmpBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            bitmap = tmpBitmap
            if (bitmap == null) {
                return null
            }
        }
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val pth = (bitmap.width + bitmap.height).toFloat() / 2
        canvas.drawRoundRect(
            0f,
            0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            pth * angle,
            pth * angle,
            paint
        )
        paint.style = Paint.Style.STROKE
        var rdd = 0.066f * pth
        paint.strokeWidth = rdd
        paint.shader = null
        paint.color = Color.TRANSPARENT
        paint.alpha = 0
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRoundRect(
            rdd / 2,
            rdd / 2,
            (bitmap.width - rdd / 2),
            (bitmap.height - rdd / 2),
            pth * angle,
            pth * angle,
            paint
        )

        rdd = 0.040f * pth
        paint.strokeWidth = rdd
        paint.color = strokeFirst
        paint.alpha = 255
        paint.xfermode = null
        canvas.drawRoundRect(
            rdd / 2,
            rdd / 2,
            (bitmap.width - rdd / 2),
            (bitmap.height - rdd / 2),
            pth * angle,
            pth * angle,
            paint
        )

        if (bitmap != output) {
            bitmap.recycle()
        }
        return output
    }
}
