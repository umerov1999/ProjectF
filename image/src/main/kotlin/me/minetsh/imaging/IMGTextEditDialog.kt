package me.minetsh.imaging

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.textfield.TextInputEditText
import me.minetsh.imaging.core.IMGText
import me.minetsh.imaging.view.IMGColorGroup

/**
 * Created by felix on 2017/12/1 上午11:21.
 */
class IMGTextEditDialog(context: Context, callback: Callback?) : Dialog(
    context, R.style.ImageTextDialog
), RadioGroup.OnCheckedChangeListener {
    private val mCallback: Callback?
    private var mEditText: TextInputEditText? = null
    private var mDefaultText: IMGText? = null
    private var mColorGroup: IMGColorGroup? = null

    init {
        setContentView(R.layout.image_text_dialog)
        mCallback = callback
        val window = window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mColorGroup = findViewById(R.id.cg_colors)
        mColorGroup?.setOnCheckedChangeListener(this)
        mEditText = findViewById(R.id.et_text)
        findViewById<AppCompatImageButton>(R.id.tv_cancel).setOnClickListener {
            dismiss()
        }
        findViewById<AppCompatImageButton>(R.id.tv_done).setOnClickListener {
            onDone()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mDefaultText != null) {
            mEditText?.setText(mDefaultText?.text)
            mEditText?.setTextColor(mDefaultText?.color ?: Color.BLACK)
            if (mDefaultText?.isEmpty == false) {
                mEditText?.setSelection(mEditText?.length() ?: 0)
            }
            mDefaultText = null
        } else mEditText?.setText("")
        mColorGroup?.checkColor = mEditText?.currentTextColor ?: Color.BLACK
    }

    fun setText(text: IMGText?) {
        mDefaultText = text
    }

    fun reset() {
        setText(IMGText(null, Color.WHITE))
    }

    private fun onDone() {
        val text = mEditText?.text.toString()
        if (text.isNotEmpty() && mCallback != null) {
            mCallback.onText(mEditText?.currentTextColor?.let { IMGText(text, it) })
        }
        dismiss()
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        mColorGroup?.checkColor?.let { mEditText?.setTextColor(it) }
    }

    interface Callback {
        fun onText(text: IMGText?)
    }

    companion object {
        private const val TAG = "IMGTextEditDialog"
    }
}
