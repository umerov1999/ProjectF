package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.rlottie.RLottieDrawable
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

class ProgressButton : FrameLayout {
    private var mButton: MaterialButton? = null
    private var animatedDrawable: RLottieDrawable? = null
    private var mProgressNow = true

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
        val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)
        val layout: Int
        val buttonTitle: String?
        val allCaps: Boolean
        try {
            layout = a.getResourceId(
                R.styleable.ProgressButton_button_layout,
                R.layout.content_progress_button
            )
            buttonTitle = a.getString(R.styleable.ProgressButton_button_text)
            allCaps = a.getBoolean(R.styleable.ProgressButton_button_all_caps, true)
        } finally {
            a.recycle()
        }
        val view = LayoutInflater.from(context).inflate(layout, this, false) as MaterialButton
        view.text = buttonTitle
        view.isAllCaps = allCaps
        addView(view)
        mButton = view
        resolveViews()
    }

    fun setText(charSequence: CharSequence?) {
        mButton?.text = charSequence
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animatedDrawable?.setCurrentParentView(this)
        animatedDrawable?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animatedDrawable?.stop()
        animatedDrawable?.setCurrentParentView(null)
    }

    fun onButtonClick(listener: OnClickListener) {
        mButton?.setOnClickListener(listener)
    }

    fun onLongButtonClick(listener: OnLongClickListener) {
        mButton?.setOnLongClickListener(listener)
    }

    private fun setAnimation(rLottieDrawable: RLottieDrawable) {
        mButton?.icon = null
        animatedDrawable?.let {
            it.stop()
            it.callback = null
            it.recycle()
            animatedDrawable = null
        }
        rLottieDrawable.setAutoRepeat(1)
        rLottieDrawable.setAllowDecodeSingleFrame(true)
        rLottieDrawable.setCurrentParentView(this)
        rLottieDrawable.start()
        animatedDrawable = rLottieDrawable
        mButton?.icon = rLottieDrawable
    }

    private fun resolveViews() {
        if (mProgressNow) {
            if (!FenrirNative.isNativeLoaded) {
                mButton?.setIconResource(R.drawable.ic_progress_button_icon_vector)
            } else {
                setAnimation(
                    RLottieDrawable(
                        dev.ragnarok.fenrir_common.R.raw.loading,
                        Utils.dp(40f),
                        Utils.dp(40f),
                        false,
                        intArrayOf(
                            0x000000,
                            CurrentTheme.getColorPrimary(context),
                            0xffffff,
                            CurrentTheme.getColorSecondary(context)
                        ),
                        false
                    )
                )
            }
        } else {
            mButton?.icon = null
            animatedDrawable?.let {
                it.stop()
                it.callback = null
                it.recycle()
                animatedDrawable = null
            }
        }
    }

    fun changeState(progress: Boolean) {
        mProgressNow = progress
        resolveViews()
    }
}