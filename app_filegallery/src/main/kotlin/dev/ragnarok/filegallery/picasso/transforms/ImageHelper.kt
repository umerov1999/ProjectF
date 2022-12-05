package dev.ragnarok.filegallery.picasso.transforms

import android.graphics.*
import android.os.Build

object ImageHelper {
    fun getRoundedBitmap(workBitmap: Bitmap?): Bitmap? {
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
        if (bitmap != output) {
            bitmap.recycle()
        }
        return output
    }

    fun getEllipseBitmap(workBitmap: Bitmap?, angle: Float): Bitmap? {
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
        if (bitmap != output) {
            bitmap.recycle()
        }
        return output
    }
}