package dev.ragnarok.fenrir.module.animation

import androidx.annotation.IntDef

@IntDef(LoadedFrom.NET, LoadedFrom.NO, LoadedFrom.FILE, LoadedFrom.RES)
@Retention(AnnotationRetention.SOURCE)
annotation class LoadedFrom {
    companion object {
        const val NET = -1
        const val NO = 0
        const val FILE = 1
        const val RES = 3
    }
}
