package dev.ragnarok.filegallery.util

import android.graphics.Bitmap
import android.graphics.Canvas

object CoverSafeResize {
    private var maxResolution = -1

    fun getMaxResolution(): Int {
        return maxResolution
    }

    fun setMaxResolution(maxResolution: Int) {
        if (maxResolution > 100) {
            CoverSafeResize.maxResolution = maxResolution
        } else {
            CoverSafeResize.maxResolution = -1
        }
    }

    fun isOverflowCanvas(Resolution: Int): Boolean {
        val canvas = Canvas()
        val maxCanvasSize = canvas.maximumBitmapWidth.coerceAtMost(canvas.maximumBitmapHeight)
        return if (maxCanvasSize > 0) {
            maxCanvasSize < Resolution
        } else false
    }

    fun checkBitmap(bitmap: Bitmap): Bitmap {
        if (maxResolution < 0 || bitmap.width <= 0 || bitmap.height <= 0 || bitmap.width <= maxResolution && bitmap.height <= maxResolution) {
            return bitmap
        }
        var mWidth = bitmap.width
        var mHeight = bitmap.height
        val mCo = mHeight.coerceAtMost(mWidth).toFloat() / mHeight.coerceAtLeast(mWidth)
        if (mWidth > mHeight) {
            mWidth = maxResolution
            mHeight = (maxResolution * mCo).toInt()
        } else {
            mHeight = maxResolution
            mWidth = (maxResolution * mCo).toInt()
        }
        if (mWidth <= 0 || mHeight <= 0) {
            return bitmap
        }
        val tmp = Bitmap.createScaledBitmap(bitmap, mWidth, mHeight, true)
        bitmap.recycle()
        return tmp
    }
}