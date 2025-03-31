package dev.ragnarok.fenrir.module.animation

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import dev.ragnarok.fenrir.module.FenrirNative
import java.io.File

object AnimatedFileFrame {
    private external fun createDecoder(src: String, params: IntArray): Long
    private external fun destroyDecoder(ptr: Long)
    private external fun getFrameAtTime(
        ptr: Long,
        ms: Long,
        bitmap: Bitmap?,
        data: IntArray,
        stride: Int
    ): Int

    fun getDurationMs(file: File): Long {
        if (!FenrirNative.isNativeLoaded) {
            return -1
        }
        val metaData = IntArray(6)
        val nPtr = createDecoder(file.absolutePath, metaData)
        if (nPtr != 0L && (metaData[0] > 3840 || metaData[1] > 3840)) {
            destroyDecoder(nPtr)
            return -1
        }
        if (nPtr == 0L) {
            return -1
        }
        val ret = metaData[4].toLong()
        destroyDecoder(nPtr)
        return ret
    }

    fun getThumbnail(file: File, positionMs: (duration: Long) -> Long): Bitmap? {
        if (!FenrirNative.isNativeLoaded) {
            return null
        }
        val metaData = IntArray(6)
        val nPtr = createDecoder(file.absolutePath, metaData)
        if (nPtr != 0L && (metaData[0] > 3840 || metaData[1] > 3840)) {
            destroyDecoder(nPtr)
            return null
        }
        if (nPtr == 0L) {
            return null
        }
        var frameAt = positionMs.invoke(metaData[4].toLong())
        if (frameAt < 0 || frameAt >= metaData[4].toLong()) {
            frameAt = (metaData[4] / 2).toLong()
        }
        var ret = createBitmap(metaData[0], metaData[1], Bitmap.Config.ARGB_8888)
        if (getFrameAtTime(
                nPtr,
                frameAt,
                ret,
                metaData,
                ret.rowBytes
            ) == 0
        ) {
            ret.recycle()
            destroyDecoder(nPtr)
            return null
        }
        destroyDecoder(nPtr)
        when {
            metaData[2] == 90 -> {
                val matrix = Matrix().apply { postRotate(90f) }
                val ret2 = Bitmap.createBitmap(ret, 0, 0, ret.width, ret.height, matrix, true)
                ret.recycle()
                ret = ret2
            }

            metaData[2] == 180 -> {
                val matrix = Matrix().apply { postRotate(180f) }
                val ret2 = Bitmap.createBitmap(ret, 0, 0, ret.width, ret.height, matrix, true)
                ret.recycle()
                ret = ret2
            }

            metaData[2] == 270 -> {
                val matrix = Matrix().apply { postRotate(270f) }
                val ret2 = Bitmap.createBitmap(ret, 0, 0, ret.width, ret.height, matrix, true)
                ret.recycle()
                ret = ret2
            }
        }
        return ret
    }

    fun getThumbnail(file: File, positionMs: Long = -1): Bitmap? {
        return getThumbnail(file) {
            positionMs
        }
    }
}
