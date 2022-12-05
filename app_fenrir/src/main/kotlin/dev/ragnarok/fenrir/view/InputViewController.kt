package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView.BotKeyboardViewDelegate
import dev.ragnarok.fenrir.view.emoji.EmojiconEditText
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.*
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.Companion.input
import dev.ragnarok.fenrir.view.emoji.section.Emojicon

class InputViewController(
    activity: Activity,
    rootView: View,
    private val callback: OnInputActionCallback
) {
    private val mActivity: Context = activity.applicationContext
    private val mInputField: EmojiconEditText = rootView.findViewById(R.id.fragment_input_text)
    private val rlEmojiContainer: LinearLayout
    private val ibEmoji: ImageView
    private val ibAttach: ImageView
    private val vgInputViewHolder: ViewGroup
    private val vgMessageInput: ViewGroup
    private val vgVoiceInput: ViewGroup
    private val tvAttCount: TextView
    private val mButtonSend: ImageView
    private val mRecordResumePause: ImageView
    private val mTextWatcher: TextWatcherAdapter
    private val mIconColorActive: Int
    private val mIconColorInactive: Int
    private val mRecordingDuration: TextView = rootView.findViewById(R.id.recording_duration)
    private val currentKeyboardShow: ImageView
    private var botKeyboard: BotKeyboardView?
    private var emojiPopup: EmojiconsPopup?
    private var emojiOnScreen = false
    private var emojiNeed = false
    private var keyboardOnScreen = false
    private var sendOnEnter = false
    private var mCurrentMode = Mode.NORMAL
    private var canEditingSave = false
    private var canNormalSend = false
    private var canStartRecording = false
    private var mRecordActionsCallback: RecordActionsCallback? = null
    fun setKeyboardBotClickListener(listener: BotKeyboardViewDelegate?) {
        botKeyboard?.setDelegate(listener)
    }

    fun setKeyboardBotLongClickListener(listener: View.OnLongClickListener?) {
        currentKeyboardShow.setOnLongClickListener(listener)
    }

    fun closeBotKeyboard() {
        if (!keyboardOnScreen) {
            return
        }
        keyboardOnScreen = false
        botKeyboard?.visibility = View.GONE
        currentKeyboardShow.clearColorFilter()
    }

    fun updateBotKeyboard(currentKeyboard: Keyboard, show: Boolean): Boolean {
        val ret: Boolean
        if (currentKeyboard.buttons.isNullOrEmpty()) {
            botKeyboard?.visibility = View.GONE
            ret = botKeyboard?.setButtons(null, false) == true
            keyboardOnScreen = false
            currentKeyboardShow.visibility = View.GONE
        } else {
            botKeyboard?.visibility = if (show) if (Settings.get()
                    .main().isShow_bot_keyboard
            ) View.VISIBLE else View.GONE else View.GONE
            ret = botKeyboard?.setButtons(
                currentKeyboard.buttons,
                currentKeyboard.one_time && !currentKeyboard.inline
            ) == true
            keyboardOnScreen = show
            currentKeyboardShow.visibility = if (Settings.get()
                    .main().isShow_bot_keyboard
            ) View.VISIBLE else View.GONE
            if (show) {
                Utils.setColorFilter(
                    currentKeyboardShow,
                    CurrentTheme.getColorPrimary(currentKeyboardShow.context)
                )
            } else {
                currentKeyboardShow.clearColorFilter()
            }
        }
        return ret
    }

    fun storeEmoji() {
        emojiPopup?.storeState()
    }

    fun destroyView() {
        emojiPopup?.destroy()
        emojiPopup = null
        botKeyboard?.destroy()
        botKeyboard = null
    }

    private fun onResumePauseButtonClick() {
        mRecordActionsCallback?.onResumePauseClick()
    }

    private fun cancelVoiceMessageRecording() {
        mRecordActionsCallback?.onRecordCancel()
    }

    private fun onEmojiButtonClick() {
        if (emojiPopup?.isKeyBoardOpen == true) {
            val imm =
                mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(mInputField.windowToken, 0)
            emojiNeed = true
            closeBotKeyboard()
            //ibEmoji.setImageResource(R.drawable.keyboard_arrow_down);
        } else {
            if (!emojiOnScreen) {
                closeBotKeyboard()
            }
            showEmoji(!emojiOnScreen)
            //ibEmoji.setImageResource(R.drawable.emoticon);
        }
    }

    fun setSendOnEnter(sendOnEnter: Boolean) {
        this.sendOnEnter = sendOnEnter
        if (sendOnEnter) {
            mInputField.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            mInputField.setSingleLine()
        }
    }

    fun showEmoji(visible: Boolean) {
        if (emojiOnScreen == visible) {
            return
        }
        if (visible && rlEmojiContainer.childCount == 0) {
            emojiPopup?.getEmojiView(rlEmojiContainer).let {
                rlEmojiContainer.addView(it)
            }
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
                closeBotKeyboard()
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
                    mInputField,
                    emojicon
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
                    mInputField.dispatchKeyEvent(event)
                }

            }
    }

    private val text: String
        get() = mInputField.text.toString()
    private val trimmedText: String
        get() = text.trim { it <= ' ' }

    fun setTextQuietly(text: String?) {
        mInputField.removeTextChangedListener(mTextWatcher)
        mInputField.setText(text)
        mInputField.requestFocus()
        if (text.nonNullNoEmpty()) mInputField.setSelection(text.length)
        mInputField.addTextChangedListener(mTextWatcher)
    }

    @SuppressLint("SetTextI18n")
    fun appendTextQuietly(text: String?) {
        if (text != null) {
            mInputField.removeTextChangedListener(mTextWatcher)
            var txt = Utils.firstNonEmptyString(mInputField.text.toString(), " ")
            if ((txt ?: return).lastIndexOf('@') != -1) {
                txt = txt.substring(0, txt.length - 1)
            }
            mInputField.setText("$txt $text")
            mInputField.requestFocus()
            if (text.nonNullNoEmpty()) mInputField.setSelection(mInputField.text?.length ?: 0)
            callback.onInputTextChanged("$txt $text")
            mInputField.addTextChangedListener(mTextWatcher)
        }
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
        if (mCurrentMode == Mode.VOICE_RECORD) {
            cancelVoiceMessageRecording()
            return false
        }
        if (emojiOnScreen) {
            showEmoji(false)
            return false
        }
        return true
    }

    fun setOnSickerClickListener(sickerClickListener: OnStickerClickedListener?) {
        emojiPopup?.onStickerClickedListener = sickerClickListener
    }

    fun setOnMySickerClickListener(sickerMyClickListener: OnMyStickerClickedListener?) {
        emojiPopup?.setMyOnStickerClickedListener(sickerMyClickListener)
    }

    private fun switchModeTo(mode: Int) {
        if (mCurrentMode != mode) {
            mCurrentMode = mode
            resolveModeViews()
        }
    }

    private fun onSendButtonClick() {
        when (mCurrentMode) {
            Mode.NORMAL -> if (canNormalSend) {
                callback.onSendClicked(trimmedText)
            } else if (canStartRecording) {
                mRecordActionsCallback?.onSwithToRecordMode()
            }
            Mode.VOICE_RECORD -> {
                mRecordActionsCallback?.onRecordSendClick()
            }
            Mode.EDITING -> callback.onSaveClick()
        }
    }

    private fun resolveModeViews() {
        when (mCurrentMode) {
            Mode.NORMAL -> {
                //                vgInputViewHolder.setVisibility(View.VISIBLE);
                vgVoiceInput.visibility = View.GONE
                vgMessageInput.visibility = View.VISIBLE
            }
            Mode.VOICE_RECORD -> {
                //                vgInputViewHolder.setVisibility(View.VISIBLE);
                vgVoiceInput.visibility = View.VISIBLE
                vgMessageInput.visibility = View.GONE
            }
            Mode.DISABLED -> vgInputViewHolder.visibility = View.GONE
        }
    }

    private fun resolveSendButton() {
        when (mCurrentMode) {
            Mode.VOICE_RECORD -> {
                mButtonSend.setImageResource(R.drawable.check)
                setupPrimaryButton(true)
            }
            Mode.NORMAL -> {
                mButtonSend.setImageResource(if (!canNormalSend && canStartRecording) R.drawable.voice else R.drawable.send)
                setupPrimaryButton(canNormalSend || canStartRecording)
            }
            Mode.EDITING -> {
                mButtonSend.setImageResource(R.drawable.check)
                setupPrimaryButton(canEditingSave)
            }
        }
    }

    private fun setupPrimaryButton(active: Boolean) {
        mButtonSend.drawable.setTint(if (active) mIconColorActive else mIconColorInactive)
    }

    fun setupRecordPauseButton(visible: Boolean, isRecording: Boolean) {
        mRecordResumePause.visibility =
            if (visible) View.VISIBLE else View.INVISIBLE
        mRecordResumePause.setImageResource(if (visible) if (isRecording) R.drawable.pause else R.drawable.play else R.drawable.pause_disabled)
    }

    fun switchModeToEditing(canSave: Boolean) {
        switchModeTo(Mode.EDITING)
        canEditingSave = canSave
        resolveSendButton()
    }

    fun switchModeToNormal(canSend: Boolean, canStartRecoring: Boolean) {
        switchModeTo(Mode.NORMAL)
        canNormalSend = canSend
        canStartRecording = canStartRecoring
        resolveSendButton()
    }

    fun switchModeToRecording() {
        switchModeTo(Mode.VOICE_RECORD)
        resolveSendButton()
    }

    fun switchModeToDisabled() {
        switchModeTo(Mode.DISABLED)
    }

    fun setRecordActionsCallback(recordActionsCallback: RecordActionsCallback?) {
        mRecordActionsCallback = recordActionsCallback
    }

    fun setRecordingDuration(time: Long) {
        val str = AppTextUtils.getDurationString((time / 1000).toInt())
        mRecordingDuration.text = mActivity.getString(R.string.recording_time, str)
    }

    interface OnInputActionCallback {
        fun onInputTextChanged(s: String?)
        fun onSendClicked(body: String?)
        fun onAttachClick()
        fun onSaveClick()
    }

    interface RecordActionsCallback {
        fun onRecordCancel()
        fun onSwithToRecordMode()
        fun onRecordSendClick()
        fun onRecordCustomClick()
        fun onResumePauseClick()
    }

    object Mode {
        const val NORMAL = 1
        const val VOICE_RECORD = 2
        const val EDITING = 3
        const val DISABLED = 4
    }

    init {
        mTextWatcher = object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                callback.onInputTextChanged(s.toString())
            }
        }
        vgInputViewHolder = rootView.findViewById(R.id.fragment_input_container)
        vgMessageInput = rootView.findViewById(R.id.message_input_container)
        vgVoiceInput = rootView.findViewById(R.id.voice_input_container)
        mIconColorActive = CurrentTheme.getColorPrimary(activity)
        mIconColorInactive = CurrentTheme.getColorOnSurface(activity)
        mButtonSend = rootView.findViewById(R.id.buttonSend)
        mButtonSend.setOnClickListener { onSendButtonClick() }
        mButtonSend.setOnLongClickListener {
            if (canStartRecording && mCurrentMode == Mode.NORMAL && mRecordActionsCallback != null) {
                mRecordActionsCallback?.onRecordCustomClick()
                return@setOnLongClickListener true
            }
            false
        }
        tvAttCount = rootView.findViewById(R.id.fragment_input_att_count)
        rlEmojiContainer = rootView.findViewById(R.id.fragment_input_emoji_container)
        ibAttach = rootView.findViewById(R.id.buttonAttach)
        ibEmoji = rootView.findViewById(R.id.buttonEmoji)
        ibAttach.setOnClickListener { callback.onAttachClick() }
        ibEmoji.setOnClickListener { onEmojiButtonClick() }
        mInputField.addTextChangedListener(mTextWatcher)
        mInputField.setOnClickListener { showEmoji(false) }
        mInputField.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (sendOnEnter && i == EditorInfo.IME_ACTION_SEND) {
                callback.onSendClicked(trimmedText)
                return@setOnEditorActionListener true
            }
            false
        }
        emojiPopup = EmojiconsPopup(rootView, activity)
        setupEmojiView()
        rootView.findViewById<View>(R.id.cancel_voice_message)
            .setOnClickListener { cancelVoiceMessageRecording() }
        mRecordResumePause = rootView.findViewById(R.id.pause_voice_message)
        mRecordResumePause.setOnClickListener { onResumePauseButtonClick() }
        botKeyboard = rootView.findViewById(R.id.fragment_input_keyboard_container)
        currentKeyboardShow = rootView.findViewById(R.id.buttonBotKeyboard)
        currentKeyboardShow.setOnClickListener {
            if (keyboardOnScreen) {
                closeBotKeyboard()
            } else {
                keyboardOnScreen = true
                botKeyboard?.visibility = View.VISIBLE
                Utils.setColorFilter(
                    currentKeyboardShow,
                    CurrentTheme.getColorPrimary(currentKeyboardShow.context)
                )
                if (emojiOnScreen) {
                    showEmoji(false)
                    ibEmoji.setImageResource(if (emojiOnScreen) R.drawable.keyboard_arrow_down else R.drawable.emoticon)
                }
                if (emojiPopup?.isKeyBoardOpen == true) {
                    val imm =
                        mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm?.hideSoftInputFromWindow(mInputField.windowToken, 0)
                }
            }
        }
        resolveModeViews()
    }
}