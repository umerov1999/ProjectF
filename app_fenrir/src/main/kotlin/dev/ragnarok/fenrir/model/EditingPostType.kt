package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(EditingPostType.DRAFT, EditingPostType.TEMP)
@Retention(AnnotationRetention.SOURCE)
annotation class EditingPostType {
    companion object {
        const val DRAFT = 2
        const val TEMP = 3
    }
}