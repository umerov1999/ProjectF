package com.yalantis.ucrop.callback

/**
 * Interface for crop bound change notifying.
 */
fun interface CropBoundsChangeListener {
    fun onCropAspectRatioChanged(cropRatio: Float)
}