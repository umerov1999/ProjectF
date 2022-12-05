package dev.ragnarok.fenrir.module.parcel

import androidx.annotation.IntDef

@IntDef(ParcelFlags.NULL_LIST, ParcelFlags.EMPTY_LIST, ParcelFlags.MUTABLE_LIST)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ParcelFlags {
    companion object {
        const val NULL_LIST = 0
        const val EMPTY_LIST = 1
        const val MUTABLE_LIST = 2
    }
}