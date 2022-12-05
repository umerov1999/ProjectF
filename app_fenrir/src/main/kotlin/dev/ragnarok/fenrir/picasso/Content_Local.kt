package dev.ragnarok.fenrir.picasso

import androidx.annotation.IntDef

@IntDef(Content_Local.PHOTO, Content_Local.VIDEO, Content_Local.AUDIO)
@Retention(AnnotationRetention.SOURCE)
annotation class Content_Local {
    companion object {
        const val PHOTO = 1
        const val VIDEO = 2
        const val AUDIO = 3
    }
}