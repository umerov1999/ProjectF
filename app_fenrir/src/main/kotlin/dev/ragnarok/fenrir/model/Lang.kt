package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(Lang.DEFAULT, Lang.ENGLISH, Lang.RUSSIA)
@Retention(AnnotationRetention.SOURCE)
annotation class Lang {
    companion object {
        const val DEFAULT = 0
        const val ENGLISH = 1
        const val RUSSIA = 2
    }
}