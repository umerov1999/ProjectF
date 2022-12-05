package dev.ragnarok.fenrir.model

import androidx.annotation.StringDef

@StringDef(FavePageType.USER, FavePageType.COMMUNITY)
@Retention(AnnotationRetention.SOURCE)
annotation class FavePageType {
    companion object {
        const val USER = "user"
        const val COMMUNITY = "group"
    }
}