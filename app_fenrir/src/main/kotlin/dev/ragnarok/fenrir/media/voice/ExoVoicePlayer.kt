package dev.ragnarok.fenrir.media.voice

import android.content.Context
import android.os.Build
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.media.exo.ExoUtil.pausePlayer
import dev.ragnarok.fenrir.media.exo.ExoUtil.startPlayer
import dev.ragnarok.fenrir.media.voice.IVoicePlayer.IErrorListener
import dev.ragnarok.fenrir.media.voice.IVoicePlayer.IPlayerStatusListener
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.getExoPlayerFactory
import dev.ragnarok.fenrir.util.Utils.makeMediaItem

class ExoVoicePlayer(context: Context, config: ProxyConfig?) : IVoicePlayer {
    private val app: Context = context.applicationContext
    private val proxyConfig: ProxyConfig? = config
    private var exoPlayer: ExoPlayer? = null
    private var status: Int
    private var playingEntry: AudioEntry? = null
    private var supposedToBePlaying = false
    private var statusListener: IPlayerStatusListener? = null
    private var errorListener: IErrorListener? = null
    private var playbackSpeed = false
    override fun toggle(id: Int, audio: VoiceMessage): Boolean {
        if (playingEntry?.id == id) {
            setSupposedToBePlaying(!isSupposedToPlay)
            return false
        }
        release()
        playingEntry = AudioEntry(id, audio)
        supposedToBePlaying = true
        preparePlayer()
        return true
    }

    private fun setStatus(status: Int) {
        if (this.status != status) {
            this.status = status
            statusListener?.onPlayerStatusChange(status)
        }
    }

    private fun preparePlayer() {
        setStatus(IVoicePlayer.STATUS_PREPARING)
        var extensionRenderer = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        when (Settings.get().other().fFmpegPlugin) {
            0 -> extensionRenderer = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            1 -> extensionRenderer = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            2 -> extensionRenderer = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        }
        exoPlayer = ExoPlayer.Builder(
            app,
            DefaultRenderersFactory(app).setExtensionRendererMode(extensionRenderer)
        ).build()
        exoPlayer?.setWakeMode(C.WAKE_MODE_NETWORK)
        val userAgent = UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
        val url = if (isOpusSupported) firstNonEmptyString(
            playingEntry?.audio?.getLinkOgg(), playingEntry?.audio?.getLinkMp3()
        ) else playingEntry?.audio?.getLinkMp3()
        val mediaSource: MediaSource =
            ProgressiveMediaSource.Factory(getExoPlayerFactory(userAgent, proxyConfig))
                .createMediaSource(
                    makeMediaItem(url)
                )
        exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: @Player.State Int) {
                onInternalPlayerStateChanged(state)
            }

            override fun onPlayerError(error: PlaybackException) {
                onExoPlayerException(error)
            }
        })
        exoPlayer?.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(
                C.USAGE_MEDIA
            ).build(), true
        )
        exoPlayer?.playWhenReady = supposedToBePlaying
        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        exoPlayer?.prepare()
    }

    internal fun onExoPlayerException(e: PlaybackException) {
        errorListener?.onPlayError(e)
    }

    internal fun onInternalPlayerStateChanged(state: @Player.State Int) {
        d("ExoVoicePlayer", "onInternalPlayerStateChanged, state: $state")
        when (state) {
            Player.STATE_READY -> setStatus(IVoicePlayer.STATUS_PREPARED)
            Player.STATE_ENDED -> {
                setSupposedToBePlaying(false)
                exoPlayer?.seekTo(0)
            }

            Player.STATE_BUFFERING, Player.STATE_IDLE -> {}
        }
    }

    private fun setSupposedToBePlaying(supposedToBePlaying: Boolean) {
        this.supposedToBePlaying = supposedToBePlaying
        if (supposedToBePlaying) {
            startPlayer(exoPlayer)
        } else {
            pausePlayer(exoPlayer)
        }
    }

    private val duration: Long
        get() = if (playingEntry == null || playingEntry?.audio == null || playingEntry?.audio?.getDuration() == 0) {
            exoPlayer?.duration?.coerceAtLeast(1L) ?: 1
        } else (playingEntry?.audio?.getDuration()?.times(1000L)) ?: 1

    override val progress: Float
        get() = when {
            exoPlayer == null -> {
                0f
            }

            status != IVoicePlayer.STATUS_PREPARED -> {
                0f
            }

            else -> {
                val duration = duration
                val position = exoPlayer?.currentPosition ?: 0
                position.toFloat() / duration.toFloat()
            }
        }


    override fun setCallback(listener: IPlayerStatusListener?) {
        statusListener = listener
    }

    override fun setErrorListener(errorListener: IErrorListener?) {
        this.errorListener = errorListener
    }

    override val playingVoiceId: Optional<Int>
        get() = playingEntry?.let { wrap(it.id) } ?: empty()


    override val isSupposedToPlay: Boolean
        get() = supposedToBePlaying

    override val isPlaybackSpeed: Boolean
        get() = playbackSpeed

    override fun togglePlaybackSpeed() {
        if (exoPlayer != null) {
            playbackSpeed = !playbackSpeed
            exoPlayer?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        }
    }

    override fun release() {
        exoPlayer?.stop()
        exoPlayer?.release()
    }

    companion object {
        internal val isOpusSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || Settings.get()
                .other().isEnable_native || Settings.get().other().fFmpegPlugin != 0
    }

    init {
        status = IVoicePlayer.STATUS_NO_PLAYBACK
    }
}