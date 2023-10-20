package me.minetsh.imaging.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.google.android.material.textview.MaterialTextView
import me.minetsh.imaging.IMGTextEditDialog
import me.minetsh.imaging.core.IMGText

/**
 * Created by felix on 2017/11/14 下午7:27.
 */
class IMGStickerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : IMGStickerView(context, attrs, defStyleAttr), IMGTextEditDialog.Callback {
    private var mTextView: TextView? = null
    private var mText: IMGText? = null
    private var mDialog: IMGTextEditDialog? = null

    init {
        if (mBaseTextSize <= 0) {
            mBaseTextSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                TEXT_SIZE_SP, context.resources.displayMetrics
            )
        }

        doInitialize(context)
    }

    override fun onCreateContentView(context: Context): View? {
        mTextView = MaterialTextView(context)
        mTextView?.textSize = mBaseTextSize
        mTextView?.setPadding(PADDING, PADDING, PADDING, PADDING)
        mTextView?.setTextColor(Color.WHITE)
        return mTextView
    }

    var text: IMGText?
        get() = mText
        set(text) {
            mText = text
            mText?.let { mTextS ->
                mTextView?.let { mTextViewS ->
                    mTextViewS.text = mTextS.text
                    mTextViewS.setTextColor(mTextS.color)
                }
            }
        }

    override fun onContentTap() {
        val dialog = dialog
        dialog.setText(mText)
        dialog.show()
    }

    private val dialog: IMGTextEditDialog
        get() {
            if (mDialog == null) {
                mDialog = IMGTextEditDialog(context, this)
            }
            return mDialog!!
        }

    override fun onText(text: IMGText?) {
        mText = text

        mText?.let { mTextS ->
            mTextView?.let { mTextViewS ->
                mTextViewS.text = mTextS.text
                mTextViewS.setTextColor(mTextS.color)
            }
        }
    }

    companion object {
        private const val TAG = "IMGStickerTextView"
        private const val PADDING = 26
        private const val TEXT_SIZE_SP = 24f
        private var mBaseTextSize = -1f
    }
}