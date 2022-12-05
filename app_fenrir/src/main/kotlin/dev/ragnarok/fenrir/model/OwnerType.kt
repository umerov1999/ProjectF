package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(OwnerType.USER, OwnerType.COMMUNITY)
@Retention(AnnotationRetention.SOURCE)
annotation class OwnerType {
    companion object {
        const val USER = 1
        const val COMMUNITY = 2
    }
}