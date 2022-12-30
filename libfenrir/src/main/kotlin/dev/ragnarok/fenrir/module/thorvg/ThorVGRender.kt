package dev.ragnarok.fenrir.module.thorvg

import android.graphics.Bitmap
import androidx.annotation.RawRes
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.FenrirNative
import java.io.InputStream

object ThorVGRender {
    private external fun registerColorsNative(name: String, value: Int)
    private external fun createBitmapNative(res: Long, bitmap: Bitmap?, w: Int, h: Int)

    fun registerColors(colors: Map<String, Int>) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        for ((key, value) in colors) {
            registerColorsNative(key, value)
        }
    }

    fun createBitmap(res: BufferWriteNative, w: Int, h: Int): Bitmap? {
        if (!FenrirNative.isNativeLoaded) {
            return null
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        createBitmapNative(res.pointer, bitmap, w, h)
        return bitmap
    }

    fun createBitmap(@RawRes res: Int, w: Int, h: Int): Bitmap? {
        if (!FenrirNative.isNativeLoaded) {
            return null
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        createBitmapNative(readRes(res)?.pointer ?: return null, bitmap, w, h)
        return bitmap
    }

    private fun readRes(@RawRes rawRes: Int): BufferWriteNative? {
        var inputStream: InputStream? = null
        return try {
            inputStream = FenrirNative.appContext.resources.openRawResource(rawRes)
            val res = BufferWriteNative.fromStreamEndlessNull(inputStream)
            if (res.bufferSize() <= 0) {
                inputStream.close()
                return null
            }
            res
        } catch (e: Throwable) {
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (ignore: Throwable) {
            }
        }
    }
}
