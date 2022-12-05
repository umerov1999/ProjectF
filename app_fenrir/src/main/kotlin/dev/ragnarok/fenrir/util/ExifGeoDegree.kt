package dev.ragnarok.fenrir.util

import androidx.exifinterface.media.ExifInterface

class ExifGeoDegree(exif: ExifInterface) {
    var isValid = false
    var latitude: Double? = null
    var longitude: Double? = null
    private fun convertToDegree(stringDMS: String): Double {
        val DMS = stringDMS.split(Regex(","), 3).toTypedArray()
        val stringD = DMS[0].split(Regex("/"), 2).toTypedArray()
        val D0 = stringD[0].toDouble()
        val D1 = stringD[1].toDouble()
        val FloatD = D0 / D1
        val stringM = DMS[1].split(Regex("/"), 2).toTypedArray()
        val M0 = stringM[0].toDouble()
        val M1 = stringM[1].toDouble()
        val FloatM = M0 / M1
        val stringS = DMS[2].split(Regex("/"), 2).toTypedArray()
        val S0 = stringS[0].toDouble()
        val S1 = stringS[1].toDouble()
        val FloatS = S0 / S1
        return FloatD + FloatM / 60 + FloatS / 3600
    }

    override fun toString(): String {
        return latitude.toString() + ", " + longitude
    }

    init {
        val attrLATITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val attrLATITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val attrLONGITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val attrLONGITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
        if (attrLATITUDE != null
            && attrLATITUDE_REF != null
            && attrLONGITUDE != null
            && attrLONGITUDE_REF != null
        ) {
            isValid = true
            latitude = if (attrLATITUDE_REF == "N") {
                convertToDegree(attrLATITUDE)
            } else {
                0 - convertToDegree(attrLATITUDE)
            }
            longitude = if (attrLONGITUDE_REF == "E") {
                convertToDegree(attrLONGITUDE)
            } else {
                0 - convertToDegree(attrLONGITUDE)
            }
        }
    }
}