package dev.ragnarok.filegallery.media.video

import android.content.Context
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.media.exo.ExoUtil.pausePlayer
import dev.ragnarok.filegallery.media.exo.ExoUtil.startPlayer
import dev.ragnarok.filegallery.media.video.IVideoPlayer.IUpdatePlayListener
import dev.ragnarok.filegallery.util.Utils
import java.lang.ref.WeakReference

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoVideoPlayer(
    context: Context,
    url: String?,
    playListener: IUpdatePlayListener
) : IVideoPlayer {
    private val player: ExoPlayer?
    private val onVideoSizeChangedListener = OnVideoSizeChangedListener(this)
    private val videoSizeChangeListeners: MutableList<IVideoPlayer.IVideoSizeChangeListener> =
        ArrayList(1)
    private var source: MediaSource
    private var supposedToBePlaying = false
    private var prepareCalled = false
    private var playbackSpeed = false
    override fun updateSource(
        context: Context,
        url: String?
    ) {
        player?.stop()
        source = createMediaSource(
            context,
            url
        )
        player?.setMediaSource(source)
        player?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        player?.prepare()
        startPlayer(player)
    }

    private fun createPlayer(context: Context): ExoPlayer {
        val ret = ExoPlayer.Builder(context, DefaultRenderersFactory(context)).build()
        ret.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).setUsage(
                C.USAGE_MEDIA
            ).build(), true
        )
        return ret
    }

    override fun play() {
        if (supposedToBePlaying) {
            return
        }
        supposedToBePlaying = true
        if (!prepareCalled) {
            player?.setMediaSource(source)
            player?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
            player?.prepare()
            prepareCalled = true
        }
        startPlayer(player)
    }

    override fun pause() {
        if (!supposedToBePlaying) {
            return
        }
        supposedToBePlaying = false
        pausePlayer(player)
    }

    override val isPlaybackSpeed: Boolean
        get() = playbackSpeed

    override fun togglePlaybackSpeed() {
        playbackSpeed = !playbackSpeed
        player?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
    }

    override fun release() {
        if (player != null) {
            try {
                player.removeListener(onVideoSizeChangedListener)
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val duration: Long
        get() = player?.duration ?: 1

    override val currentPosition: Long
        get() = player?.currentPosition ?: 0

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    override val isPlaying: Boolean
        get() = supposedToBePlaying

    override val bufferPercentage: Int
        get() = player?.bufferedPercentage ?: 0

    override val bufferPosition: Long
        get() = player?.bufferedPosition ?: 0

    override fun setSurfaceHolder(holder: SurfaceHolder?) {
        player?.setVideoSurfaceHolder(holder)
    }

    internal fun onVideoSizeChanged(w: Int, h: Int) {
        for (listener in videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, w, h)
        }
    }

    override fun addVideoSizeChangeListener(listener: IVideoPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.add(listener)
    }

    override fun removeVideoSizeChangeListener(listener: IVideoPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.remove(listener)
    }

    private class OnVideoSizeChangedListener(player: ExoVideoPlayer) :
        Player.Listener {
        val ref: WeakReference<ExoVideoPlayer> = WeakReference(player)
        override fun onVideoSizeChanged(size: VideoSize) {
            val player = ref.get()
            player?.onVideoSizeChanged(size.width, size.height)
        }

        override fun onRenderedFirstFrame() {}

    }

    companion object {
        internal fun createMediaSource(context: Context, url: String?): MediaSource {
            val userAgent: String = Constants.USER_AGENT
            return if (url?.contains("file://") == true || url?.contains("content://") == true) {
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
                    .createMediaSource(Utils.makeMediaItem(url))
            } else ProgressiveMediaSource.Factory(Utils.getExoPlayerFactory(userAgent))
                .createMediaSource(Utils.makeMediaItem(url))
        }
    }

    init {
        player = createPlayer(context)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player.seekTo(0)
                    pause()
                    playListener.onPlayChanged(true)
                }
            }
        })
        player.addListener(onVideoSizeChangedListener)
        source = createMediaSource(
            context,
            url
        )
    }
}
