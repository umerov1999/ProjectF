package dev.ragnarok.fenrir.media.video

import android.content.Context
import android.view.SurfaceHolder
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.model.VideoSize

interface IVideoPlayer {
    fun updateSource(
        context: Context,
        url: String?,
        config: ProxyConfig?,
        @InternalVideoSize size: Int
    )

    fun play()
    fun pause()
    fun release()
    val duration: Long
    val currentPosition: Long
    fun seekTo(position: Long)
    val isPlaying: Boolean
    val bufferPercentage: Int
    val bufferPosition: Long
    fun setSurfaceHolder(holder: SurfaceHolder?)
    val isPlaybackSpeed: Boolean
    fun togglePlaybackSpeed()
    fun addVideoSizeChangeListener(listener: IVideoSizeChangeListener)
    fun removeVideoSizeChangeListener(listener: IVideoSizeChangeListener)
    interface IVideoSizeChangeListener {
        fun onVideoSizeChanged(player: IVideoPlayer, size: VideoSize)
    }

    interface IUpdatePlayListener {
        fun onPlayChanged(isPause: Boolean)
    }
}