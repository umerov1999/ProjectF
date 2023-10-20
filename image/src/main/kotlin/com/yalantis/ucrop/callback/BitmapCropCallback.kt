package com.yalantis.ucrop.callback

import android.net.Uri

interface BitmapCropCallback {
    fun onBitmapCropped(
        resultUri: Uri,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int
    )

    fun onCropFailure(t: Throwable)
}