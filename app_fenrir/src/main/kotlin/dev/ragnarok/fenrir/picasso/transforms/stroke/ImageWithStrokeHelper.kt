package dev.ragnarok.fenrir.picasso.transforms.stroke

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.ColorInt

object ImageWithStrokeHelper {
    fun getRoundedBitmap(
        @ColorInt strokeFirst: Int,
        workBitmap: Bitmap?
    ): Bitmap? {
        workBitmap ?: return null
        val bitmapWidth = workBitmap.width
        val bitmapHeight = workBitmap.height
        val isHardware =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && workBitmap.config == Bitmap.Config.HARDWARE

        var output: Bitmap? = null
        val canvas: Canvas
        var obj: Picture? = null
        if (isHardware) {
            obj = Picture()
            canvas = obj.beginRecording(bitmapWidth, bitmapHeight)
        } else {
            output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, workBitmap.config)
            canvas = Canvas(output)
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(workBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawOval(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), paint)

        paint.style = Paint.Style.STROKE
        val pth = (bitmapWidth + bitmapHeight).toFloat() / 2
        var rdd = 0.066f * pth
        paint.strokeWidth = rdd
        paint.shader = null
        paint.color = Color.TRANSPARENT
        paint.alpha = 0
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawOval(
            rdd / 2,
            rdd / 2,
            (bitmapWidth - rdd / 2),
            (bitmapHeight - rdd / 2),
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
            (bitmapWidth - rdd / 2),
            (bitmapHeight - rdd / 2),
            paint
        )
        workBitmap.recycle()
        if (isHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            obj?.endRecording()
            output =
                obj?.let { Bitmap.createBitmap(it, it.width, it.height, Bitmap.Config.HARDWARE) }
        }
        return output
    }

    fun getEllipseBitmap(
        @ColorInt strokeFirst: Int,
        workBitmap: Bitmap?,
        angle: Float
    ): Bitmap? {
        workBitmap ?: return null
        val bitmapWidth = workBitmap.width
        val bitmapHeight = workBitmap.height
        val isHardware =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && workBitmap.config == Bitmap.Config.HARDWARE

        var output: Bitmap? = null
        val canvas: Canvas
        var obj: Picture? = null
        if (isHardware) {
            obj = Picture()
            canvas = obj.beginRecording(bitmapWidth, bitmapHeight)
        } else {
            output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, workBitmap.config)
            canvas = Canvas(output)
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(workBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val pth = (bitmapWidth + bitmapHeight).toFloat() / 2
        canvas.drawRoundRect(
            0f,
            0f,
            bitmapWidth.toFloat(),
            bitmapHeight.toFloat(),
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
            (bitmapWidth - rdd / 2),
            (bitmapHeight - rdd / 2),
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
            (bitmapWidth - rdd / 2),
            (bitmapHeight - rdd / 2),
            pth * angle,
            pth * angle,
            paint
        )
        workBitmap.recycle()
        if (isHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            obj?.endRecording()
            output =
                obj?.let { Bitmap.createBitmap(it, it.width, it.height, Bitmap.Config.HARDWARE) }
        }
        return output
    }
}
