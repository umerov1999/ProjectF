package dev.ragnarok.filegallery.media.music

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.PlayWhenReadyChangeReason
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso.LoadedFrom
import dev.ragnarok.filegallery.*
import dev.ragnarok.filegallery.media.exo.ExoUtil
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.AppPerms
import dev.ragnarok.filegallery.util.Logger
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.Utils.makeMediaItem
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MusicPlaybackService : Service() {
    private val mBinder: IBinder = ServiceStub(this)
    private var mPlayer: MultiPlayer? = null
    private var shutdownDelayedDisposable = Disposable.disposed()
    var isPlaying = false
        private set

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private var errorsCount = 0
    private var onceCloseMiniPlayer = false
    private var mAnyActivityInForeground = false
    private var mMediaSession: MediaSessionCompat? = null
    private var mTransportController: MediaControllerCompat.TransportControls? = null
    private var mPlayPos = -1
    private var coverAudio: String? = null
    private var coverBitmap: Bitmap? = null
    private var albumTitle: String? = null
    private var mShuffleMode = SHUFFLE_NONE
    private var mRepeatMode = REPEAT_NONE
    private var mPlayList: ArrayList<Audio>? = null
    private var mPlayListOrig: ArrayList<Audio>? = null
    private var mNotificationHelper: NotificationHelper? = null
    private var mMediaMetadataCompat: MediaMetadataCompat? = null
    private var inForeground: Boolean = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var mIntentReceiver: BroadcastReceiver? = null
    override fun onBind(intent: Intent): IBinder {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Service bound, intent = $intent")
        cancelShutdown()
        mAnyActivityInForeground = false
        return mBinder
    }

    fun goForeground(id: Int, notification: Notification?, mManager: NotificationManager?) {
        if (notification == null || mManager == null) {
            return
        }
        try {
            if (inForeground && AppPerms.hasNotificationPermissionSimple(this)) {
                mManager.notify(id, notification)
                return
            }
            if (Utils.hasQ()) {
                startForeground(
                    id,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(id, notification)
            }
            inForeground = true
        } catch (ignored: Exception) {
        }
    }

    @SuppressLint("WrongConstant")
    @Suppress("deprecation")
    fun outForeground(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !removeNotification) {
            return
        }
        inForeground = false
        if (Utils.hasNougat()) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else 0)
        } else {
            stopForeground(removeNotification)
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Service unbound")
        if (isPlaying || mAnyActivityInForeground) {
            Logger.d(
                TAG,
                "onUnbind, mIsSupposedToBePlaying || mPausedByTransientLossOfFocus || isPreparing()"
            )
            return true
        }
        Logger.d(TAG, "onUnbind, stopSelf(mServiceStartId)")
        terminate()
        return true
    }

    override fun onRebind(intent: Intent) {
        cancelShutdown()
        mAnyActivityInForeground = false
    }

    override fun onCreate() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Creating service")
        super.onCreate()
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            wakeLock?.setReferenceCounted(false)
        }
        mNotificationHelper = NotificationHelper(this)
        setUpRemoteControlClient()

        IDLE_DELAY = Settings.get().main().musicLifecycle

        mPlayer = MultiPlayer(this)
        val filter = IntentFilter()
        filter.addAction(SERVICECMD)
        filter.addAction(TOGGLEPAUSE_ACTION)
        filter.addAction(SWIPE_DISMISS_ACTION)
        filter.addAction(PAUSE_ACTION)
        filter.addAction(STOP_ACTION)
        filter.addAction(NEXT_ACTION)
        filter.addAction(PREVIOUS_ACTION)
        filter.addAction(REPEAT_ACTION)
        filter.addAction(SHUFFLE_ACTION)

        mIntentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleCommandIntent(intent)
            }
        }
        ContextCompat.registerReceiver(
            this,
            mIntentReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Listen for the idle state
        scheduleDelayedShutdown()
        notifyChange(META_CHANGED)
    }

    private fun setUpRemoteControlClient() {
        mMediaSession =
            MediaSessionCompat(application, resources.getString(R.string.app_name), null, null)
        val playbackStateCompat = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                position(),
                1.0f
            )
            .build()
        mMediaSession?.setPlaybackState(playbackStateCompat)
        mMediaSession?.setCallback(mMediaSessionCallback)
        mMediaSession?.isActive = true
        updateRemoteControlClient(META_CHANGED)
        mTransportController = mMediaSession?.controller?.transportControls
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                play()
            }

            override fun onPause() {
                super.onPause()
                pause()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                gotoNext(true)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                prev()
            }

            override fun onStop() {
                super.onStop()
                pause()
                Logger.d(javaClass.simpleName, "Stopping services. onStop()")
                terminate()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                seek(pos)
            }
        }

    override fun onDestroy() {
        shutdownDelayedDisposable.dispose()
        wakeLock?.release()
        wakeLock = null
        if (Constants.IS_DEBUG) Logger.d(TAG, "Destroying service")
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
        mPlayer?.release()
        mPlayer = null
        mMediaSession?.release()
        mMediaSession = null
        mIntentReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
            }
        }
        mIntentReceiver = null
        mNotificationHelper?.killNotification()
        mNotificationHelper = null
        super.onDestroy()
    }

    /**
     * {@inheritDoc}
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Got new intent $intent, startId = $startId")
        intent?.let {
            handleCommandIntent(it)
            MediaButtonReceiver.handleIntent(mMediaSession, it)
        }
        scheduleDelayedShutdown()
        return START_STICKY
    }

    internal fun terminate() {
        if (!stopSelfResult(-1)) {
            onDestroy()
        }
    }

    internal fun releaseServiceUiAndStop() {
        if (isPlaying || mAnyActivityInForeground) {
            return
        }
        if (Constants.IS_DEBUG) Logger.d(TAG, "Nothing is playing anymore, releasing notification")
        terminate()
    }

    internal fun handleCommandIntent(intent: Intent) {
        val action = intent.action
        val command = if (SERVICECMD == action) intent.getStringExtra(CMDNAME) else null
        if (Constants.IS_DEBUG) Logger.d(
            TAG,
            "handleCommandIntent: action = $action, command = $command"
        )
        if (SWIPE_DISMISS_ACTION == action) {
            terminate()
        }
        if (CMDNEXT == command || NEXT_ACTION == action) {
            mTransportController?.skipToNext()
        }
        if (CMDPREVIOUS == command || PREVIOUS_ACTION == action) {
            mTransportController?.skipToPrevious()
        }
        if (CMDTOGGLEPAUSE == command || TOGGLEPAUSE_ACTION == action) {
            if (isPlaying) {
                mTransportController?.pause()
            } else {
                mTransportController?.play()
            }
        }
        if (CMDPAUSE == command || PAUSE_ACTION == action) {
            mTransportController?.pause()
        }
        if (CMDPLAY == command) {
            play()
        }
        if (CMDSTOP == command || STOP_ACTION == action) {
            mTransportController?.pause()
            seek(0)
            releaseServiceUiAndStop()
        }
        if (REPEAT_ACTION == action) {
            cycleRepeat()
        }
        if (SHUFFLE_ACTION == action) {
            cycleShuffle()
        }
        if (CMDPLAYLIST == action) {
            val apiAudios: ArrayList<Audio>? =
                intent.getParcelableArrayListExtraCompat(Extra.AUDIOS)
            val position = intent.getIntExtra(Extra.POSITION, 0)
            val forceShuffle = intent.getIntExtra(Extra.SHUFFLE_MODE, SHUFFLE_NONE)
            shuffleMode = forceShuffle
            if (apiAudios != null)
                open(apiAudios, position)
        }
    }

    /**
     * Updates the notification, considering the current play and activity state
     */
    private fun updateNotification() {
        mNotificationHelper?.buildNotification(
            this,
            artistName,
            trackName,
            isPlaying,
            Utils.firstNonNull(
                coverBitmap,
                BitmapFactory.decodeResource(resources, R.drawable.generic_audio_nowplaying_service)
            ),
            mMediaSession?.sessionToken
        )
    }

    private fun scheduleDelayedShutdown() {
        shutdownDelayedDisposable.dispose()
        if (Constants.IS_DEBUG) Log.v(TAG, "Scheduling shutdown in $IDLE_DELAY ms")
        shutdownDelayedDisposable = Observable.just(Any())
            .delay(IDLE_DELAY.toLong(), TimeUnit.MILLISECONDS)
            .toMainThread()
            .subscribe { terminate() }
    }

    private fun cancelShutdown() {
        if (Constants.IS_DEBUG) Logger.d(
            TAG,
            "Cancelling delayed shutdown"
        )
        shutdownDelayedDisposable.dispose()
    }

    /**
     * Stops playback
     *
     * @param goToIdle True to go to the idle state, false otherwise
     */
    private fun stop(goToIdle: Boolean) {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Stopping playback, goToIdle = $goToIdle")
        if (mPlayer?.isInitialized == true) {
            mPlayer?.stop()
        }
        if (goToIdle) {
            scheduleDelayedShutdown()
            isPlaying = false
        } else {
            outForeground(false) //надо подумать
        }
    }

    internal val isInitialized: Boolean
        get() = mPlayer?.isInitialized == true

    internal val isPreparing: Boolean
        get() = mPlayer?.isPreparing == true

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    internal fun playCurrentTrack(UpdateMeta: Boolean) {
        synchronized(this) {
            Logger.d(TAG, "playCurrentTrack, mPlayListLen: " + Utils.safeCountOf(mPlayList))
            if (mPlayList.isNullOrEmpty()) {
                return
            }
            stop(false)
            mPlayList?.let {
                if (it.size - 1 < mPlayPos) {
                    mPlayPos = 0
                }
            }
            val current = mPlayList?.get(mPlayPos)
            openFile(current, UpdateMeta)
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     * otherwise.
     * @return The next position to play.
     */
    private fun getNextPosition(force: Boolean): Int {
        if (!force && mRepeatMode == REPEAT_CURRENT) {
            return mPlayPos.coerceAtLeast(0)
        }
        return if (mPlayPos >= Utils.safeCountOf(mPlayList) - 1) {
            if (mRepeatMode == REPEAT_NONE && !force) {
                return -1
            }
            if (mRepeatMode == REPEAT_ALL || force) {
                0
            } else -1
        } else {
            mPlayPos + 1
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    internal fun notifyChange(what: String) {
        if (Constants.IS_DEBUG) Logger.d(TAG, "notifyChange: what = $what")
        updateRemoteControlClient(what)
        if (what == POSITION_CHANGED) {
            return
        }
        MusicPlaybackController.publishFromServiceState(what)
        if (what == PLAYSTATE_CHANGED) {
            mNotificationHelper?.updatePlayState(isPlaying)
        }
    }

    /**
     * Updates the lockscreen controls.
     *
     * @param what The broadcast
     */
    private fun updateRemoteControlClient(what: String) {
        when (what) {
            PLAYSTATE_CHANGED, POSITION_CHANGED -> {
                val playState =
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                val pmc = PlaybackStateCompat.Builder()
                    .setState(playState, position(), 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
                mMediaSession?.setPlaybackState(pmc)
            }

            META_CHANGED -> fetchCoverAndUpdateMetadata()
        }
    }

    private fun fetchCoverAndUpdateMetadata() {
        updateMetadata()
        if (coverBitmap != null || albumCover.isNullOrEmpty()) {
            return
        }
        PicassoInstance.with()
            .load(albumCover)
            .into(object : BitmapTarget {
                override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                    coverBitmap = bitmap
                    updateMetadata()
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                }

            })
    }

    internal fun updateMetadata() {
        updateNotification()
        mMediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, trackName)
            //.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, Utils.firstNonNull(CoverBitmap, BitmapFactory.decodeResource(getResources(), R.drawable.generic_audio_nowplaying_service)))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration())
            .build()
        mMediaSession?.setMetadata(mMediaMetadataCompat)
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param audio The path of the file to open
     */
    fun openFile(audio: Audio?, UpdateMeta: Boolean) {
        synchronized(this) {
            if (audio == null) {
                stop(true)
                return
            }

            if (UpdateMeta) {
                errorsCount = 0
                coverAudio = null
                albumTitle = null
                coverBitmap = null
                onceCloseMiniPlayer = false
            }
            mPlayer?.setDataSource(audio)
            if (audio.thumb_image != null && UpdateMeta) {
                coverAudio = audio.thumb_image
                albumTitle = audio.artist
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            } else {
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            }
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val audioSessionId: Int
        get() {
            synchronized(this) { return mPlayer?.audioSessionId ?: -1 }
        }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val bufferPercent: Int
        get() {
            synchronized(this) { return mPlayer?.bufferPercent ?: 0 }
        }

    fun doNotDestroyWhenActivityRecreated() {
        synchronized(this) {
            mAnyActivityInForeground = true
        }
    }

    val bufferPos: Long
        get() {
            synchronized(this) { return mPlayer?.bufferPos ?: 0 }
        }

    var shuffleMode: Int
        get() = mShuffleMode
        set(shufflemode) {
            synchronized(this) {
                if (mShuffleMode == shufflemode && Utils.safeCountOf(mPlayList) > 0) {
                    return
                }
                mShuffleMode = shufflemode
                notifyChange(SHUFFLEMODE_CHANGED)
                if (mShuffleMode == SHUFFLE) {
                    mPlayList?.shuffle()
                    skip(0, true)
                } else {
                    val ps = mPlayListOrig?.indexOf(mPlayList?.get(mPlayPos))
                    ps?.let {
                        mPlayPos = if (it < 0) {
                            mPlayList?.get(mPlayPos)?.let { it1 -> mPlayListOrig?.add(0, it1) }
                            0
                        } else {
                            it
                        }
                    }
                    mPlayList?.clear()
                    mPlayListOrig?.let { mPlayList?.addAll(it) }
                    notifyChange(META_CHANGED)
                }
            }
        }

    var repeatMode: Int
        get() = mRepeatMode
        set(repeatmode) {
            synchronized(this) {
                mRepeatMode = repeatmode
                notifyChange(REPEATMODE_CHANGED)
            }
        }

    val queuePosition: Int
        get() {
            synchronized(this) { return mPlayPos }
        }

    val path: String?
        get() {
            synchronized(this) {
                val apiAudio = currentTrack ?: return null
                return apiAudio.url
            }
        }

    val albumName: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else albumTitle
            }
        }

    /**
     * Returns the album cover
     *
     * @return url
     */

    internal val albumCover: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else coverAudio
            }
        }

    val trackName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.title
            }
        }

    val artistName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.artist
            }
        }

    val currentTrack: Audio?
        get() {
            synchronized(this) {
                mPlayList?.let {
                    if (mPlayPos >= 0 && it.size > mPlayPos) {
                        return it[mPlayPos]
                    }
                }
            }
            return null
        }

    val currentTrackPos: Int
        get() {
            synchronized(this) {
                mPlayList?.let {
                    if (mPlayPos >= 0 && it.size > mPlayPos) {
                        return mPlayPos
                    }
                }
            }
            return -1
        }

    fun seek(position: Long): Long {
        var positiontemp = position
        mPlayer?.let {
            if (it.isInitialized) {
                if (positiontemp < 0) {
                    positiontemp = 0
                } else if (positiontemp > it.duration()) {
                    positiontemp = it.duration()
                }
                val result = it.seek(positiontemp)
                notifyChange(POSITION_CHANGED)
                return result
            }
        }
        return -1
    }

    fun position(): Long {
        return if (mPlayer?.isInitialized == true) {
            mPlayer?.position() ?: -1
        } else -1
    }

    fun duration(): Long {
        return if (mPlayer?.isInitialized == true) {
            mPlayer?.duration() ?: -1
        } else -1
    }

    val queue: List<Audio>
        get() {
            synchronized(this) {
                val len = Utils.safeCountOf(mPlayList)
                val list: MutableList<Audio> = ArrayList(len)
                for (i in 0 until len) {
                    mPlayList?.let {
                        list.add(i, it[i])
                    }
                }
                return list
            }
        }

    private val currentTrackNotSyncPos: Int
        get() {
            mPlayList?.let {
                if (mPlayPos >= 0 && it.size > mPlayPos) {
                    return mPlayPos
                }
            }
            return -1
        }

    fun canPlayAfterCurrent(audio: Audio): Boolean {
        synchronized(this) {
            val current = currentTrackNotSyncPos
            if (mPlayList.isNullOrEmpty() || current == -1 || mPlayList?.get(current) == audio) {
                return false
            }
            return true
        }
    }

    fun playAfterCurrent(audio: Audio) {
        synchronized(this) {
            val current = currentTrackNotSyncPos
            if (mPlayList.isNullOrEmpty() || current == -1 || mPlayList?.get(current) == audio) {
                return
            }
            mPlayList?.insertAfter(current, audio)
            notifyChange(QUEUE_CHANGED)
        }
    }

    /**
     * Opens a list for playback
     *
     * @param list     The list of tracks to open
     * @param position The position to start playback at
     */
    fun open(list: List<Audio>, position: Int) {
        synchronized(this) {
            val oldAudio = currentTrack
            mPlayList = ArrayList(list)
            if (mShuffleMode == SHUFFLE)
                mPlayList?.shuffle()
            mPlayListOrig = ArrayList(list)
            notifyChange(QUEUE_CHANGED)
            mPlayPos = if (position >= 0) {
                position
            } else {
                0
            }
            playCurrentTrack(true)
            if (oldAudio !== currentTrack) {
                notifyChange(META_CHANGED)
            }
        }
    }

    fun play() {
        mPlayer?.let {
            if (it.isInitialized) {
                val duration = it.duration()
                if (mRepeatMode != REPEAT_CURRENT && duration > 2000 && it.position() >= duration - 2000) {
                    gotoNext(false)
                }
                it.start()
                if (!isPlaying) {
                    isPlaying = true
                    notifyChange(PLAYSTATE_CHANGED)
                }
                cancelShutdown()
                fetchCoverAndUpdateMetadata()
            }
        }
    }

    /**
     * Temporarily pauses playback.
     */
    fun pause() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Pausing playback")
        synchronized(this) {
            if (isPlaying) {
                mPlayer?.pause()
                scheduleDelayedShutdown()
                isPlaying = false
                notifyChange(PLAYSTATE_CHANGED)
            }
        }
    }

    private fun pauseNonSync() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Pausing playback")
        if (isPlaying) {
            mPlayer?.pause()
            scheduleDelayedShutdown()
            isPlaying = false
            notifyChange(PLAYSTATE_CHANGED)
        }
    }

    /**
     * Changes from the current track to the next track
     */
    fun gotoNext(force: Boolean): Boolean {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to next track")
        synchronized(this) {
            if (Utils.safeCountOf(mPlayList) <= 0) {
                if (Constants.IS_DEBUG) Logger.d(TAG, "No play queue")
                scheduleDelayedShutdown()
                return true
            }
            val pos = getNextPosition(force)
            if (Constants.IS_DEBUG) Logger.d(TAG, pos.toString())
            if (pos < 0) {
                seek(0)
                pauseNonSync()
                return false
            }
            mPlayPos = pos
            stop(false)
            mPlayPos = pos
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
        return true
    }

    fun skip(pos: Int, force: Boolean) {
        if (!force && pos == currentTrackPos)
            return
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to next track")
        synchronized(this) {
            if (Utils.safeCountOf(mPlayList) <= 0) {
                if (Constants.IS_DEBUG) Logger.d(TAG, "No play queue")
                scheduleDelayedShutdown()
                return
            }
            if (Constants.IS_DEBUG) Logger.d(TAG, pos.toString())
            if (pos < 0) {
                seek(0)
                pauseNonSync()
                return
            }
            mPlayPos = pos
            stop(false)
            mPlayPos = pos
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    /**
     * Changes from the current track to the previous played track
     */
    fun prev() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to previous track")
        synchronized(this) {
            if (mPlayPos > 0) {
                mPlayPos--
            } else {
                mPlayPos = Utils.safeCountOf(mPlayList) - 1
            }
            stop(false)
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    private fun cycleRepeat() {
        when (mRepeatMode) {
            REPEAT_NONE -> repeatMode = REPEAT_ALL
            REPEAT_ALL -> {
                repeatMode = REPEAT_CURRENT
                if (mShuffleMode != SHUFFLE_NONE) {
                    shuffleMode = SHUFFLE_NONE
                }
            }

            else -> repeatMode = REPEAT_NONE
        }
    }

    private fun cycleShuffle() {
        when (mShuffleMode) {
            SHUFFLE -> shuffleMode = SHUFFLE_NONE
            SHUFFLE_NONE -> {
                shuffleMode = SHUFFLE
                if (mRepeatMode == REPEAT_CURRENT) {
                    repeatMode = REPEAT_ALL
                }
            }
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    fun refresh() {
        notifyChange(REFRESH)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private class MultiPlayer(service: MusicPlaybackService) {
        val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        var mCurrentMediaPlayer: ExoPlayer = ExoPlayer.Builder(
            service, DefaultRenderersFactory(service)
                .setExtensionRendererMode(
                    when (Settings.get().main().fFmpegPlugin) {
                        0 -> EXTENSION_RENDERER_MODE_OFF
                        1 -> EXTENSION_RENDERER_MODE_ON
                        2 -> EXTENSION_RENDERER_MODE_PREFER
                        else -> EXTENSION_RENDERER_MODE_OFF
                    }
                )
        ).build()
        var isInitialized = false
        var isPreparing = false
        val factory = Utils.getExoPlayerFactory(
            Constants.USER_AGENT
        )
        val factoryLocal =
            DefaultDataSource.Factory(service)

        /**
         * @param remoteUrl The path of the file, or the http/rtsp URL of the stream
         * you want to play
         * return True if the `player` has been prepared and is
         * ready to play, false otherwise
         */
        fun setDataSource(remoteUrl: String?) {
            isPreparing = true
            val url = Utils.firstNonEmptyString(
                remoteUrl,
                "file:///android_asset/audio_error.ogg"
            )
            val mediaSource: MediaSource =
                if (url?.contains("file://") == true || url?.contains("content://") == true || url?.contains(
                        RawResourceDataSource.RAW_RESOURCE_SCHEME
                    ) == true
                ) {
                    ProgressiveMediaSource.Factory(factoryLocal)
                        .createMediaSource(makeMediaItem(url))
                } else {
                    ProgressiveMediaSource.Factory(
                        factory
                    ).createMediaSource(makeMediaItem(url))
                }
            mCurrentMediaPlayer.setMediaSource(mediaSource)
            mCurrentMediaPlayer.prepare()
            mCurrentMediaPlayer.setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), true
            )
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mService.get()?.packageName)
            mService.get()?.sendBroadcast(intent)
            mService.get()?.notifyChange(PLAYSTATE_CHANGED)
        }

        fun setDataSource(audio: Audio) {
            setDataSource(audio.url)
        }

        fun start() {
            ExoUtil.startPlayer(mCurrentMediaPlayer)
        }

        fun stop() {
            this.isInitialized = false
            isPreparing = false
            mCurrentMediaPlayer.stop()
            mCurrentMediaPlayer.clearMediaItems()
        }

        fun release() {
            stop()
            mCurrentMediaPlayer.release()
        }

        fun pause() {
            ExoUtil.pausePlayer(mCurrentMediaPlayer)
        }

        fun duration(): Long {
            return mCurrentMediaPlayer.duration
        }

        fun position(): Long {
            return mCurrentMediaPlayer.currentPosition
        }

        fun seek(whereto: Long): Long {
            mCurrentMediaPlayer.seekTo(whereto)
            return whereto
        }

        val audioSessionId: Int
            get() = mCurrentMediaPlayer.audioSessionId

        val bufferPercent: Int
            get() = mCurrentMediaPlayer.bufferedPercentage

        val bufferPos: Long
            get() = mCurrentMediaPlayer.bufferedPosition

        /**
         * Constructor of `MultiPlayer`
         */
        init {
            mCurrentMediaPlayer.setHandleAudioBecomingNoisy(true)
            mCurrentMediaPlayer.setWakeMode(C.WAKE_MODE_NETWORK)

            mCurrentMediaPlayer.repeatMode = Player.REPEAT_MODE_OFF

            mCurrentMediaPlayer.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(@Player.State state: Int) {
                    when (state) {
                        Player.STATE_READY -> if (isPreparing) {
                            isPreparing = false
                            isInitialized = true
                            mService.get()?.notifyChange(PREPARED)
                            mService.get()?.play()
                        }

                        Player.STATE_ENDED -> if (!isPreparing && isInitialized) {
                            isInitialized = mService.get()?.gotoNext(false) == false
                        }

                        else -> {
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    @PlayWhenReadyChangeReason reason: Int
                ) {
                    if (mService.get()?.isPlaying != playWhenReady) {
                        mService.get()?.isPlaying = playWhenReady
                        mService.get()?.notifyChange(PLAYSTATE_CHANGED)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mService.get()?.let {
                        it.errorsCount++
                        if (it.errorsCount > 10) {
                            it.errorsCount = 0
                            it.terminate()
                        } else {
                            val playbackPos = mCurrentMediaPlayer.currentPosition
                            it.playCurrentTrack(false)
                            mCurrentMediaPlayer.seekTo(playbackPos)
                            it.notifyChange(META_CHANGED)
                        }
                    }
                }
            })
        }
    }

    private class ServiceStub(service: MusicPlaybackService) : IAudioPlayerService.Stub() {
        private val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        override fun openFile(audio: Audio) {
            mService.get()?.openFile(audio, true)
        }

        override fun open(list: List<Audio>, position: Int) {
            mService.get()?.open(list, position)
        }

        override fun stop() {
            mService.get()?.pause()
            mService.get()?.releaseServiceUiAndStop()
        }

        override fun pause() {
            mService.get()?.pause()
        }

        override fun play() {
            mService.get()?.play()
        }

        override fun prev() {
            mService.get()?.prev()
        }

        override fun next() {
            mService.get()?.gotoNext(true)
        }

        override fun setShuffleMode(shufflemode: Int) {
            mService.get()?.shuffleMode = shufflemode
        }

        override fun setRepeatMode(repeatmode: Int) {
            mService.get()?.repeatMode = repeatmode
        }

        override fun closeMiniPlayer() {
            mService.get()?.onceCloseMiniPlayer = true
        }

        override fun refresh() {
            mService.get()?.refresh()
        }

        override fun isPlaying(): Boolean {
            return mService.get()?.isPlaying == true
        }

        override fun isPreparing(): Boolean {
            return mService.get()?.isPreparing == true
        }

        override fun isInitialized(): Boolean {
            return mService.get()?.isInitialized == true
        }

        override fun canPlayAfterCurrent(audio: Audio): Boolean {
            return mService.get()?.canPlayAfterCurrent(audio) == true
        }

        override fun playAfterCurrent(audio: Audio) {
            mService.get()?.playAfterCurrent(audio)
        }

        override fun getQueue(): List<Audio>? {
            return mService.get()?.queue
        }

        override fun duration(): Long {
            return mService.get()?.duration() ?: -1
        }

        override fun position(): Long {
            return mService.get()?.position() ?: -1
        }

        override fun getMiniplayerVisibility(): Boolean {
            return mService.get()?.onceCloseMiniPlayer != true && mService.get()?.currentTrack != null
        }

        override fun seek(position: Long): Long {
            return mService.get()?.seek(position) ?: -1
        }

        override fun skip(position: Int) {
            mService.get()?.skip(position, false)
        }

        override fun getCurrentAudio(): Audio? {
            return mService.get()?.currentTrack
        }

        override fun getCurrentAudioPos(): Int {
            return mService.get()?.currentTrackPos ?: -1
        }

        override fun getArtistName(): String? {
            return mService.get()?.artistName
        }

        override fun getTrackName(): String? {
            return mService.get()?.trackName
        }

        override fun getAlbumName(): String? {
            return mService.get()?.albumName
        }

        override fun getAlbumCover(): String? {
            return mService.get()?.albumCover
        }

        override fun getPath(): String? {
            return mService.get()?.path
        }

        override fun getQueuePosition(): Int {
            return mService.get()?.queuePosition ?: -1
        }

        override fun getShuffleMode(): Int {
            return mService.get()?.shuffleMode ?: SHUFFLE_NONE
        }

        override fun getRepeatMode(): Int {
            return mService.get()?.repeatMode ?: REPEAT_NONE
        }

        override fun getAudioSessionId(): Int {
            return mService.get()?.audioSessionId ?: -1
        }

        override fun getBufferPercent(): Int {
            return mService.get()?.bufferPercent ?: 0
        }

        override fun getBufferPosition(): Long {
            return mService.get()?.bufferPos ?: 0
        }

        override fun doNotDestroyWhenActivityRecreated() {
            mService.get()?.doNotDestroyWhenActivityRecreated()
        }
    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        const val PLAYSTATE_CHANGED = "dev.ragnarok.filegallery.player.playstatechanged"
        const val POSITION_CHANGED = "dev.ragnarok.filegallery.player.positionchanged"
        const val META_CHANGED = "dev.ragnarok.filegallery.player.metachanged"
        const val PREPARED = "dev.ragnarok.filegallery.player.prepared"
        const val REPEATMODE_CHANGED = "dev.ragnarok.filegallery.player.repeatmodechanged"
        const val SHUFFLEMODE_CHANGED = "dev.ragnarok.filegallery.player.shufflemodechanged"
        const val QUEUE_CHANGED = "dev.ragnarok.filegallery.player.queuechanged"
        const val SERVICECMD = "dev.ragnarok.filegallery.player.musicservicecommand"
        const val TOGGLEPAUSE_ACTION = "dev.ragnarok.filegallery.player.togglepause"
        const val PAUSE_ACTION = "dev.ragnarok.filegallery.player.pause"
        const val STOP_ACTION = "dev.ragnarok.filegallery.player.stop"
        const val SWIPE_DISMISS_ACTION = "dev.ragnarok.filegallery.player.swipe_dismiss"
        const val PREVIOUS_ACTION = "dev.ragnarok.filegallery.player.previous"
        const val NEXT_ACTION = "dev.ragnarok.filegallery.player.next"
        const val REPEAT_ACTION = "dev.ragnarok.filegallery.player.repeat"
        const val SHUFFLE_ACTION = "dev.ragnarok.filegallery.player.shuffle"

        const val REFRESH = "dev.ragnarok.filegallery.player.refresh"

        /**
         * Called to update the remote control client
         */
        const val CMDNAME = "command"
        const val CMDTOGGLEPAUSE = "togglepause"
        const val CMDSTOP = "stop"
        const val CMDPAUSE = "pause"
        const val CMDPLAY = "play"
        const val CMDPREVIOUS = "previous"
        const val CMDNEXT = "next"
        const val CMDPLAYLIST = "playlist"
        const val SHUFFLE_NONE = 0
        const val SHUFFLE = 1
        const val REPEAT_NONE = 0
        const val REPEAT_CURRENT = 1
        const val REPEAT_ALL = 2
        private var IDLE_DELAY = Constants.AUDIO_PLAYER_SERVICE_IDLE
        private const val MAX_QUEUE_SIZE = 200

        fun startForPlayList(
            context: Context,
            audios: ArrayList<Audio>,
            position: Int,
            forceShuffle: Boolean
        ) {
            if (audios.isEmpty()) {
                return
            }
            Logger.d(TAG, "startForPlayList, count: " + audios.size + ", position: " + position)
            val target: ArrayList<Audio>
            var targetPosition: Int
            if (audios.size <= MAX_QUEUE_SIZE) {
                target = audios
                targetPosition = position
            } else {
                target = ArrayList(MAX_QUEUE_SIZE)
                val half = MAX_QUEUE_SIZE / 2
                var startAt = position - half
                if (startAt < 0) {
                    startAt = 0
                }
                targetPosition = position - startAt
                var i = startAt
                while (target.size < MAX_QUEUE_SIZE) {
                    if (i > audios.size - 1) {
                        break
                    }
                    target.add(audios[i])
                    i++
                }
                if (target.size < MAX_QUEUE_SIZE) {
                    var it = startAt - 1
                    while (target.size < MAX_QUEUE_SIZE) {
                        target.add(0, audios[it])
                        targetPosition++
                        it--
                    }
                }
            }
            val intent = Intent(context, MusicPlaybackService::class.java)
            intent.action = CMDPLAYLIST
            intent.putParcelableArrayListExtra(Extra.AUDIOS, target)
            intent.putExtra(Extra.POSITION, targetPosition)
            intent.putExtra(Extra.SHUFFLE_MODE, if (forceShuffle) SHUFFLE else SHUFFLE_NONE)
            context.startService(intent)
        }
    }
}
