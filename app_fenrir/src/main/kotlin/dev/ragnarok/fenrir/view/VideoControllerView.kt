package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.media.MaterialPlayPauseFab
import dev.ragnarok.fenrir.view.media.MediaActionDrawable
import java.lang.ref.WeakReference

class VideoControllerView : FrameLayout, CustomSeekBar.CustomSeekBarListener {
    private val mContext: Context
    private val mUseFastForward: Boolean
    private val mHandler: Handler = MessageHandler(this)
    private var mPlayer: MediaPlayerControl? = null
    private var mAnchor: ViewGroup? = null
    private var mRoot: View? = null
    private var mProgress: CustomSeekBar? = null
    private var mEndTime: TextView? = null
    private var mCurrentTime: TextView? = null
    var isShowing = false
        private set
    private var mDragging = false
    private var mFromXml = false
    private var mListenersSet = false
    private var mNextListener: OnClickListener? = null
    private var mPrevListener: OnClickListener? = null
    private var mPauseButton: MaterialPlayPauseFab? = null
    private var mFfwdButton: ImageButton? = null
    private var mRewButton: ImageButton? = null
    private val mPauseListener = OnClickListener {
        doPauseResume()
        show(sDefaultTimeout)
    }
    private val mFullscreenListener = OnClickListener {
        doToggleFullscreen()
        show(sDefaultTimeout)
    }

    private val mRewListener = OnClickListener {
        mPlayer ?: return@OnClickListener
        var pos = mPlayer?.currentPosition
        pos = pos?.minus(5000) // milliseconds
        if (pos != null) {
            mPlayer?.seekTo(pos)
        }
        setProgress()
        show(sDefaultTimeout)
    }
    private val mFfwdListener = OnClickListener {
        if (mPlayer == null) {
            return@OnClickListener
        }
        var pos = mPlayer?.currentPosition
        pos = pos?.plus(15000) // milliseconds
        if (pos != null) {
            mPlayer?.seekTo(pos)
        }
        setProgress()
        show(sDefaultTimeout)
    }
    private var mComment: TextView? = null
    private var mPopup: TextView? = null
    private var mNextButton: ImageButton? = null
    private var mPrevButton: ImageButton? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mRoot = null
        mContext = context
        mUseFastForward = false
        mFromXml = true
        Log.i(TAG, TAG)
    }

    constructor(context: Context, useFastForward: Boolean) : super(context) {
        mContext = context
        mUseFastForward = useFastForward
        Log.i(TAG, TAG)
    }

    constructor(context: Context) : this(context, true) {
        Log.i(TAG, TAG)
    }

    public override fun onFinishInflate() {
        super.onFinishInflate()
        mRoot?.let { initControllerView(it) }
    }

    fun setMediaPlayer(player: MediaPlayerControl?) {
        mPlayer = player
        updatePausePlay()
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    fun setAnchorView(view: ViewGroup?, matchParent: Boolean) {
        mAnchor = view
        val frameParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (matchParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        )
        removeAllViews()
        constructControllerView()
        mRoot?.let { addView(it, frameParams) }
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    private fun constructControllerView() {
        mRoot = inflate(mContext, R.layout.video_media_controller, null)
        mRoot?.let { initControllerView(it) }
    }

    private fun initControllerView(v: View) {
        mPauseButton = v.findViewById(R.id.pause)
        mPauseButton?.requestFocus()
        mPauseButton?.setOnClickListener(mPauseListener)
        val mFullscreenButton = v.findViewById<TextView>(R.id.fullscreen)
        if (mFullscreenButton != null) {
            mFullscreenButton.requestFocus()
            mFullscreenButton.setOnClickListener(mFullscreenListener)
        }
        mFfwdButton = v.findViewById(R.id.ffwd)
        if (mFfwdButton != null) {
            mFfwdButton?.setOnClickListener(mFfwdListener)
            if (!mFromXml) {
                mFfwdButton?.visibility = if (mUseFastForward) VISIBLE else GONE
            }
        }
        mRewButton = v.findViewById(R.id.rew)
        mRewButton?.setOnClickListener(mRewListener)
        if (!mFromXml) {
            mRewButton?.visibility = if (mUseFastForward) VISIBLE else GONE
        }
        mComment = v.findViewById(R.id.comment)
        mComment?.setOnClickListener { mPlayer?.commentClick() }
        mPopup = v.findViewById(R.id.pip_screen)
        mPopup?.setOnClickListener { mPlayer?.toPIPScreen() }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        mNextButton = v.findViewById(R.id.next)
        if (!mFromXml && !mListenersSet) {
            mNextButton?.visibility = GONE
        }
        mPrevButton = v.findViewById(R.id.prev)
        if (!mFromXml && !mListenersSet) {
            mPrevButton?.visibility = GONE
        }
        mProgress = v.findViewById(R.id.mediacontroller_progress)
        mProgress?.setCustomSeekBarListener(this)
        mEndTime = v.findViewById(R.id.time)
        mCurrentTime = v.findViewById(R.id.time_current)
        installPrevNextListeners()
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private fun disableUnsupportedButtons() {
        if (mPlayer == null) {
            return
        }
        try {
            if (mPlayer?.canPause() != true) {
                mPauseButton?.isEnabled = false
            }
            if (mPlayer?.canSeekBackward() != true) {
                mRewButton?.isEnabled = false
            }
            if (mPlayer?.canSeekForward() != true) {
                mFfwdButton?.isEnabled = false
            }
        } catch (ex: IncompatibleClassChangeError) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * the controller until hide() is called.
     */
    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    @JvmOverloads
    fun show(timeout: Int = sDefaultTimeout) {
        if (!isShowing && mAnchor != null) {
            setProgress()
            mPauseButton?.requestFocus()
            disableUnsupportedButtons()
            val tlp = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            mAnchor?.addView(this, tlp)
            isShowing = true
        }
        updatePausePlay()

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS)
        val msg = mHandler.obtainMessage(FADE_OUT)
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT)
            mHandler.sendMessageDelayed(msg, timeout.toLong())
        }
    }

    /**
     * Remove the controller from the screen.
     */
    fun hide() {
        try {
            mAnchor?.removeView(this)
            mHandler.removeMessages(SHOW_PROGRESS)
        } catch (ex: IllegalArgumentException) {
            Log.w("MediaController", "already removed")
        }
        isShowing = false
    }

    private fun stringForTime(timeMs: Long): String {
        if (timeMs < 0) {
            return "--:--"
        }
        if (timeMs == 0L) {
            return "00:00"
        }
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(
                Utils.appLocale,
                "%d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        } else {
            String.format(
                Utils.appLocale,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    internal fun setProgress(): Long {
        if (mPlayer == null || mDragging) {
            return 0
        }
        val position = mPlayer?.currentPosition ?: -1
        val duration = mPlayer?.duration ?: -1
        val buffered = mPlayer?.bufferPosition ?: -1

        mProgress?.updateFullState(duration, position, buffered)
        mEndTime?.text = stringForTime(duration)
        mCurrentTime?.text = stringForTime(position)
        return position
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        show(sDefaultTimeout)
        return true
    }

    override fun onTrackballEvent(ev: MotionEvent): Boolean {
        show(sDefaultTimeout)
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mPlayer == null) {
            return true
        }
        val keyCode = event.keyCode
        val uniqueDown = (event.repeatCount == 0
                && event.action == KeyEvent.ACTION_DOWN)
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                if (uniqueDown) {
                    doPauseResume()
                    show(sDefaultTimeout)
                    mPauseButton?.requestFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (uniqueDown && mPlayer?.isPlaying != true) {
                    mPlayer?.start()
                    updatePausePlay()
                    show(sDefaultTimeout)
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_PAUSE
            -> {
                if (uniqueDown && mPlayer?.isPlaying == true) {
                    mPlayer?.pause()
                    updatePausePlay()
                    show(sDefaultTimeout)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE -> {
                // don't show the controls for volume adjustment
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {
                if (uniqueDown) {
                    hide()
                }
                return true
            }
            else -> {
                show(sDefaultTimeout)
                return super.dispatchKeyEvent(event)
            }
        }
    }

    fun updatePausePlay() {
        if (mPlayer?.isPlaying == true) {
            mPauseButton?.setIcon(MediaActionDrawable.ICON_PAUSE, true)
        } else {
            mPauseButton?.setIcon(MediaActionDrawable.ICON_PLAY, true)
        }
    }

    fun updateComment(can: Boolean) {
        if (mRoot == null || mComment == null || mPlayer == null) {
            return
        }
        mComment?.visibility = if (can) VISIBLE else GONE
    }

    fun updatePip(can: Boolean) {
        if (mRoot == null || mPopup == null || mPlayer == null) {
            return
        }
        mPopup?.visibility = if (can) VISIBLE else GONE
    }

    private fun doPauseResume() {
        if (mPlayer == null) {
            return
        }
        if (mPlayer?.isPlaying == true) {
            mPlayer?.pause()
        } else {
            mPlayer?.start()
        }
        updatePausePlay()
    }

    private fun doToggleFullscreen() {
        mPlayer?.toggleFullScreen()
    }

    override fun setEnabled(enabled: Boolean) {
        mPauseButton?.isEnabled = enabled
        mFfwdButton?.isEnabled = enabled
        mRewButton?.isEnabled = enabled
        mNextButton?.isEnabled = enabled && mNextListener != null
        mPrevButton?.isEnabled = enabled && mPrevListener != null
        mProgress?.isEnabled = enabled
        disableUnsupportedButtons()
        super.setEnabled(enabled)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = VideoControllerView::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = VideoControllerView::class.java.name
    }

    private fun installPrevNextListeners() {
        mNextButton?.setOnClickListener(mNextListener)
        mNextButton?.isEnabled = mNextListener != null


        mPrevButton?.setOnClickListener(mPrevListener)
        mPrevButton?.isEnabled = mPrevListener != null
    }

    fun setPrevNextListeners(next: OnClickListener?, prev: OnClickListener?) {
        mNextListener = next
        mPrevListener = prev
        mListenersSet = true
        if (mRoot != null) {
            installPrevNextListeners()
            if (!mFromXml) {
                mNextButton?.visibility = VISIBLE
            }
            if (!mFromXml) {
                mPrevButton?.visibility = VISIBLE
            }
        }
    }

    interface MediaPlayerControl {
        fun start()
        fun pause()
        val duration: Long
        val currentPosition: Long
        val bufferPosition: Long
        fun seekTo(pos: Long)
        val isPlaying: Boolean
        val bufferPercentage: Int
        fun canPause(): Boolean
        fun canSeekBackward(): Boolean
        fun canSeekForward(): Boolean
        val isFullScreen: Boolean
        fun commentClick()
        fun toggleFullScreen()
        fun toPIPScreen()
    }

    private class MessageHandler(view: VideoControllerView) :
        Handler(Looper.getMainLooper()) {
        private val mView: WeakReference<VideoControllerView> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view?.mPlayer == null) {
                return
            }
            val pos: Long
            when (msg.what) {
                FADE_OUT -> view.hide()
                SHOW_PROGRESS -> {
                    pos = view.setProgress()
                    if (!view.mDragging && view.isShowing && view.mPlayer?.isPlaying == true) {
                        obtainMessage(SHOW_PROGRESS)
                        sendMessageDelayed(obtainMessage(SHOW_PROGRESS), 1000 - pos % 1000)
                    }
                }
            }
        }

    }

    companion object {
        private const val TAG = "VideoControllerView"
        private const val sDefaultTimeout = 0
        private const val FADE_OUT = 1
        private const val SHOW_PROGRESS = 2
    }

    override fun onSeekBarDrag(position: Long) {
        mPlayer ?: return
        mPlayer?.seekTo(position)
        mDragging = false
        setProgress()
        updatePausePlay()
        show(sDefaultTimeout)

        // Ensure that progress is properly updated in the future,
        // the call to show() does not guarantee this because it is a
        // no-op if we are already showing.
        mHandler.sendEmptyMessage(SHOW_PROGRESS)
    }

    override fun onSeekBarMoving(position: Long) {
        mPlayer ?: return
        if (!mDragging) {
            show(3600000)
            mDragging = true
            mHandler.removeMessages(SHOW_PROGRESS)
        }

        mPlayer?.seekTo(position)
        if (mCurrentTime != null) mCurrentTime?.text = stringForTime(position)
    }
}
