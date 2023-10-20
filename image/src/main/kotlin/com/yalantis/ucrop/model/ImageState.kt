package com.yalantis.ucrop.model

import android.graphics.RectF

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
class ImageState(
    val cropRect: RectF,
    val currentImageRect: RectF,
    val currentScale: Float,
    val currentAngle: Float
)
