package dev.ragnarok.fenrir.module.camerax

import android.view.Surface

object SurfaceUtilNative {
    fun getSurfaceInfo(surface: Surface?): IntArray {
        return nativeGetSurfaceInfo(surface)
    }

    private external fun nativeGetSurfaceInfo(surface: Surface?): IntArray
}