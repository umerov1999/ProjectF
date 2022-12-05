package dev.ragnarok.fenrir.util

import androidx.annotation.IntDef

@IntDef(Side.DISABLED, Side.NO_LOADING, Side.LOADING)
@Retention(AnnotationRetention.SOURCE)
annotation class Side {
    companion object {
        const val DISABLED = 1
        const val NO_LOADING = 2
        const val LOADING = 3
    }
}