package dev.ragnarok.fenrir.crypt

import androidx.annotation.IntDef

@IntDef(KeyLocationPolicy.PERSIST, KeyLocationPolicy.RAM)
@Retention(AnnotationRetention.SOURCE)
annotation class KeyLocationPolicy {
    companion object {
        const val PERSIST = 1
        const val RAM = 2
    }
}