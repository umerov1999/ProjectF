package dev.ragnarok.fenrir.view

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils

class CircleCounterButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private var mIcon: Drawable? = null
    private var textColor = 0
    private var mActiveIconColor = 0
    private var mNoactiveIconColor = 0
    private var mActiveBackgroundColor = 0
    private var mNoactiveBackgroundColor = 0
    private var mAlwaysCounter = false
    private var mActive = false
    private var mCount = 0
    private var icon: ImageView? = null
    private var counter: TextView? = null
    private var animator: ObjectAnimator? = null
    private fun init(context: Context, attrs: AttributeSet?) {
        inflate(context, R.layout.button_circle_with_counter, this)
        icon = findViewById(R.id.icon)
        counter = findViewById(R.id.counter)
        val attrArray = context.obtainStyledAttributes(attrs, R.styleable.CircleCounterButton)
        try {
            initAttributes(attrArray, counter?.currentTextColor ?: 0)
        } finally {
            attrArray.recycle()
        }
        counter?.setTextColor(textColor)
        initViews()
        gravity = Gravity.CENTER
    }

    private fun initViews() {
        resolveActiveViews()
        resolveCounter()
        icon?.setImageDrawable(mIcon)
    }

    private fun initAttributes(attrArray: TypedArray, deftextcolor: Int) {
        mIcon = attrArray.getDrawable(R.styleable.CircleCounterButton_button_icon)
        textColor = attrArray.getColor(R.styleable.CircleCounterButton_text_color, deftextcolor)
        mCount = attrArray.getInt(R.styleable.CircleCounterButton_count, 0)
        mActive =
            attrArray.getBoolean(R.styleable.CircleCounterButton_active, false)
        mAlwaysCounter = attrArray.getBoolean(
            R.styleable.CircleCounterButton_always_counter,
            false
        )
        mActiveIconColor = attrArray.getColor(
            R.styleable.CircleCounterButton_active_icon_color,
            DEF_ACTIVE_ICON_COLOR
        )
        mNoactiveIconColor = attrArray.getColor(
            R.styleable.CircleCounterButton_noactive_icon_color,
            DEF_NOACTIVE_ICON_COLOR
        )
        mActiveBackgroundColor = attrArray.getColor(
            R.styleable.CircleCounterButton_active_background_color,
            DEF_ACTIVE_BACKGROUND_COLOR
        )
        mNoactiveBackgroundColor = attrArray.getColor(
            R.styleable.CircleCounterButton_noactive_background_color,
            DEF_NOACTIVE_BACKGROUND_COLOR
        )
    }

    private fun resolveCounter() {
        counter?.visibility =
            if (mAlwaysCounter || mCount > 0) VISIBLE else GONE
        counter?.text = mCount.toString()
    }

    private fun resolveActiveViews() {
        if (mActive) {
            Utils.setTint(icon, mActiveIconColor)
            Utils.setBackgroundTint(icon, mActiveBackgroundColor)
        } else {
            Utils.setTint(icon, mNoactiveIconColor)
            Utils.setBackgroundTint(icon, mNoactiveBackgroundColor)
        }
    }

    var isActive: Boolean
        get() = mActive
        set(active) {
            mActive = active
            resolveActiveViews()
        }

    fun setCount(count: Int, animate: Boolean) {
        mCount = count
        counter?.visibility =
            if (mAlwaysCounter || mCount > 0) VISIBLE else GONE
        if (animator != null) {
            animator?.cancel()
        }
        animator = ViewUtils.setCountText(counter, count, animate)
    }

    fun setIcon(drawable: Drawable?) {
        mIcon = drawable
        icon?.setImageDrawable(mIcon)
    }

    fun setIcon(res: Int) {
        mIcon = ContextCompat.getDrawable(context, res)
        icon?.setImageDrawable(mIcon)
    }

    var count: Int
        get() = mCount
        set(count) {
            setCount(count, false)
        }

    companion object {
        private val DEF_ACTIVE_ICON_COLOR = Color.parseColor("#f0f0f0")
        private val DEF_NOACTIVE_ICON_COLOR = Color.parseColor("#b0b0b0")
        private const val DEF_ACTIVE_BACKGROUND_COLOR = Color.RED
        private val DEF_NOACTIVE_BACKGROUND_COLOR = Color.parseColor("#45dcdcdc")
    }

    init {
        init(context, attrs)
    }
}