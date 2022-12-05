package dev.ragnarok.fenrir.view.emoji

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

class BotKeyboardView : ScrollView {
    private val buttonViews = ArrayList<View>()
    private val isFullSize = if (isInEditMode) false else Settings.get().ui().isEmojis_full_screen
    private var container: LinearLayout? = null
    private var botButtons: List<List<Keyboard.Button>>? = null
    private var delegate: BotKeyboardViewDelegate? = null
    private var panelHeight = 0
    private var buttonHeight = 0

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private val onGlobalLayoutListener = OnGlobalLayoutListener {
        val r = Rect()
        getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.height
        var heightDifference = screenHeight - (r.bottom - r.top)
        val navBarHeight =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navBarHeight > 0) {
            heightDifference -= context.resources.getDimensionPixelSize(navBarHeight)
        }
        val statusbarHeight =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (statusbarHeight > 0) {
            heightDifference -= context.resources.getDimensionPixelSize(statusbarHeight)
        }
        if (heightDifference > 200) {
            setPanelHeight(heightDifference)
        }
    }
    private var needKeyboardListen = false
    private var needTrackKeyboard = true

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeAttributes(context, attrs)
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initializeAttributes(context, attrs)
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initializeAttributes(context, attrs)
        init(context)
    }

    private fun init(context: Context) {
        container = LinearLayout(context)
        container?.orientation = LinearLayout.VERTICAL
        addView(container)
        if (needTrackKeyboard) {
            listenKeyboardSize()
        }
    }

    private fun initializeAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.BotKeyboardView)
            needTrackKeyboard =
                array.getBoolean(R.styleable.BotKeyboardView_track_keyboard_height, true)
            array.recycle()
        }
    }

    fun setDelegate(botKeyboardViewDelegate: BotKeyboardViewDelegate?) {
        delegate = botKeyboardViewDelegate
    }

    fun invalidateViews() {
        for (a in buttonViews.indices) {
            buttonViews[a].invalidate()
        }
    }

    fun setButtons(buttons: List<List<Keyboard.Button>>?, needClose: Boolean): Boolean {
        if (botButtons == buttons) {
            return false
        }
        botButtons = buttons
        container?.removeAllViews()
        buttonViews.clear()
        scrollTo(0, 0)
        if (buttons != null && buttons.isNotEmpty()) {
            buttonHeight = if (!isFullSize) 42 else 42f.coerceAtLeast(
                (panelHeight - Utils.dp(30f) - ((botButtons?.size
                    ?: 0) - 1) * Utils.dp(10f)).toFloat() / (botButtons?.size
                    ?: 1) / Utils.density
            )
                .toInt()
            for (a in buttons.indices) {
                val row = buttons[a]
                val layout = LinearLayout(context)
                layout.orientation = LinearLayout.HORIZONTAL
                container?.addView(
                    layout,
                    createLinear(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        buttonHeight,
                        15f,
                        if (a == 0) 15f else 10.toFloat(),
                        15f,
                        if (a == buttons.size - 1) 15f else 0.toFloat()
                    )
                )
                val weight = 1.0f / row.size
                for (b in row.indices) {
                    val button = row[b]
                    val holder = ButtonHolder(
                        LayoutInflater.from(
                            context
                        ).inflate(R.layout.item_keyboard_button, layout, false)
                    )
                    holder.button.tag = button
                    holder.button.text = button.label
                    holder.button.setTextColor(Color.parseColor("#ffffff"))
                    when (button.color) {
                        "default", "secondary" -> {
                            holder.button.setTextColor(Color.parseColor("#000000"))
                            holder.button.setBackgroundColor(Color.parseColor("#eeeeee"))
                        }
                        "negative" -> holder.button.setBackgroundColor(Color.parseColor("#E64646"))
                        "positive" -> holder.button.setBackgroundColor(Color.parseColor("#4BB34B"))
                        else -> holder.button.setBackgroundColor(Color.parseColor("#5181B8"))
                    }
                    layout.addView(
                        holder.itemView,
                        createLinear(
                            0,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            weight,
                            0,
                            0,
                            if (b != row.size - 1) 10 else 0,
                            0
                        )
                    )
                    holder.button.setOnClickListener { v: View ->
                        delegate?.didPressedButton(
                            v.tag as Keyboard.Button,
                            needClose
                        )
                    }
                    buttonViews.add(holder.itemView)
                }
            }
        }
        return true
    }

    private fun setPanelHeight(height: Int) {
        panelHeight = height
        if (isFullSize && botButtons != null && botButtons?.isNotEmpty() == true) {
            buttonHeight = 42f.coerceAtLeast(
                (panelHeight - Utils.dp(30f) - ((botButtons?.size ?: 0) - 1) * Utils.dp(
                    10f
                )).toFloat() / (botButtons?.size ?: 1) / Utils.density
            ).toInt()
            val count = container?.childCount
            val newHeight = Utils.dp(buttonHeight.toFloat())
            count?.let {
                for (a in 0 until it) {
                    val v = container?.getChildAt(a)
                    val layoutParams = v?.layoutParams as LinearLayout.LayoutParams?
                    if (layoutParams?.height != newHeight) {
                        layoutParams?.height = newHeight
                        v?.layoutParams = layoutParams
                    }
                }
            }
        }
    }

    val keyboardHeight: Int
        get() {
            if (botButtons == null) {
                return 0
            }
            return if (isFullSize) panelHeight else (botButtons?.size
                ?: 0) * Utils.dp(buttonHeight.toFloat()) + Utils.dp(
                30f
            ) + ((botButtons?.size ?: 0) - 1) * Utils.dp(10f)
        }

    private fun listenKeyboardSize() {
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        needKeyboardListen = true
    }

    fun destroy() {
        if (needKeyboardListen) {
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }
        container?.removeAllViews()
        buttonViews.clear()
    }

    interface BotKeyboardViewDelegate {
        fun didPressedButton(button: Keyboard.Button, needClose: Boolean)
    }

    private class ButtonHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val button: MaterialButton = itemView.findViewById(R.id.keyboard_button)
    }

    companion object {
        private fun getSize(size: Float): Int {
            return (if (size < 0) size.toInt() else Utils.dp(size))
        }

        internal fun createLinear(
            width: Int,
            height: Int,
            leftMargin: Float,
            topMargin: Float,
            rightMargin: Float,
            bottomMargin: Float
        ): LinearLayout.LayoutParams {
            val layoutParams =
                LinearLayout.LayoutParams(getSize(width.toFloat()), getSize(height.toFloat()))
            layoutParams.setMargins(
                Utils.dp(leftMargin),
                Utils.dp(topMargin),
                Utils.dp(rightMargin),
                Utils.dp(bottomMargin)
            )
            return layoutParams
        }

        internal fun createLinear(
            width: Int,
            height: Int,
            weight: Float,
            leftMargin: Int,
            topMargin: Int,
            rightMargin: Int,
            bottomMargin: Int
        ): LinearLayout.LayoutParams {
            val layoutParams = LinearLayout.LayoutParams(
                getSize(width.toFloat()),
                getSize(height.toFloat()),
                weight
            )
            layoutParams.setMargins(
                Utils.dp(leftMargin.toFloat()),
                Utils.dp(topMargin.toFloat()),
                Utils.dp(rightMargin.toFloat()),
                Utils.dp(bottomMargin.toFloat())
            )
            return layoutParams
        }
    }
}