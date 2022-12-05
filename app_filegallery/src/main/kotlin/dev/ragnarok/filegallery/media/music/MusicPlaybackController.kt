package dev.ragnarok.filegallery.media.music

import android.app.Activity
import android.content.*
import android.os.IBinder
import android.os.RemoteException
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.Logger
import dev.ragnarok.filegallery.util.existfile.AbsFileExist
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*

object MusicPlaybackController {
    val Audios: MutableMap<Int, ArrayList<Audio>> = LinkedHashMap()
    private val mConnectionMap: WeakHashMap<Context, ServiceBinder> = WeakHashMap()
    private val SERVICE_BIND_PUBLISHER = PublishSubject.create<Int>()
    private val TAG = MusicPlaybackController::class.java.simpleName
    var mService: IAudioPlayerService? = null

    lateinit var tracksExist: AbsFileExist
    private var sForegroundActivities = 0
    fun registerBroadcast(appContext: Context) {
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent == null || intent.action == null) return
                var result = PlayerStatus.SERVICE_KILLED
                when (intent.action) {
                    MusicPlaybackService.PREPARED, MusicPlaybackService.PLAYSTATE_CHANGED -> result =
                        PlayerStatus.UPDATE_PLAY_PAUSE
                    MusicPlaybackService.SHUFFLEMODE_CHANGED -> result =
                        PlayerStatus.SHUFFLEMODE_CHANGED
                    MusicPlaybackService.REPEATMODE_CHANGED -> result =
                        PlayerStatus.REPEATMODE_CHANGED
                    MusicPlaybackService.META_CHANGED -> result = PlayerStatus.UPDATE_TRACK_INFO
                    MusicPlaybackService.QUEUE_CHANGED -> result = PlayerStatus.UPDATE_PLAY_LIST
                }
                SERVICE_BIND_PUBLISHER.onNext(result)
            }
        }
        val filter = IntentFilter()
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED)
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED)
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED)
        filter.addAction(MusicPlaybackService.META_CHANGED)
        filter.addAction(MusicPlaybackService.PREPARED)
        filter.addAction(MusicPlaybackService.QUEUE_CHANGED)
        appContext.registerReceiver(receiver, filter)
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

    fun observeServiceBinding(): Observable<Int> {
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
        val durationFormat = context.resources.getString(
            if (hours == 0L) R.string.durationformatshort else R.string.durationformatlong
        )
        return String.format(durationFormat, hours, mins, pSecs)
    }

    /**
     * Changes to the next track
     */
    operator fun next() {
        try {
            mService?.next()
        } catch (ignored: RemoteException) {
        }
    }

    val isInitialized: Boolean
        get() {
            try {
                return mService?.isInitialized == true
            } catch (ignored: RemoteException) {

            }
            return false
        }

    val isPreparing: Boolean
        get() {
            try {
                return mService?.isPreparing == true
            } catch (ignored: RemoteException) {
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
        } catch (ignored: Exception) {
        }
    }

    fun stop() {
        try {
            mService?.stop()
        } catch (ignored: Exception) {
        }
    }

    fun closeMiniPlayer() {
        try {
            mService?.closeMiniPlayer()
        } catch (ignored: Exception) {
        }
    }

    val miniPlayerVisibility: Boolean
        get() {
            if (!Settings.get().main().isShow_mini_player()) return false
            try {
                return mService?.miniplayerVisibility == true
            } catch (ignored: Exception) {
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
        } catch (ignored: RemoteException) {
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
        } catch (ignored: RemoteException) {
        }
    }

    fun canPlayAfterCurrent(audio: Audio): Boolean {
        try {
            return mService?.canPlayAfterCurrent(audio) == true
        } catch (ignored: RemoteException) {
        }
        return false
    }

    fun playAfterCurrent(audio: Audio) {
        try {
            mService?.playAfterCurrent(audio)
        } catch (ignored: RemoteException) {
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    val isPlaying: Boolean
        get() {
            try {
                return mService?.isPlaying == true
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
            }
            return 0
        }

    val currentAudio: Audio?
        get() {
            try {
                return mService?.currentAudio
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
            }
            return null
        }
    val albumCoverBig: String?
        get() {
            try {
                return mService?.albumCover
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
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
            } catch (ignored: RemoteException) {
            }
            return emptyList()
        }

    /**
     * Called when one of the lists should refresh or requery.
     */
    fun refresh() {
        try {
            mService?.refresh()
        } catch (ignored: RemoteException) {
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
        } catch (ignored: RemoteException) {
        }
    }

    fun skip(position: Int) {
        try {
            mService?.skip(position)
        } catch (ignored: RemoteException) {
        }
    }

    /**
     * @return The current position time of the track
     */
    fun position(): Long {
        try {
            return mService?.position() ?: -1
        } catch (ignored: RemoteException) {
        }
        return -1
    }

    /**
     * @return The total length of the current track
     */
    fun duration(): Long {
        try {
            return mService?.duration() ?: -1
        } catch (ignored: RemoteException) {
        }
        return 0
    }

    fun bufferPercent(): Int {
        try {
            return mService?.bufferPercent ?: 0
        } catch (ignored: RemoteException) {
        }
        return 0
    }

    fun bufferPosition(): Long {
        try {
            return mService?.bufferPosition ?: 0
        } catch (ignored: RemoteException) {
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

    /**
     * Used to build and show a notification when player is sent into the
     * background
     *
     * @param context The [Context] to use.
     */
    fun notifyForegroundStateChanged(context: Context, inForeground: Boolean) {
        val old = sForegroundActivities
        if (inForeground) {
            sForegroundActivities++
        } else {
            sForegroundActivities--
            if (sForegroundActivities < 0) sForegroundActivities = 0
        }
        if (old == 0 || sForegroundActivities == 0) {
            try {
                val nowInForeground = sForegroundActivities != 0
                Logger.d(TAG, "notifyForegroundStateChanged, nowInForeground: $nowInForeground")
                val intent = Intent(context, MusicPlaybackService::class.java)
                intent.action = MusicPlaybackService.FOREGROUND_STATE_CHANGED
                intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, nowInForeground)
                context.startService(intent)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = IAudioPlayerService.Stub.asInterface(service)
            mCallback?.onServiceConnected(className, service)
            SERVICE_BIND_PUBLISHER.onNext(PlayerStatus.UPDATE_PLAY_LIST)
            SERVICE_BIND_PUBLISHER.onNext(PlayerStatus.UPDATE_TRACK_INFO)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mCallback?.onServiceDisconnected(className)
            mService = null
            SERVICE_BIND_PUBLISHER.onNext(PlayerStatus.SERVICE_KILLED)
        }
    }

    class ServiceToken(val mWrappedContext: ContextWrapper)
}
