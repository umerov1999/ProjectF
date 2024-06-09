package com.yalantis.ucrop.callback

import android.graphics.Bitmap
import com.yalantis.ucrop.model.ExifInfo

interface BitmapLoadCallback {
    fun onBitmapLoaded(
        bitmap: Bitmap,
        exifInfo: ExifInfo,
        imageInputPath: String?,
        imageOutputPath: String?
    )

    fun onFailure(bitmapWorkerException: Throwable)
}