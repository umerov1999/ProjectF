package com.yalantis.ucrop.model

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
class ExifInfo(var exifOrientation: Int, var exifDegrees: Int, var exifTranslation: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val exifInfo = other as ExifInfo
        if (exifOrientation != exifInfo.exifOrientation) return false
        return if (exifDegrees != exifInfo.exifDegrees) false else exifTranslation == exifInfo.exifTranslation
    }

    override fun hashCode(): Int {
        var result = exifOrientation
        result = 31 * result + exifDegrees
        result = 31 * result + exifTranslation
        return result
    }
}
