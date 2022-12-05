package dev.ragnarok.filegallery.upload

import androidx.annotation.IntDef

@IntDef(
    Method.REMOTE_PLAY_AUDIO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class Method {
    companion object {
        const val REMOTE_PLAY_AUDIO = 1
    }
}