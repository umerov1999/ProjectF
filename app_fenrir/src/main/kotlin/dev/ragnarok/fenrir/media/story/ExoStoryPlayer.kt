package dev.ragnarok.fenrir.media.story

import android.view.SurfaceHolder
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.media.story.IStoryPlayer.IStatus
import dev.ragnarok.fenrir.media.story.IStoryPlayer.IStatusChangeListener
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.model.VideoSize
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Utils.getExoPlayerFactory
import dev.ragnarok.fenrir.util.Utils.makeMediaItem

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoStoryPlayer(
    private val url: String,
    private val proxyConfig: ProxyConfig?,
    private val isRepeat: Boolean
) : IStoryPlayer {
    private val videoSizeChangeListeners: MutableList<IStoryPlayer.IVideoSizeChangeListener> =
        ArrayList(1)
    private val statusChangeListeners: MutableList<IStatusChangeListener> = ArrayList(1)
    override var videoSize: VideoSize? = null
        private set
    private var playbackSpeed = false
    private val videoListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            this@ExoStoryPlayer.videoSize = VideoSize(videoSize.width, videoSize.height)
            this@ExoStoryPlayer.onVideoSizeChanged()
        }

        override fun onRenderedFirstFrame() {}
        override fun onPlaybackStateChanged(state: @Player.State Int) {
            d("FenrirExoStoryPlayer", "onPlaybackStateChanged, state: $state")
            onInternalPlayerStateChanged(state)
        }
    }
    override var playerStatus: Int = IStatus.INIT
    private var internalPlayer: ExoPlayer? = null
    private var supposedToBePlaying = false
    override fun play() {
        if (supposedToBePlaying) return
        supposedToBePlaying = true
        when (playerStatus) {
            IStatus.PREPARED -> startPlayer(internalPlayer)
            IStatus.INIT -> preparePlayer()
            IStatus.PREPARING -> {}
        }
    }

    override fun updateSource(url: String?) {
        internalPlayer?.stop()
        setStatus(IStatus.PREPARING)
        val userAgent = UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
        val mediaSource: MediaSource =
            ProgressiveMediaSource.Factory(getExoPlayerFactory(userAgent, proxyConfig))
                .createMediaSource(
                    makeMediaItem(
                        url
                    )
                )
        internalPlayer?.playWhenReady = true
        internalPlayer?.setMediaSource(mediaSource)
        internalPlayer?.prepare()
    }

    private fun preparePlayer() {
        setStatus(IStatus.PREPARING)
        internalPlayer = ExoPlayer.Builder(instance).build()
        val userAgent = UserAgentTool.USER_AGENT_CURRENT_ACCOUNT

        // This is the MediaSource representing the media to be played:
        // FOR SD CARD SOURCE:
        // MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
        // FOR LIVESTREAM LINK:
        val mediaSource: MediaSource =
            ProgressiveMediaSource.Factory(getExoPlayerFactory(userAgent, proxyConfig))
                .createMediaSource(
                    makeMediaItem(
                        url
                    )
                )
        internalPlayer?.repeatMode =
            if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        internalPlayer?.addListener(videoListener)
        internalPlayer?.playWhenReady = true
        internalPlayer?.setMediaSource(mediaSource)
        internalPlayer?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        internalPlayer?.prepare()
    }

    override val isPlaybackSpeed: Boolean
        get() = playbackSpeed

    override fun setPlaybackSpeed(isSpeed: Boolean) {
        playbackSpeed = isSpeed
        internalPlayer?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
    }

    internal fun onInternalPlayerStateChanged(state: @Player.State Int) {
        if (state == Player.STATE_READY) {
            setStatus(IStatus.PREPARED)
        } else if (state == Player.STATE_ENDED && !isRepeat) {
            setStatus(IStatus.ENDED)
        }
    }

    internal fun onVideoSizeChanged() {
        for (listener in videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, videoSize ?: VideoSize(1, 1))
        }
    }

    override fun pause() {
        if (!supposedToBePlaying) return
        supposedToBePlaying = false
        if (internalPlayer != null) {
            try {
                internalPlayer?.let { pausePlayer(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        internalPlayer?.setVideoSurfaceHolder(holder)
    }

    override fun release() {
        if (internalPlayer != null) {
            try {
                internalPlayer?.removeListener(videoListener)
                internalPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setStatus(newStatus: Int) {
        val oldStatus = playerStatus
        if (playerStatus == newStatus) {
            return
        }
        playerStatus = newStatus
        for (listener in statusChangeListeners) {
            listener.onPlayerStatusChange(this, oldStatus, newStatus)
        }
    }

    override fun addVideoSizeChangeListener(listener: IStoryPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.add(listener)
    }

    override fun addStatusChangeListener(listener: IStatusChangeListener) {
        statusChangeListeners.add(listener)
    }

    override fun removeVideoSizeChangeListener(listener: IStoryPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.remove(listener)
    }

    override fun removeStatusChangeListener(listener: IStatusChangeListener) {
        statusChangeListeners.remove(listener)
    }

    companion object {
        internal fun pausePlayer(internalPlayer: ExoPlayer) {
            internalPlayer.playWhenReady = false
            internalPlayer.playbackState
        }

        internal fun startPlayer(internalPlayer: ExoPlayer?) {
            internalPlayer?.playWhenReady = true
            internalPlayer?.playbackState
        }
    }
}
