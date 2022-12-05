package dev.ragnarok.filegallery.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import dev.ragnarok.filegallery.R

class KeyboardView : FrameLayout {
    private var mOnKeyboardClickListener: OnKeyboardClickListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        @LayoutRes val layoutRes: Int
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.KeyboardView, 0, 0)
        try {
            var theme = a.getString(R.styleable.KeyboardView_keyboard_theme)
            if (theme.isNullOrEmpty()) {
                theme = "fullscreen"
            }
            layoutRes =
                if ("dialog" == theme) R.layout.dialog_pin_keyboard else R.layout.fragment_pin_keyboard
        } finally {
            a.recycle()
        }
        LayoutInflater.from(context).inflate(layoutRes, this, true)
        val digitsButtons = arrayOfNulls<View>(10)
        digitsButtons[0] = findViewById(R.id.button0)
        digitsButtons[1] = findViewById(R.id.button1)
        digitsButtons[2] = findViewById(R.id.button2)
        digitsButtons[3] = findViewById(R.id.button3)
        digitsButtons[4] = findViewById(R.id.button4)
        digitsButtons[5] = findViewById(R.id.button5)
        digitsButtons[6] = findViewById(R.id.button6)
        digitsButtons[7] = findViewById(R.id.button7)
        digitsButtons[8] = findViewById(R.id.button8)
        digitsButtons[9] = findViewById(R.id.button9)
        for (i in digitsButtons.indices) {
            digitsButtons[i]?.setOnClickListener {
                onDigitButtonClick(
                    i
                )
            }
        }
        findViewById<View>(R.id.buttonBackspace).setOnClickListener {
            mOnKeyboardClickListener?.onBackspaceClick()
        }
        findViewById<View>(R.id.buttonFingerprint).setOnClickListener {
            mOnKeyboardClickListener?.onFingerPrintClick()
        }
    }

    private fun onDigitButtonClick(num: Int) {
        mOnKeyboardClickListener?.onButtonClick(num)
    }

    fun setOnKeyboardClickListener(onKeyboardClickListener: OnKeyboardClickListener?) {
        mOnKeyboardClickListener = onKeyboardClickListener
    }

    interface OnKeyboardClickListener {
        fun onButtonClick(number: Int)
        fun onBackspaceClick()
        fun onFingerPrintClick()
    }
}