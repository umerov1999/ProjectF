package dev.ragnarok.fenrir.media.voice

import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.util.Optional

interface IVoicePlayer {
    fun toggle(id: Int, audio: VoiceMessage): Boolean
    val progress: Float
    fun setCallback(listener: IPlayerStatusListener?)
    fun setErrorListener(errorListener: IErrorListener?)
    val playingVoiceId: Optional<Int>
    val isSupposedToPlay: Boolean
    val isPlaybackSpeed: Boolean
    fun togglePlaybackSpeed()
    fun release()
    interface IPlayerStatusListener {
        fun onPlayerStatusChange(status: Int)
    }

    interface IErrorListener {
        fun onPlayError(t: Throwable?)
    }

    companion object {
        const val STATUS_NO_PLAYBACK = 0
        const val STATUS_PREPARING = 1
        const val STATUS_PREPARED = 2
    }
}