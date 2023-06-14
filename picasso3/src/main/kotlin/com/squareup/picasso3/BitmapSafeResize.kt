package com.squareup.picasso3

import android.graphics.Bitmap
import android.graphics.Canvas

object BitmapSafeResize {
    private var maxResolution = -1
    private var hardwareRendering = 0

    fun isHardwareRendering(): Int {
        return hardwareRendering
    }

    fun setHardwareRendering(state: Int) {
        hardwareRendering = state
    }

    fun setMaxResolution(maxResolution: Int) {
        if (maxResolution > 100) {
            BitmapSafeResize.maxResolution = maxResolution
        } else {
            BitmapSafeResize.maxResolution = -1
        }
    }

    fun isOverflowCanvas(resolution: Int): Boolean {
        val canvas = Canvas()
        val maxCanvasSize = canvas.maximumBitmapWidth.coerceAtMost(canvas.maximumBitmapHeight)
        return if (maxCanvasSize > 0) {
            maxCanvasSize < resolution
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

    interface ResizeBitmap {
        fun doResize(resizedWidth: Int, resizedHeight: Int)
    }

    fun checkSizeOfBitmapP(sourceWidth: Int, sourceHeight: Int, callback: ResizeBitmap) {
        if (maxResolution < 0 || sourceWidth <= 0 || sourceHeight <= 0 || sourceWidth <= maxResolution && sourceHeight <= maxResolution) {
            return
        }
        var mWidth = sourceWidth
        var mHeight = sourceHeight
        val mCo = mHeight.coerceAtMost(mWidth).toFloat() / mHeight.coerceAtLeast(mWidth)
        if (mWidth > mHeight) {
            mWidth = maxResolution
            mHeight = (maxResolution * mCo).toInt()
        } else {
            mHeight = maxResolution
            mWidth = (maxResolution * mCo).toInt()
        }
        if (mWidth <= 0 || mHeight <= 0) {
            return
        }
        callback.doResize(mWidth, mHeight)
    }
}