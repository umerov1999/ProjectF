package dev.ragnarok.fenrir.picasso.transforms

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.PorterDuff
import android.graphics.Shader
import android.os.Build

object ImageHelper {
    fun getRoundedBitmap(workBitmap: Bitmap?): Bitmap? {
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
        workBitmap.recycle()
        if (isHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            obj?.endRecording()
            output =
                obj?.let { Bitmap.createBitmap(it, it.width, it.height, Bitmap.Config.HARDWARE) }
        }
        return output
    }

    fun getEllipseBitmap(workBitmap: Bitmap?, angle: Float): Bitmap? {
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
        workBitmap.recycle()
        if (isHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            obj?.endRecording()
            output =
                obj?.let { Bitmap.createBitmap(it, it.width, it.height, Bitmap.Config.HARDWARE) }
        }
        return output
    }
}
