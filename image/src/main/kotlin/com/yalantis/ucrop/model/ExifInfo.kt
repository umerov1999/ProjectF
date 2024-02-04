package com.yalantis.ucrop.model

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
class ExifInfo(var exifOrientation: Int, var exifDegrees: Int, var exifTranslation: Int) {

    override fun equals(other: Any?): Boolean {
        return if (other !is ExifInfo || exifOrientation != other.exifOrientation || exifDegrees != other.exifDegrees) false else exifTranslation == other.exifTranslation
    }

    override fun hashCode(): Int {
        var result = exifOrientation
        result = 31 * result + exifDegrees
        result = 31 * result + exifTranslation
        return result
    }
}
