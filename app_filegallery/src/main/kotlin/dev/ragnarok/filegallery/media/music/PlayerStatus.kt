package dev.ragnarok.filegallery.media.music

import androidx.annotation.IntDef

@IntDef(
    PlayerStatus.SERVICE_KILLED,
    PlayerStatus.SHUFFLEMODE_CHANGED,
    PlayerStatus.REPEATMODE_CHANGED,
    PlayerStatus.UPDATE_TRACK_INFO,
    PlayerStatus.UPDATE_PLAY_PAUSE,
    PlayerStatus.UPDATE_PLAY_LIST
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class PlayerStatus {
    companion object {
        const val SERVICE_KILLED = 0
        const val SHUFFLEMODE_CHANGED = 1
        const val REPEATMODE_CHANGED = 2
        const val UPDATE_TRACK_INFO = 3
        const val UPDATE_PLAY_PAUSE = 4
        const val UPDATE_PLAY_LIST = 5
    }
}