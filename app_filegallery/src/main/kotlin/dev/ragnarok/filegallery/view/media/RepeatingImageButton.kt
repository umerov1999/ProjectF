package dev.ragnarok.filegallery.view.media

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController

/**
 * A [AppCompatImageButton] that will repeatedly call a 'listener' method as long
 * as the button is pressed, otherwise functions like a typical
 * [AppCompatImageButton]
 */
class RepeatingImageButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(
    context, attrs
), View.OnClickListener {
    private var mStartTime: Long = 0
    private var mRepeatCount = 0
    private var mListener: RepeatListener? = null
    private val mRepeater: Runnable = object : Runnable {
        override fun run() {
            doRepeat(false)
            if (isPressed) {
                postDelayed(this, sInterval)
            }
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.action_button_previous) {
            MusicPlaybackController.previous(context)
        } else if (view.id == R.id.action_button_next) {
            MusicPlaybackController.next()
        }
    }

    fun setRepeatListener(l: RepeatListener?) {
        mListener = l
    }

    override fun performLongClick(): Boolean {
        mStartTime = SystemClock.elapsedRealtime()
        mRepeatCount = 0
        post(mRepeater)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            /* Remove the repeater, but call the hook one more time */
            removeCallbacks(mRepeater)
            if (mStartTime != 0L) {
                doRepeat(true)
                mStartTime = 0
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                /*
                 * Need to call super to make long press work, but return true
                 * so that the application doesn't get the down event
                 */super.onKeyDown(keyCode, event)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                /* Remove the repeater, but call the hook one more time */removeCallbacks(mRepeater)
                if (mStartTime != 0L) {
                    doRepeat(true)
                    mStartTime = 0
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * @param shouldRepeat If True the repeat count stops at -1, false if to add
     * incrementally add the repeat count
     */
    internal fun doRepeat(shouldRepeat: Boolean) {
        val now = SystemClock.elapsedRealtime()
        mListener?.onRepeat(this, now - mStartTime, if (shouldRepeat) -1 else mRepeatCount++)
    }

    fun updateState() {
        if (id == R.id.action_button_next) {
            setImageResource(R.drawable.page_last)
        } else if (id == R.id.action_button_previous) {
            setImageResource(R.drawable.page_first)
        }
    }

    interface RepeatListener {
        /**
         * @param v           View to be set
         * @param duration    Duration of the long press
         * @param repeatcount The number of repeat counts
         */
        fun onRepeat(v: View, duration: Long, repeatcount: Int)
    }

    companion object {
        private const val sInterval: Long = 400
    }

    init {
        isFocusable = true
        isLongClickable = true
        setOnClickListener(this)
        updateState()
    }
}