package dev.ragnarok.fenrir.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Property
import android.view.View
import dev.ragnarok.fenrir.R

class CircleRoadProgress(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var circleCenterPointX = 0f
    private var circleCenterPointY = 0f
    private var roadColor = 0
    private var roadStrokeWidth = 0f
    private var roadRadius = 0f
    private var arcLoadingColor = 0
    private var arcLoadingStrokeWidth = 0f
    private var arcLoadingStartAngle = 0f
    private var displayedPercentage = 0f
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        circleCenterPointX = w.toFloat() / 2
        circleCenterPointY = h.toFloat() / 2
        val paddingInContainer = 3
        roadRadius = w.toFloat() / 2 - roadStrokeWidth / 2 - paddingInContainer
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawRoad(canvas)
        drawArcLoading(canvas)
    }

    private fun initializeAttributes(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleRoadProgress)
        circleCenterPointX = ta.getFloat(R.styleable.CircleRoadProgress_circleCenterPointX, 54f)
        circleCenterPointY = ta.getFloat(R.styleable.CircleRoadProgress_circleCenterPointY, 54f)
        roadColor =
            ta.getColor(R.styleable.CircleRoadProgress_roadColor, Color.parseColor("#575757"))
        roadStrokeWidth =
            ta.getDimensionPixelSize(R.styleable.CircleRoadProgress_roadStrokeWidth, 10).toFloat()
        roadRadius =
            ta.getDimensionPixelSize(R.styleable.CircleRoadProgress_roadRadius, 42).toFloat()
        arcLoadingColor =
            ta.getColor(R.styleable.CircleRoadProgress_arcLoadingColor, Color.parseColor("#f5d600"))
        arcLoadingStrokeWidth =
            ta.getDimensionPixelSize(R.styleable.CircleRoadProgress_arcLoadingStrokeWidth, 3)
                .toFloat()
        arcLoadingStartAngle =
            ta.getFloat(R.styleable.CircleRoadProgress_arcLoadingStartAngle, 270f)
        ta.recycle()
    }

    private fun drawRoad(canvas: Canvas) {
        PAINT.isDither = true
        PAINT.color = roadColor
        PAINT.style = Paint.Style.STROKE
        PAINT.strokeWidth = roadStrokeWidth
        PAINT.strokeCap = Paint.Cap.ROUND
        PAINT.strokeJoin = Paint.Join.ROUND
        canvas.drawCircle(circleCenterPointX, circleCenterPointY, roadRadius, PAINT)
    }

    private fun drawArcLoading(canvas: Canvas) {
        PAINT.color = arcLoadingColor
        PAINT.strokeWidth = arcLoadingStrokeWidth
        val delta = circleCenterPointX - roadRadius
        val arcSize = (circleCenterPointX - delta / 2f) * 2f
        val box = RectF(delta, delta, arcSize, arcSize)
        //float sweep = 360 * percent * 0.01f;
        val sweep = 360 * displayedPercentage * 0.01f
        canvas.drawArc(box, arcLoadingStartAngle, sweep, false, PAINT)
    }

    fun changePercentage(percent: Int) {
        displayedPercentage = percent.toFloat()
        invalidate()
    }

    fun changePercentageSmoothly(percent: Int) {
        val animator = ObjectAnimator.ofFloat(this, PROGRESS_PROPERTY, percent.toFloat())
        animator.duration = 750
        //animator.setInterpolator(new AccelerateInterpolator(1.75f));
        animator.start()
    }

    companion object {
        private val PAINT =
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val PROGRESS_PROPERTY: Property<CircleRoadProgress, Float> =
            object : Property<CircleRoadProgress, Float>(
                Float::class.java, "displayed-precentage"
            ) {
                override fun get(view: CircleRoadProgress): Float {
                    return view.displayedPercentage
                }

                override fun set(view: CircleRoadProgress, value: Float) {
                    view.displayedPercentage = value
                    view.invalidate()
                }
            }
    }

    init {
        initializeAttributes(context, attrs)
    }
}