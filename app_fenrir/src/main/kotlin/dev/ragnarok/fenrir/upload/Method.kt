package dev.ragnarok.fenrir.upload

import androidx.annotation.IntDef

@IntDef(
    Method.PHOTO_TO_ALBUM,
    Method.TO_WALL,
    Method.TO_COMMENT,
    Method.PHOTO_TO_PROFILE,
    Method.PHOTO_TO_CHAT,
    Method.TO_MESSAGE,
    Method.AUDIO,
    Method.VIDEO,
    Method.DOCUMENT,
    Method.STORY,
    Method.REMOTE_PLAY_AUDIO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class Method {
    companion object {
        const val PHOTO_TO_ALBUM = 1
        const val TO_WALL = 2
        const val TO_COMMENT = 3
        const val PHOTO_TO_PROFILE = 4
        const val PHOTO_TO_CHAT = 5
        const val TO_MESSAGE = 6
        const val AUDIO = 7
        const val VIDEO = 8
        const val DOCUMENT = 9
        const val STORY = 10
        const val REMOTE_PLAY_AUDIO = 11
    }
}