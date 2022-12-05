package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(Sex.MAN, Sex.WOMAN, Sex.UNKNOWN)
@Retention(AnnotationRetention.SOURCE)
annotation class Sex {
    companion object {
        const val MAN = 2
        const val WOMAN = 1
        const val UNKNOWN = 0
    }
}