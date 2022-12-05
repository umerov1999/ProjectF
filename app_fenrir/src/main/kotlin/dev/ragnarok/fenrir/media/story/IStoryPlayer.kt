package dev.ragnarok.fenrir.media.story

import android.view.SurfaceHolder
import dev.ragnarok.fenrir.model.VideoSize

interface IStoryPlayer {
    val videoSize: VideoSize?
    fun play()
    fun pause()
    fun setDisplay(holder: SurfaceHolder?)
    fun release()
    fun addVideoSizeChangeListener(listener: IVideoSizeChangeListener)
    fun addStatusChangeListener(listener: IStatusChangeListener)
    fun removeVideoSizeChangeListener(listener: IVideoSizeChangeListener)
    fun removeStatusChangeListener(listener: IStatusChangeListener)
    var playerStatus: Int

    interface IStatus {
        companion object {
            const val INIT = 1
            const val PREPARING = 2
            const val PREPARED = 3
            const val ENDED = 4
        }
    }

    interface IVideoSizeChangeListener {
        fun onVideoSizeChanged(player: IStoryPlayer, size: VideoSize)
    }

    interface IStatusChangeListener {
        fun onPlayerStatusChange(player: IStoryPlayer, previousStatus: Int, currentStatus: Int)
    }
}