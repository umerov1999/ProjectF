package de.maxr1998.modernpreferences.preferences.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.ColorWheelRendererBuilder
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.PaintBuilder
import de.maxr1998.modernpreferences.preferences.colorpicker.renderer.ColorWheelRenderer
import de.maxr1998.modernpreferences.preferences.colorpicker.slider.AlphaSlider
import de.maxr1998.modernpreferences.preferences.colorpicker.slider.LightnessSlider
import kotlin.math.cos
import kotlin.math.sin

class ColorPickerView : View {
    private val colorWheelFill = PaintBuilder.newPaint().color(0).build()
    private val alphaPatternPaint = PaintBuilder.newPaint().build()
    private val colorChangedListeners: ArrayList<OnColorChangedListener> = ArrayList()
    private val listeners: ArrayList<OnColorSelectedListener> = ArrayList()
    private var colorWheel: Bitmap? = null
    private var colorWheelCanvas: Canvas? = null
    private var currentColor: Bitmap? = null
    private var currentColorCanvas: Canvas? = null
    private var showBorder = false
    private var density = 8
    private var lightness = 1f
    private var pAlpha = 1f
    var allColors: Array<Int?>? = arrayOf(null, null, null, null, null)
        private set
    private var colorSelection = 0
    private var initialColor: Int? = null
    private var currentColorCircle: ColorCircle? = null
    private var lightnessSlider: LightnessSlider? = null
    private var alphaSlider: AlphaSlider? = null
    private var colorEdit: TextInputEditText? = null
    private var colorPreview: LinearLayout? = null
    private var renderer: ColorWheelRenderer? = null
    private val colorTextChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            try {
                val color = Color.parseColor(s.toString())

                // set the color without changing the edit text preventing stack overflow
                setColor(color, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun afterTextChanged(s: Editable) {}
    }
    private var alphaSliderViewId = 0
    private var lightnessSliderViewId = 0

    constructor(context: Context) : super(context) {
        initWith(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initWith(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initWith(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initWith(context, attrs)
    }

    private fun initWith(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerView)
        density = typedArray.getInt(R.styleable.ColorPickerView_density, 10)
        initialColor = typedArray.getInt(R.styleable.ColorPickerView_initialColor, -0x1)
        val wheelType =
            WHEEL_TYPE.indexOf(typedArray.getInt(R.styleable.ColorPickerView_wheelType, 0))
        val renderer = ColorWheelRendererBuilder.getRenderer(wheelType)
        alphaSliderViewId = typedArray.getResourceId(R.styleable.ColorPickerView_alphaSliderView, 0)
        lightnessSliderViewId =
            typedArray.getResourceId(R.styleable.ColorPickerView_lightnessSliderView, 0)
        setRenderer(renderer)
        setDensity(density)
        setInitialColor(initialColor ?: return, true)
        typedArray.recycle()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        updateColorWheel()
        currentColorCircle = findNearestByColor(initialColor ?: return)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (alphaSliderViewId != 0) setAlphaSlider(rootView.findViewById(alphaSliderViewId))
        if (lightnessSliderViewId != 0) setLightnessSlider(
            rootView.findViewById(
                lightnessSliderViewId
            )
        )
        updateColorWheel()
        currentColorCircle = findNearestByColor(initialColor ?: return)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateColorWheel()
    }

    private fun updateColorWheel() {
        var width = measuredWidth
        val height = measuredHeight
        if (height < width) width = height
        if (width <= 0) return
        if (colorWheel == null || (colorWheel ?: return).width != width) {
            colorWheel = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            colorWheelCanvas = Canvas(colorWheel ?: return)
            alphaPatternPaint.shader = PaintBuilder.createAlphaPatternShader(26)
        }
        if (currentColor == null || (currentColor ?: return).width != width) {
            currentColor = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            currentColorCanvas = Canvas(currentColor ?: return)
        }
        drawColorWheel()
        invalidate()
    }

    private fun drawColorWheel() {
        colorWheelCanvas?.drawColor(0, PorterDuff.Mode.CLEAR)
        currentColorCanvas?.drawColor(0, PorterDuff.Mode.CLEAR)
        if (renderer == null) return
        val half = (colorWheelCanvas ?: return).width / 2f
        val strokeWidth: Float = STROKE_RATIO * (1f + ColorWheelRenderer.GAP_PERCENTAGE)
        val maxRadius = half - strokeWidth - half / density
        val cSize = maxRadius / (density - 1) / 2
        val colorWheelRenderOption = (renderer ?: return).renderOption
        colorWheelRenderOption.density = density
        colorWheelRenderOption.maxRadius = maxRadius
        colorWheelRenderOption.cSize = cSize
        colorWheelRenderOption.strokeWidth = strokeWidth
        colorWheelRenderOption.alpha = pAlpha
        colorWheelRenderOption.lightness = lightness
        colorWheelRenderOption.targetCanvas = colorWheelCanvas
        renderer?.initWith(colorWheelRenderOption)
        renderer?.draw()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = 0
        when (widthMode) {
            MeasureSpec.UNSPECIFIED -> width =
                widthMeasureSpec

            MeasureSpec.AT_MOST -> width =
                MeasureSpec.getSize(widthMeasureSpec)

            MeasureSpec.EXACTLY -> width =
                MeasureSpec.getSize(widthMeasureSpec)
        }
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = 0
        when (heightMode) {
            MeasureSpec.UNSPECIFIED -> height =
                heightMeasureSpec

            MeasureSpec.AT_MOST -> height =
                MeasureSpec.getSize(heightMeasureSpec)

            MeasureSpec.EXACTLY -> height =
                MeasureSpec.getSize(heightMeasureSpec)
        }
        val squareDimen = height.coerceAtMost(width)
        setMeasuredDimension(squareDimen, squareDimen)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val lastSelectedColor = selectedColor
                currentColorCircle = findNearestByPosition(event.x, event.y)
                val selectedColor = selectedColor
                callOnColorChangedListeners(lastSelectedColor, selectedColor)
                initialColor = selectedColor
                setColorToSliders(selectedColor)
                updateColorWheel()
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                val selectedColor = selectedColor
                for (listener in listeners) {
                    try {
                        listener.onColorSelected(selectedColor)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                setColorToSliders(selectedColor)
                setColorText(selectedColor)
                setColorPreviewColor(selectedColor)
                invalidate()
            }
        }
        return true
    }

    private fun callOnColorChangedListeners(oldColor: Int, newColor: Int) {
        if (oldColor != newColor) {
            for (listener in colorChangedListeners) {
                try {
                    listener.onColorChanged(newColor)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val backgroundColor = 0x00000000
        canvas.drawColor(backgroundColor)
        val maxRadius: Float = width / (1f + ColorWheelRenderer.GAP_PERCENTAGE)
        val size = maxRadius / density / 2
        if (colorWheel != null && currentColorCircle != null) {
            colorWheelFill.color =
                Color.HSVToColor((currentColorCircle ?: return).getHsvWithLightness(lightness))
            colorWheelFill.alpha = (pAlpha * 0xff).toInt()

            // a separate canvas is used to erase an issue with the alpha pattern around the edges
            // draw circle slightly larger than it needs to be, then erase edges to proper dimensions
            currentColorCanvas?.drawCircle(
                (currentColorCircle ?: return).x,
                (currentColorCircle ?: return).y,
                size + 4,
                alphaPatternPaint
            )
            currentColorCanvas?.drawCircle(
                (currentColorCircle ?: return).x,
                (currentColorCircle ?: return).y,
                size + 4,
                colorWheelFill
            )
            val selectorStroke = PaintBuilder.newPaint().color(-0x1).style(Paint.Style.STROKE)
                .stroke(size * (STROKE_RATIO - 1)).xPerMode(PorterDuff.Mode.CLEAR).build()
            if (showBorder) colorWheelCanvas?.drawCircle(
                (currentColorCircle ?: return).x,
                (currentColorCircle ?: return).y,
                size + selectorStroke.strokeWidth / 2f,
                selectorStroke
            )
            canvas.drawBitmap(colorWheel ?: return, 0f, 0f, null)
            currentColorCanvas?.drawCircle(
                (currentColorCircle ?: return).x,
                (currentColorCircle ?: return).y,
                size + selectorStroke.strokeWidth / 2f,
                selectorStroke
            )
            canvas.drawBitmap(currentColor ?: return, 0f, 0f, null)
        }
    }

    private fun findNearestByPosition(x: Float, y: Float): ColorCircle? {
        var near: ColorCircle? = null
        var minDist = Double.MAX_VALUE
        renderer?.colorCircleList()?.let {
            for (colorCircle in it) {
                val dist = colorCircle.sqDist(x, y)
                if (minDist > dist) {
                    minDist = dist
                    near = colorCircle
                }
            }
        }
        return near
    }

    private fun findNearestByColor(color: Int): ColorCircle? {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        var near: ColorCircle? = null
        var minDiff = Double.MAX_VALUE
        val x = hsv[1] * cos(hsv[0] * Math.PI / 180)
        val y = hsv[1] * sin(hsv[0] * Math.PI / 180)
        renderer?.colorCircleList()?.let {
            for (colorCircle in it) {
                val hsv1 = colorCircle.hsv
                val x1 = hsv1[1] * cos(hsv1[0] * Math.PI / 180)
                val y1 = hsv1[1] * sin(hsv1[0] * Math.PI / 180)
                val dx = x - x1
                val dy = y - y1
                val dist = dx * dx + dy * dy
                if (dist < minDiff) {
                    minDiff = dist
                    near = colorCircle
                }
            }
        }
        return near
    }

    var selectedColor: Int
        get() {
            var color = 0
            currentColorCircle?.let {
                color =
                    Utils.colorAtLightness(it.color, lightness)
            }
            return Utils.adjustAlpha(pAlpha, color)
        }
        set(previewNumber) {
            if (allColors == null || allColors!!.size < previewNumber) return
            colorSelection = previewNumber
            setHighlightedColor(previewNumber)
            val color = allColors!![previewNumber] ?: return
            setColor(color, true)
        }

    fun setInitialColors(colors: Array<Int?>?, selectedColor: Int) {
        allColors = colors
        colorSelection = selectedColor
        var initialColor = (allColors ?: return)[colorSelection]
        if (initialColor == null) initialColor = -0x1
        setInitialColor(initialColor, true)
    }

    private fun setInitialColor(color: Int, updateText: Boolean) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        pAlpha = Utils.getAlphaPercent(color)
        lightness = hsv[2]
        (allColors ?: return)[colorSelection] = color
        initialColor = color
        setColorPreviewColor(color)
        setColorToSliders(color)
        if (colorEdit != null && updateText) setColorText(color)
        currentColorCircle = findNearestByColor(color)
    }

    fun setLightness(lightness: Float) {
        val lastSelectedColor = selectedColor
        this.lightness = lightness
        if (currentColorCircle != null) {
            initialColor = Color.HSVToColor(
                Utils.alphaValueAsInt(pAlpha),
                (currentColorCircle ?: return).getHsvWithLightness(lightness)
            )
            if (colorEdit != null) (colorEdit ?: return).setText(
                Utils.getHexString(
                    initialColor ?: return, alphaSlider != null
                )
            )
            if (alphaSlider != null && initialColor != null) (alphaSlider ?: return).setColor(
                initialColor ?: return
            )
            callOnColorChangedListeners(lastSelectedColor, initialColor ?: return)
            updateColorWheel()
            invalidate()
        }
    }

    fun setColor(color: Int, updateText: Boolean) {
        setInitialColor(color, updateText)
        updateColorWheel()
        invalidate()
    }

    fun setAlphaValue(pAlpha: Float) {
        val lastSelectedColor = selectedColor
        this.pAlpha = pAlpha
        initialColor = Color.HSVToColor(
            Utils.alphaValueAsInt(this.pAlpha),
            (currentColorCircle ?: return).getHsvWithLightness(lightness)
        )
        if (colorEdit != null) (colorEdit ?: return).setText(
            Utils.getHexString(
                initialColor ?: return, alphaSlider != null
            )
        )
        if (lightnessSlider != null && initialColor != null) (lightnessSlider ?: return).setColor(
            initialColor ?: return
        )
        callOnColorChangedListeners(lastSelectedColor, initialColor ?: return)
        updateColorWheel()
        invalidate()
    }

    fun addOnColorChangedListener(listener: OnColorChangedListener) {
        colorChangedListeners.add(listener)
    }

    fun addOnColorSelectedListener(listener: OnColorSelectedListener) {
        listeners.add(listener)
    }

    fun setLightnessSlider(lightnessSlider: LightnessSlider?) {
        this.lightnessSlider = lightnessSlider
        this.lightnessSlider?.setColorPicker(this)
        this.lightnessSlider?.setColor(selectedColor)
    }

    fun setAlphaSlider(alphaSlider: AlphaSlider?) {
        this.alphaSlider = alphaSlider
        this.alphaSlider?.setColorPicker(this)
        this.alphaSlider?.setColor(selectedColor)
    }

    fun setColorEdit(colorEdit: TextInputEditText?) {
        this.colorEdit = colorEdit
        this.colorEdit?.visibility = VISIBLE
        this.colorEdit?.addTextChangedListener(colorTextChange)
    }

    fun setDensity(density: Int) {
        this.density = 2.coerceAtLeast(density)
        invalidate()
    }

    fun setRenderer(renderer: ColorWheelRenderer?) {
        this.renderer = renderer
        invalidate()
    }

    fun setColorPreview(colorPreview: LinearLayout?, selectedColor: Int?) {
        var pSelectedColor = selectedColor
        if (colorPreview == null) return
        this.colorPreview = colorPreview
        if (pSelectedColor == null) pSelectedColor = 0
        val children = colorPreview.childCount
        if (children == 0 || colorPreview.visibility != VISIBLE) return
        for (i in 0 until children) {
            val childView = colorPreview.getChildAt(i) as? LinearLayout ?: continue
            if (i == pSelectedColor) {
                childView.setBackgroundColor(Color.WHITE)
            }
            val childImage = childView.findViewById<ImageView>(R.id.image_preview)
            childImage.isClickable = true
            childImage.tag = i
            childImage.setOnClickListener { v: View? ->
                if (v == null) return@setOnClickListener
                val tag = v.tag as? Int ?: return@setOnClickListener
                pSelectedColor = tag
            }
        }
    }

    fun setShowBorder(showBorder: Boolean) {
        this.showBorder = showBorder
    }

    private fun setHighlightedColor(previewNumber: Int) {
        val children = colorPreview?.childCount ?: 0
        if (children == 0 || (colorPreview ?: return).visibility != VISIBLE) return
        for (i in 0 until children) {
            val childView = (colorPreview ?: return).getChildAt(i) as? LinearLayout ?: continue
            if (i == previewNumber) {
                childView.setBackgroundColor(Color.WHITE)
            } else {
                childView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun setColorPreviewColor(newColor: Int) {
        if (colorPreview == null || allColors == null || colorSelection > (allColors
                ?: return).size || (allColors ?: return)[colorSelection] == null
        ) return
        val children = (colorPreview ?: return).childCount
        if (children == 0 || (colorPreview ?: return).visibility != VISIBLE) return
        val childView =
            (colorPreview ?: return).getChildAt(colorSelection) as? LinearLayout ?: return
        val childImage = childView.findViewById<ImageView>(R.id.image_preview)
        childImage.setImageDrawable(ColorCircleDrawable(newColor))
    }

    private fun setColorText(argb: Int) {
        colorEdit?.setText(Utils.getHexString(argb, alphaSlider != null))
    }

    private fun setColorToSliders(selectedColor: Int) {
        if (lightnessSlider != null) (lightnessSlider ?: return).setColor(selectedColor)
        if (alphaSlider != null) (alphaSlider ?: return).setColor(selectedColor)
    }

    enum class WHEEL_TYPE {
        FLOWER, CIRCLE;

        companion object {
            fun toInt(value: WHEEL_TYPE?): Int {
                when (value) {
                    FLOWER -> return 0
                    CIRCLE -> return 1
                    else -> {}
                }
                return 0
            }

            fun indexOf(index: Int): WHEEL_TYPE {
                when (index) {
                    0 -> return FLOWER
                    1 -> return CIRCLE
                }
                return FLOWER
            }
        }
    }

    companion object {
        private const val STROKE_RATIO = 1.5f
    }
}