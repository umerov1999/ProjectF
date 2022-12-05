package dev.ragnarok.fenrir.view

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.*
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.Companion.input
import dev.ragnarok.fenrir.view.emoji.section.Emojicon

class CommentsInputViewController(
    activity: Activity,
    rootView: View,
    private val callback: OnInputActionCallback
) {
    private val mActivity: Context = activity.applicationContext
    val inputField: TextInputEditText = rootView.findViewById(R.id.fragment_input_text)
    private val rlEmojiContainer: LinearLayout
    private val ibEmoji: ImageView
    private val ibAttach: ImageView
    private val tvAttCount: TextView
    private val mButtonSend: ImageView
    private val mTextWatcher: TextWatcherAdapter
    private val mIconColorActive: Int
    private val mIconColorInactive: Int
    private var emojiPopup: EmojiconsPopup?
    private var emojiOnScreen = false
    private var emojiNeed = false
    private var mCanSendNormalMessage = false
    private var sendOnEnter = false
    fun destroyView() {
        emojiPopup?.destroy()
        emojiPopup = null
    }

    private fun onEmojiButtonClick() {
        if (emojiPopup?.isKeyBoardOpen == true) {
            val imm =
                mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(inputField.windowToken, 0)
            emojiNeed = true
            //ibEmoji.setImageResource(R.drawable.keyboard_arrow_down);
        } else {
            showEmoji(!emojiOnScreen)
            //ibEmoji.setImageResource(R.drawable.emoticon);
        }
    }

    fun setSendOnEnter(sendOnEnter: Boolean) {
        this.sendOnEnter = sendOnEnter
        if (sendOnEnter) {
            inputField.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            inputField.setSingleLine()
        }
    }

    internal fun showEmoji(visible: Boolean) {
        if (emojiOnScreen == visible) {
            return
        }
        if (visible && rlEmojiContainer.childCount == 0) {
            val emojiView = emojiPopup?.getEmojiView(rlEmojiContainer)
            emojiView?.let { rlEmojiContainer.addView(it) }
        }
        rlEmojiContainer.visibility = if (visible) View.VISIBLE else View.GONE
        emojiOnScreen = visible
    }

    private fun setupEmojiView() {
        emojiPopup?.onSoftKeyboardOpenCloseListener = object : OnSoftKeyboardOpenCloseListener {
            override fun onKeyboardOpen() {
                if (emojiOnScreen) {
                    showEmoji(false)
                }
                ibEmoji.setImageResource(if (emojiOnScreen) R.drawable.keyboard_arrow_down else R.drawable.emoticon)
            }

            override fun onKeyboardClose() {
                if (emojiNeed) {
                    showEmoji(true)
                    emojiNeed = false
                }
                ibEmoji.setImageResource(if (emojiOnScreen) R.drawable.keyboard_arrow_down else R.drawable.emoticon)
            }
        }
        emojiPopup?.onEmojiconClickedListener = object : OnEmojiconClickedListener {
            override fun onEmojiconClicked(emojicon: Emojicon) {
                input(
                    inputField, emojicon
                )
            }
        }
        emojiPopup?.onEmojiconBackspaceClickedListener =
            object : OnEmojiconBackspaceClickedListener {
                override fun onEmojiconBackspaceClicked(v: View) {
                    val event =
                        KeyEvent(
                            0,
                            0,
                            0,
                            KeyEvent.KEYCODE_DEL,
                            0,
                            0,
                            0,
                            0,
                            KeyEvent.KEYCODE_ENDCALL
                        )
                    inputField.dispatchKeyEvent(event)
                }
            }
    }

    fun setTextQuietly(text: String?) {
        inputField.removeTextChangedListener(mTextWatcher)
        inputField.setText(text)
        inputField.addTextChangedListener(mTextWatcher)
    }

    fun setAttachmentsCount(count: Int) {
        tvAttCount.text = count.toString()
        tvAttCount.visibility = if (count > 0) View.VISIBLE else View.GONE
        tvAttCount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (count > 9) 10f else 12.toFloat())
        val color = if (count > 0) mIconColorActive else mIconColorInactive
        tvAttCount.setTextColor(color)
        ibAttach.drawable.setTint(color)
    }

    fun onBackPressed(): Boolean {
        if (emojiOnScreen) {
            showEmoji(false)
            return false
        }
        return true
    }

    fun setOnSickerClickListener(sickerClickListener: OnStickerClickedListener?) {
        emojiPopup?.onStickerClickedListener = sickerClickListener
    }

    private fun onSendButtonClick() {
        callback.onSendClicked()
    }

    private fun resolveSendButton() {
        mButtonSend.drawable.setTint(if (mCanSendNormalMessage) mIconColorActive else mIconColorInactive)
    }

    fun setCanSendNormalMessage(canSend: Boolean) {
        mCanSendNormalMessage = canSend
        resolveSendButton()
    }

    interface OnInputActionCallback {
        fun onInputTextChanged(s: String?)
        fun onSendClicked()
        fun onSendLongClick(): Boolean
        fun onAttachClick()
    }

    init {
        mTextWatcher = object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                callback.onInputTextChanged(s.toString())
            }
        }
        mIconColorActive = CurrentTheme.getColorPrimary(activity)
        mIconColorInactive = CurrentTheme.getColorOnSurface(activity)
        mButtonSend = rootView.findViewById(R.id.buttonSend)
        mButtonSend.setOnClickListener { onSendButtonClick() }
        mButtonSend.setOnLongClickListener { callback.onSendLongClick() }
        tvAttCount = rootView.findViewById(R.id.fragment_input_att_count)
        rlEmojiContainer = rootView.findViewById(R.id.fragment_input_emoji_container)
        ibAttach = rootView.findViewById(R.id.buttonAttach)
        ibEmoji = rootView.findViewById(R.id.buttonEmoji)
        ibAttach.setOnClickListener { callback.onAttachClick() }
        ibEmoji.setOnClickListener { onEmojiButtonClick() }
        inputField.addTextChangedListener(mTextWatcher)
        inputField.setOnClickListener { showEmoji(false) }
        inputField.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (sendOnEnter && i == EditorInfo.IME_ACTION_SEND) {
                callback.onSendClicked()
                return@setOnEditorActionListener true
            }
            false
        }
        emojiPopup = EmojiconsPopup(rootView, activity)
        setupEmojiView()
    }
}