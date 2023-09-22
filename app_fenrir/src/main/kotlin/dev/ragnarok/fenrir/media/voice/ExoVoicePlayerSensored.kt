package dev.ragnarok.fenrir.media.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.PlayWhenReadyChangeReason
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.media.exo.ExoUtil.pausePlayer
import dev.ragnarok.fenrir.media.exo.ExoUtil.startPlayer
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.isPreparing
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playOrPause
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoVoicePlayerSensored(context: Context, config: ProxyConfig?) : IVoicePlayer,
    SensorEventListener {
    private val app: Context = context.applicationContext
    private val proxyConfig: ProxyConfig? = config
    private val sensorManager: SensorManager?
    private val proxym: Sensor?
    private val proximityWakelock: PowerManager.WakeLock?
    private val headset: MusicIntentReceiver
    private var exoPlayer: ExoPlayer? = null
    private var status: Int
    private var playingEntry: AudioEntry? = null
    private var supposedToBePlaying = false
    private var statusListener: IPlayerStatusListener? = null
    private var errorListener: IErrorListener? = null
    private var isProximityNear = false
    private var isPlaying = false
    private var HasPlaying: Boolean
    private var Registered: Boolean
    private var ProximitRegistered: Boolean
    private var isHeadset = false
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

    internal fun RegisterCallBack() {
        if (Registered) return
        try {
            Registered = true
            if (MusicPlaybackController.isPlaying || isPreparing) {
                playOrPause()
                HasPlaying = true
            }
            isProximityNear = false
            isHeadset = false
            exoPlayer?.setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(
                    C.USAGE_MEDIA
                ).build(), true
            )
            sensorManager?.registerListener(this, proxym, SensorManager.SENSOR_DELAY_NORMAL)
            val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            app.registerReceiver(headset, filter)
        } catch (ignored: Exception) {
        }
    }

    internal fun UnRegisterCallBack() {
        if (!Registered) return
        try {
            Registered = false
            sensorManager?.unregisterListener(this)
            app.unregisterReceiver(headset)
            if (HasPlaying) {
                playOrPause()
            }
            HasPlaying = false
            if (ProximitRegistered) {
                ProximitRegistered = false
                proximityWakelock?.release()
            }
            isProximityNear = false
            isHeadset = false
            isPlaying = false
        } catch (ignored: Exception) {
        }
    }

    private fun setStatus(status: Int) {
        if (this.status != status) {
            this.status = status
            statusListener?.onPlayerStatusChange(status)
        }
    }

    private fun preparePlayer() {
        isProximityNear = false
        isHeadset = false
        isPlaying = false
        setStatus(IVoicePlayer.STATUS_PREPARING)
        var extensionRenderer = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        when (Settings.get().main().fFmpegPlugin) {
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

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: @PlayWhenReadyChangeReason Int
            ) {
                if (isPlaying != playWhenReady) {
                    isPlaying = playWhenReady
                    if (isPlaying) {
                        RegisterCallBack()
                    } else {
                        UnRegisterCallBack()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onExoPlayerException(error)
                UnRegisterCallBack()
            }
        })
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
                UnRegisterCallBack()
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
        try {
            if (exoPlayer != null) {
                exoPlayer?.stop()
                exoPlayer?.release()
                UnRegisterCallBack()
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isHeadset) return
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val newIsNear = event.values[0] < event.sensor.maximumRange.coerceAtMost(3f)
            if (newIsNear != isProximityNear) {
                isProximityNear = newIsNear
                try {
                    if (isProximityNear) {
                        exoPlayer?.setAudioAttributes(
                            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                                .setUsage(
                                    C.USAGE_VOICE_COMMUNICATION
                                ).build(), false
                        )
                        if (!ProximitRegistered) {
                            ProximitRegistered = true
                            proximityWakelock?.acquire(10 * 60 * 1000L /*10 minutes*/)
                        }
                    } else {
                        if (ProximitRegistered) {
                            ProximitRegistered = false
                            proximityWakelock?.release(1) // this is non-public API before L
                        }
                        exoPlayer?.setAudioAttributes(
                            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .setUsage(
                                    C.USAGE_MEDIA
                                ).build(), true
                        )
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    private inner class MusicIntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                when (intent.getIntExtra("state", -1)) {
                    0 -> if (isHeadset) {
                        isHeadset = false
                    }

                    1 -> if (!isHeadset) {
                        isHeadset = true
                        isProximityNear = false
                        try {
                            if (ProximitRegistered) {
                                ProximitRegistered = false
                                proximityWakelock?.release(1) // this is non-public API before L
                            }
                            exoPlayer?.setAudioAttributes(
                                AudioAttributes.Builder().setContentType(
                                    C.AUDIO_CONTENT_TYPE_MUSIC
                                ).setUsage(C.USAGE_MEDIA).build(), true
                            )
                        } catch (ignored: Exception) {
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    companion object {
        internal val isOpusSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || Settings.get()
                .main().isEnable_native || Settings.get().main().fFmpegPlugin != 0
    }

    init {
        status = IVoicePlayer.STATUS_NO_PLAYBACK
        headset = MusicIntentReceiver()
        sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        proxym = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximityWakelock =
            (app.getSystemService(Context.POWER_SERVICE) as PowerManager?)?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "fenrir:voip=proxim"
            )
        Registered = false
        ProximitRegistered = false
        HasPlaying = false
    }
}
