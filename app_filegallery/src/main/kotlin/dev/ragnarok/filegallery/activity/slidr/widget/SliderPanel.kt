package dev.ragnarok.filegallery.activity.slidr.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.model.SlidrInterface
import dev.ragnarok.filegallery.activity.slidr.model.SlidrPosition
import dev.ragnarok.filegallery.activity.slidr.util.ViewHelper.hasScrollableChildUnderPoint
import kotlin.math.abs

class SliderPanel : FrameLayout {
    private var screenWidth = 0
    private var screenHeight = 0
    private var decorView: View? = null
    private var dragHelper: ViewDragHelper? = null
    private var listener: OnPanelSlideListener? = null
    private lateinit var scrimPaint: Paint
    private var scrimRenderer: ScrimRenderer? = null
    private var isLocked = false

    /**
     * Get the default [SlidrInterface] from which to control the panel with after attachment
     */
    val defaultInterface: SlidrInterface = object : SlidrInterface {
        override fun lock() {
            this@SliderPanel.lock()
        }

        override fun unlock() {
            this@SliderPanel.unlock()
        }
    }
    private var isEdgeTouched = false
    private var edgePosition = 0
    private var config: SlidrConfig = SlidrConfig.Builder().build()
    private var startX = 0f
    private var startY = 0f

    /**
     * The drag helper callback interface for the Left position
     */
    private val leftCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                child,
                SlidrPosition.LEFT,
                startX.toInt(),
                startY.toInt()
            )
            val edgeCase =
                !config.isEdgeOnly || dragHelper?.isEdgeTouched(edgePosition, pointerId) == true
            return !canChildScroll && child.id == decorView?.id && edgeCase
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return clamp(left, 0, screenWidth)
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return screenWidth
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val left = releasedChild.left
            var settleLeft = 0
            val leftThreshold = (width * config.getDistanceThreshold()).toInt()
            val isVerticalSwiping = abs(yvel) > config.getVelocityThreshold()
            if (xvel > 0) {
                if (abs(xvel) > config.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = screenWidth
                } else if (left > leftThreshold) {
                    settleLeft = screenWidth
                }
            } else if (xvel == 0f) {
                if (left > leftThreshold) {
                    settleLeft = screenWidth
                }
            }
            dragHelper?.settleCapturedViewAt(settleLeft, releasedChild.top)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - left.toFloat() / screenWidth.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.left == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    /**
     * The drag helper callbacks for dragging the slidr attachment from the right of the screen
     */
    private val rightCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                child,
                SlidrPosition.RIGHT,
                startX.toInt(),
                startY.toInt()
            )
            val edgeCase =
                !config.isEdgeOnly || dragHelper?.isEdgeTouched(edgePosition, pointerId) == true
            return !canChildScroll && child.id == decorView?.id && edgeCase
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return clamp(left, -screenWidth, 0)
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return screenWidth
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val left = releasedChild.left
            var settleLeft = 0
            val leftThreshold = (width * config.getDistanceThreshold()).toInt()
            val isVerticalSwiping = abs(yvel) > config.getVelocityThreshold()
            if (xvel < 0) {
                if (abs(xvel) > config.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = -screenWidth
                } else if (left < -leftThreshold) {
                    settleLeft = -screenWidth
                }
            } else if (xvel == 0f) {
                if (left < -leftThreshold) {
                    settleLeft = -screenWidth
                }
            }
            dragHelper?.settleCapturedViewAt(settleLeft, releasedChild.top)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - abs(left)
                .toFloat() / screenWidth.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.left == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    /**
     * The drag helper callbacks for dragging the slidr attachment from the top of the screen
     */
    private val topCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                child,
                SlidrPosition.TOP,
                startX.toInt(),
                startY.toInt()
            )
            return !canChildScroll && child.id == decorView?.id && (!config.isEdgeOnly || isEdgeTouched)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return clamp(top, 0, screenHeight)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return screenHeight
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val top = releasedChild.top
            var settleTop = 0
            val topThreshold = (height * config.getDistanceThreshold()).toInt()
            val isSideSwiping = abs(xvel) > config.getVelocityThreshold()
            if (yvel > 0) {
                if (abs(yvel) > config.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = screenHeight
                } else if (top > topThreshold) {
                    settleTop = screenHeight
                }
            } else if (yvel == 0f) {
                if (top > topThreshold) {
                    settleTop = screenHeight
                }
            }
            dragHelper?.settleCapturedViewAt(releasedChild.left, settleTop)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - abs(top)
                .toFloat() / screenHeight.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.top == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    /**
     * The drag helper callbacks for dragging the slidr attachment from the bottom of hte screen
     */
    private val bottomCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                child,
                SlidrPosition.BOTTOM,
                startX.toInt(),
                startY.toInt()
            )
            return !canChildScroll && child.id == decorView?.id && (!config.isEdgeOnly || isEdgeTouched)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return clamp(top, -screenHeight, 0)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return screenHeight
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val top = releasedChild.top
            var settleTop = 0
            val topThreshold = (height * config.getDistanceThreshold()).toInt()
            val isSideSwiping = abs(xvel) > config.getVelocityThreshold()
            if (yvel < 0) {
                if (abs(yvel) > config.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = -screenHeight
                } else if (top < -topThreshold) {
                    settleTop = -screenHeight
                }
            } else if (yvel == 0f) {
                if (top < -topThreshold) {
                    settleTop = -screenHeight
                }
            }
            dragHelper?.settleCapturedViewAt(releasedChild.left, settleTop)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - abs(top)
                .toFloat() / screenHeight.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.top == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    /**
     * The drag helper callbacks for dragging the slidr attachment in both vertical directions
     */
    private val verticalCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child.id == decorView?.id && (!config.isEdgeOnly || isEdgeTouched)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if (top > 0 && dy > 0 || top < 0 && dy < 0) {
                val slidrPosition = if (dy > 0) SlidrPosition.TOP else SlidrPosition.BOTTOM
                val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                    child,
                    slidrPosition,
                    startX.toInt(),
                    startY.toInt()
                )
                if (canChildScroll) {
                    return 0
                }
            }
            return clamp(top, -screenHeight, screenHeight)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return screenHeight
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val top = releasedChild.top
            var settleTop = 0
            val topThreshold = (height * config.getDistanceThreshold()).toInt()
            val isSideSwiping = abs(xvel) > config.getVelocityThreshold()
            if (yvel > 0) {

                // Being slinged down
                if (abs(yvel) > config.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = screenHeight
                } else if (top > topThreshold) {
                    settleTop = screenHeight
                }
            } else if (yvel < 0) {
                // Being slinged up
                if (abs(yvel) > config.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = -screenHeight
                } else if (top < -topThreshold) {
                    settleTop = -screenHeight
                }
            } else {
                if (top > topThreshold) {
                    settleTop = screenHeight
                } else if (top < -topThreshold) {
                    settleTop = -screenHeight
                }
            }
            dragHelper?.settleCapturedViewAt(releasedChild.left, settleTop)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - abs(top)
                .toFloat() / screenHeight.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.top == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    /**
     * The drag helper callbacks for dragging the slidr attachment in both horizontal directions
     */
    private val horizontalCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val edgeCase =
                !config.isEdgeOnly || dragHelper?.isEdgeTouched(edgePosition, pointerId) == true
            return child.id == decorView?.id && edgeCase
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (left > 0 && dx > 0 || left < 0 && dx < 0) {
                val slidrPosition = if (dx > 0) SlidrPosition.LEFT else SlidrPosition.RIGHT
                val canChildScroll = !config.isIgnoreChildScroll && hasScrollableChildUnderPoint(
                    child,
                    slidrPosition,
                    startX.toInt(),
                    startY.toInt()
                )
                if (canChildScroll) {
                    return 0
                }
            }
            return clamp(left, -screenWidth, screenWidth)
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return screenWidth
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val left = releasedChild.left
            var settleLeft = 0
            val leftThreshold = (width * config.getDistanceThreshold()).toInt()
            val isVerticalSwiping = abs(yvel) > config.getVelocityThreshold()
            if (xvel > 0) {
                if (abs(xvel) > config.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = screenWidth
                } else if (left > leftThreshold) {
                    settleLeft = screenWidth
                }
            } else if (xvel < 0) {
                if (abs(xvel) > config.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = -screenWidth
                } else if (left < -leftThreshold) {
                    settleLeft = -screenWidth
                }
            } else {
                if (left > leftThreshold) {
                    settleLeft = screenWidth
                } else if (left < -leftThreshold) {
                    settleLeft = -screenWidth
                }
            }
            dragHelper?.settleCapturedViewAt(settleLeft, releasedChild.top)
            invalidate()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val percent = 1f - abs(left)
                .toFloat() / screenWidth.toFloat()
            listener?.onSlideChange(percent)

            // Update the dimmer alpha
            applyScrim(percent)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            listener?.onStateChanged(state)
            when (state) {
                ViewDragHelper.STATE_IDLE -> if (decorView?.left == 0) {
                    // State Open
                    listener?.onOpened()
                } else {
                    // State Closed
                    listener?.onClosed()
                }

                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {}
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, decorView: View?, config: SlidrConfig?) : super(
        context
    ) {
        this.decorView = decorView
        this.config = config ?: SlidrConfig.Builder().build()
        init()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isLocked) {
            return false
        }
        startX = ev.x
        startY = ev.y
        if (config.isEdgeOnly) {
            isEdgeTouched = canDragFromEdge(ev)
        }

        // Fix for pull request #13 and issue #12
        val interceptForDrag: Boolean = try {
            dragHelper?.shouldInterceptTouchEvent(ev) == true
        } catch (e: Exception) {
            false
        }
        return interceptForDrag && !isLocked
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            return false
        }
        try {
            dragHelper?.processTouchEvent(event)
        } catch (e: IllegalArgumentException) {
            return false
        }
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (dragHelper?.continueSettling(true) == true) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        scrimRenderer?.render(canvas, config.position, scrimPaint)
    }

    /**
     * Set the panel slide listener that gets called based on slider changes
     *
     * @param listener callback implementation
     */
    fun setOnPanelSlideListener(listener: OnPanelSlideListener?) {
        this.listener = listener
    }

    private fun init() {
        setWillNotDraw(false)
        screenWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val minVel = MIN_FLING_VELOCITY * density
        val callback: ViewDragHelper.Callback
        when (config.position) {
            SlidrPosition.RIGHT -> {
                callback = rightCallback
                edgePosition = ViewDragHelper.EDGE_RIGHT
            }

            SlidrPosition.TOP -> {
                callback = topCallback
                edgePosition = ViewDragHelper.EDGE_TOP
            }

            SlidrPosition.BOTTOM -> {
                callback = bottomCallback
                edgePosition = ViewDragHelper.EDGE_BOTTOM
            }

            SlidrPosition.VERTICAL -> {
                callback = verticalCallback
                edgePosition = ViewDragHelper.EDGE_TOP or ViewDragHelper.EDGE_BOTTOM
            }

            SlidrPosition.HORIZONTAL -> {
                callback = horizontalCallback
                edgePosition = ViewDragHelper.EDGE_LEFT or ViewDragHelper.EDGE_RIGHT
            }

            else -> {
                callback = leftCallback
                edgePosition = ViewDragHelper.EDGE_LEFT
            }
        }
        dragHelper = ViewDragHelper.create(this, config.getSensitivity(), callback)
        dragHelper?.minVelocity = minVel
        dragHelper?.setEdgeTrackingEnabled(edgePosition)
        isMotionEventSplittingEnabled = false

        // Setup the dimmer view
        scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.getScrimColor() >= 0) {
            scrimPaint.color = config.getScrimColor()
            scrimPaint.alpha = toAlpha(config.getScrimStartAlpha())
        } else {
            scrimPaint.color = Color.BLACK
            scrimPaint.alpha = 0
        }
        decorView?.let {
            scrimRenderer = ScrimRenderer(this, it)
        }

        /*
         * This is so we can get the height of the view and
         * ignore the system navigation that would be included if we
         * retrieved this value from the DisplayMetrics
         */post { screenHeight = height }
    }

    internal fun lock() {
        dragHelper?.abort()
        isLocked = true
    }

    internal fun unlock() {
        dragHelper?.abort()
        isLocked = false
    }

    private fun canDragFromEdge(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        return when (config.position) {
            SlidrPosition.LEFT -> x < config.getEdgeSize(width.toFloat())
            SlidrPosition.RIGHT -> x > width - config.getEdgeSize(width.toFloat())
            SlidrPosition.BOTTOM -> y > height - config.getEdgeSize(height.toFloat())
            SlidrPosition.TOP -> y < config.getEdgeSize(height.toFloat())
            SlidrPosition.HORIZONTAL -> x < config.getEdgeSize(width.toFloat()) || x > width - config.getEdgeSize(
                width.toFloat()
            )

            SlidrPosition.VERTICAL -> y < config.getEdgeSize(height.toFloat()) || y > height - config.getEdgeSize(
                height.toFloat()
            )
        }
    }

    internal fun applyScrim(percent: Float) {
        if (config.getScrimColor() < 0) {
            return
        }
        val alpha =
            percent * (config.getScrimStartAlpha() - config.getScrimEndAlpha()) + config.getScrimEndAlpha()
        scrimPaint.alpha = toAlpha(alpha)
        //invalidate(scrimRenderer.getDirtyRect(config.getPosition()));
        invalidate()
    }

    /**
     * The panel sliding interface that gets called
     * whenever the panel is closed or opened
     */
    interface OnPanelSlideListener {
        fun onStateChanged(state: Int)
        fun onClosed()
        fun onOpened()
        fun onSlideChange(percent: Float)
    }

    companion object {
        private const val MIN_FLING_VELOCITY = 400 // dips per second
        internal fun clamp(value: Int, min: Int, max: Int): Int {
            return min.coerceAtLeast(max.coerceAtMost(value))
        }

        internal fun toAlpha(percentage: Float): Int {
            return (percentage * 255).toInt()
        }
    }
}