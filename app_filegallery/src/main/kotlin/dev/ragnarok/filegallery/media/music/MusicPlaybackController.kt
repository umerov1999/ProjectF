package dev.ragnarok.filegallery.media.music

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.myEmit
import dev.ragnarok.filegallery.util.existfile.AbsFileExist
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.WeakHashMap

object MusicPlaybackController {
    private val mConnectionMap: WeakHashMap<Context, ServiceBinder> = WeakHashMap()
    private val SERVICE_BIND_PUBLISHER =
        MutableSharedFlow<Int>(replay = 1)
    var mService: IAudioPlayerService? = null

    lateinit var tracksExist: AbsFileExist

    fun publishFromServiceState(action: String) {
        val result = when (action) {
            MusicPlaybackService.PREPARED, MusicPlaybackService.PLAYSTATE_CHANGED -> PlayerStatus.UPDATE_PLAY_PAUSE

            MusicPlaybackService.SHUFFLEMODE_CHANGED -> PlayerStatus.SHUFFLEMODE_CHANGED

            MusicPlaybackService.REPEATMODE_CHANGED -> PlayerStatus.REPEATMODE_CHANGED

            MusicPlaybackService.META_CHANGED -> PlayerStatus.UPDATE_TRACK_INFO
            MusicPlaybackService.QUEUE_CHANGED -> PlayerStatus.UPDATE_PLAY_LIST
            else -> PlayerStatus.SERVICE_KILLED
        }
        SERVICE_BIND_PUBLISHER.myEmit(result)
    }

    fun bindToServiceWithoutStart(
        realActivity: Activity?,
        callback: ServiceConnection?
    ): ServiceToken? {
        val contextWrapper = ContextWrapper(realActivity)
        val binder = ServiceBinder(callback)
        if (contextWrapper.bindService(
                Intent().setClass(
                    contextWrapper,
                    MusicPlaybackService::class.java
                ), binder, 0
            )
        ) {
            mConnectionMap[contextWrapper] = binder
            return ServiceToken(contextWrapper)
        }
        return null
    }

    /**
     * @param token The [ServiceToken] to unbind from
     */
    fun unbindFromService(token: ServiceToken?) {
        if (token == null) {
            return
        }
        val mContextWrapper = token.mWrappedContext
        val mBinder = mConnectionMap.remove(mContextWrapper) ?: return
        mContextWrapper.unbindService(mBinder)
        if (mConnectionMap.isEmpty()) {
            mService = null
        }
    }

    fun observeServiceBinding(): SharedFlow<Int> {
        return SERVICE_BIND_PUBLISHER
    }

    fun makeTimeString(context: Context, secs: Long): String {
        if (secs < 0) {
            return "--:--"
        }
        var pSecs = secs
        val hours: Long = pSecs / 3600
        pSecs -= hours * 3600
        val mins: Long = pSecs / 60
        pSecs -= mins * 60
        return if (hours == 0L) context.resources.getString(
            R.string.duration_format_min_sec,
            mins,
            pSecs
        ) else context.resources.getString(
            R.string.duration_format_hour_min_sec,
            hours,
            mins,
            pSecs
        )
    }

    /**
     * Changes to the next track
     */
    operator fun next() {
        try {
            mService?.next()
        } catch (_: RemoteException) {
        }
    }

    val isInitialized: Boolean
        get() {
            try {
                return mService?.isInitialized == true
            } catch (_: RemoteException) {

            }
            return false
        }

    val isPreparing: Boolean
        get() {
            try {
                return mService?.isPreparing == true
            } catch (_: RemoteException) {
            }
            return false
        }

    /**
     * Changes to the previous track.
     */
    fun previous(context: Context) {
        val previous = Intent(context, MusicPlaybackService::class.java)
        previous.action = MusicPlaybackService.PREVIOUS_ACTION
        context.startService(previous)
    }

    /**
     * Plays or pauses the music.
     */
    fun playOrPause() {
        try {
            if (mService?.isPlaying == true) {
                mService?.pause()
            } else {
                mService?.play()
            }
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            mService?.stop()
        } catch (_: Exception) {
        }
    }

    fun closeMiniPlayer() {
        try {
            mService?.closeMiniPlayer()
        } catch (_: Exception) {
        }
    }

    val miniPlayerVisibility: Boolean
        get() {
            if (!Settings.get().main().isShow_mini_player) return false
            try {
                return mService?.miniplayerVisibility == true
            } catch (_: Exception) {
            }
            return false
        }

    /**
     * Cycles through the repeat options.
     */
    fun cycleRepeat() {
        try {
            mService?.let {
                when (it.repeatMode) {
                    MusicPlaybackService.REPEAT_NONE -> it.repeatMode =
                        MusicPlaybackService.REPEAT_ALL

                    MusicPlaybackService.REPEAT_ALL -> {
                        it.repeatMode = MusicPlaybackService.REPEAT_CURRENT
                        if (it.shuffleMode != MusicPlaybackService.SHUFFLE_NONE) {
                            it.shuffleMode = MusicPlaybackService.SHUFFLE_NONE
                        }
                    }

                    else -> it.repeatMode = MusicPlaybackService.REPEAT_NONE
                }
            }
        } catch (_: RemoteException) {
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    fun cycleShuffle() {
        try {
            mService?.let {
                when (it.shuffleMode) {
                    MusicPlaybackService.SHUFFLE_NONE -> {
                        it.shuffleMode = MusicPlaybackService.SHUFFLE
                        if (it.repeatMode == MusicPlaybackService.REPEAT_CURRENT) {
                            it.repeatMode = MusicPlaybackService.REPEAT_ALL
                        }
                    }

                    MusicPlaybackService.SHUFFLE -> it.shuffleMode =
                        MusicPlaybackService.SHUFFLE_NONE

                    else -> {}
                }
            }
        } catch (_: RemoteException) {
        }
    }

    fun canPlayAfterCurrent(audio: Audio): Boolean {
        try {
            return mService?.canPlayAfterCurrent(audio) == true
        } catch (_: RemoteException) {
        }
        return false
    }

    fun playAfterCurrent(audio: Audio) {
        try {
            mService?.playAfterCurrent(audio)
        } catch (_: RemoteException) {
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    val isPlaying: Boolean
        get() {
            try {
                return mService?.isPlaying == true
            } catch (_: RemoteException) {
            }
            return false
        }

    /**
     * @return The current shuffle mode.
     */
    val shuffleMode: Int
        get() {
            try {
                return mService?.shuffleMode ?: MusicPlaybackService.SHUFFLE_NONE
            } catch (_: RemoteException) {
            }
            return 0
        }

    /**
     * @return The current repeat mode.
     */
    val repeatMode: Int
        get() {
            try {
                return mService?.repeatMode ?: MusicPlaybackService.REPEAT_NONE
            } catch (_: RemoteException) {
            }
            return 0
        }

    val currentAudio: Audio?
        get() {
            try {
                return mService?.currentAudio
            } catch (_: RemoteException) {
            }
            return null
        }
    val currentAudioPos: Int?
        get() {
            try {
                val ret = mService?.currentAudioPos
                if (ret != null) {
                    return if (ret < 0) {
                        null
                    } else {
                        ret
                    }
                }
            } catch (_: RemoteException) {
            }
            return null
        }

    /**
     * @return The current track name.
     */
    val trackName: String?
        get() {
            try {
                return mService?.trackName
            } catch (_: RemoteException) {
            }
            return null
        }

    /**
     * @return The current album name.
     */
    val albumName: String?
        get() {
            try {
                return mService?.albumName
            } catch (_: RemoteException) {
            }
            return null
        }

    /**
     * @return The current artist name.
     */
    val artistName: String?
        get() {
            try {
                return mService?.artistName
            } catch (_: RemoteException) {
            }
            return null
        }
    val albumCoverBig: String?
        get() {
            try {
                return mService?.albumCover
            } catch (_: RemoteException) {
            }
            return null
        }

    /**
     * @return The current song Id.
     */
    val audioSessionId: Int
        get() {
            try {
                return mService?.audioSessionId ?: -1
            } catch (_: RemoteException) {
            }
            return -1
        }

    /**
     * @return The queue.
     */
    val queue: List<Audio>
        get() {
            try {
                return mService?.queue ?: emptyList()
            } catch (_: RemoteException) {
            }
            return emptyList()
        }

    /**
     * Called when one of the lists should refresh or requery.
     */
    fun refresh() {
        try {
            mService?.refresh()
        } catch (_: RemoteException) {
        }
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    fun seek(position: Long) {
        try {
            mService?.seek(position)
        } catch (_: RemoteException) {
        }
    }

    fun skip(position: Int) {
        try {
            mService?.skip(position)
        } catch (_: RemoteException) {
        }
    }

    fun doNotDestroyWhenActivityRecreated() {
        try {
            mService?.doNotDestroyWhenActivityRecreated()
        } catch (_: RemoteException) {
        }
    }

    /**
     * @return The current position time of the track
     */
    fun position(): Long {
        try {
            return mService?.position() ?: -1
        } catch (_: RemoteException) {
        }
        return -1
    }

    /**
     * @return The total length of the current track
     */
    fun duration(): Long {
        try {
            return mService?.duration() ?: -1
        } catch (_: RemoteException) {
        }
        return 0
    }

    fun bufferPercent(): Int {
        try {
            return mService?.bufferPercent ?: 0
        } catch (_: RemoteException) {
        }
        return 0
    }

    fun bufferPosition(): Long {
        try {
            return mService?.bufferPosition ?: 0
        } catch (_: RemoteException) {
        }
        return 0
    }

    fun isNowPlayingOrPreparingOrPaused(audio: Audio): Boolean {
        return audio == currentAudio
    }

    fun playerStatus(): Int {
        if (isPreparing || isPlaying) return 1
        return if (currentAudio != null) 2 else 0
    }

    class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = IAudioPlayerService.Stub.asInterface(service)
            mCallback?.onServiceConnected(className, service)
            SERVICE_BIND_PUBLISHER.myEmit(PlayerStatus.UPDATE_PLAY_LIST)
            SERVICE_BIND_PUBLISHER.myEmit(PlayerStatus.UPDATE_TRACK_INFO)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mCallback?.onServiceDisconnected(className)
            mService = null
            SERVICE_BIND_PUBLISHER.myEmit(PlayerStatus.SERVICE_KILLED)
        }
    }

    class ServiceToken(val mWrappedContext: ContextWrapper)
}
