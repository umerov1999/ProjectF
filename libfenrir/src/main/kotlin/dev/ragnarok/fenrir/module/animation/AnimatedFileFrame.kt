package dev.ragnarok.fenrir.module.animation

import android.graphics.Bitmap
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

    fun getThumbnail(file: File): Bitmap? {
        if (!FenrirNative.isNativeLoaded) {
            return null
        }
        val metaData = IntArray(5)
        val nPtr = createDecoder(file.absolutePath, metaData)
        if (nPtr != 0L && (metaData[0] > 3840 || metaData[1] > 3840)) {
            destroyDecoder(nPtr)
            return null
        }
        if (nPtr == 0L) {
            return null
        }
        val ret = Bitmap.createBitmap(metaData[0], metaData[1], Bitmap.Config.ARGB_8888)
        if (getFrameAtTime(
                nPtr,
                (metaData[4] / 2).toLong(),
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
        return ret
    }
}