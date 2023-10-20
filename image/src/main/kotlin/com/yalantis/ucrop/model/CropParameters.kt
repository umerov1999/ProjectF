package com.yalantis.ucrop.model

import android.graphics.Bitmap.CompressFormat

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
class CropParameters(
    val maxResultImageSizeX: Int, val maxResultImageSizeY: Int,
    val compressFormat: CompressFormat, val compressQuality: Int,
    val imageInputPath: String?, val imageOutputPath: String?
)