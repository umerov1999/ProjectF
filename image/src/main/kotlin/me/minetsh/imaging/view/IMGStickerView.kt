package me.minetsh.imaging.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import me.minetsh.imaging.R
import me.minetsh.imaging.core.sticker.IMGSticker
import me.minetsh.imaging.core.sticker.IMGStickerAdjustHelper
import me.minetsh.imaging.core.sticker.IMGStickerHelper
import me.minetsh.imaging.core.sticker.IMGStickerMoveHelper
import me.minetsh.imaging.core.sticker.IMGStickerPortrait
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Created by felix on 2017/12/12 下午4:26.
 */
abstract class IMGStickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), IMGSticker, View.OnClickListener {
    private var PAINT: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mMatrix = Matrix()
    private val mFrame = RectF()
    private val mTempFrame = Rect()
    private var mContentView: View? = null
    private var mScale = 1f

    // TODO
    private var mDownShowing = 0
    private var mMoveHelper: IMGStickerMoveHelper? = null
    private var mStickerHelper: IMGStickerHelper<IMGStickerView>? = null
    private var mRemoveView: ImageView? = null
    private var mAdjustView: ImageView? = null

    init {
        PAINT.color = Color.WHITE
        PAINT.style = Paint.Style.STROKE
        PAINT.strokeWidth = STROKE_WIDTH
    }

    fun doInitialize(context: Context) {
        setBackgroundColor(Color.TRANSPARENT)
        mContentView = onCreateContentView(context)
        addView(mContentView, contentLayoutParams)
        mRemoveView = AppCompatImageView(context)
        mRemoveView?.scaleType = ImageView.ScaleType.FIT_XY
        mRemoveView?.setImageResource(R.drawable.image_ic_delete)
        addView(mRemoveView, anchorLayoutParams)
        mRemoveView?.setOnClickListener(this)
        mAdjustView = AppCompatImageView(context)
        mAdjustView?.scaleType = ImageView.ScaleType.FIT_XY
        mAdjustView?.setImageResource(R.drawable.image_ic_adjust)
        addView(mAdjustView, anchorLayoutParams)
        mAdjustView?.let {
            IMGStickerAdjustHelper(this, it)
        }
        mStickerHelper = IMGStickerHelper(this)
        mMoveHelper = IMGStickerMoveHelper(this)
    }

    abstract fun onCreateContentView(context: Context): View?
    override fun getScale(): Float {
        return mScale
    }

    override fun setScale(scale: Float) {
        mScale = scale
        mContentView?.scaleX = mScale
        mContentView?.scaleY = mScale
        val pivotX = left + right shr 1
        val pivotY = top + bottom shr 1
        mFrame[pivotX.toFloat(), pivotY.toFloat(), pivotX.toFloat()] = pivotY.toFloat()
        mFrame.inset(
            -((mContentView?.measuredWidth ?: 0) shr 1).toFloat(),
            -((mContentView?.measuredHeight ?: 0) shr 1).toFloat()
        )
        mMatrix.setScale(mScale, mScale, mFrame.centerX(), mFrame.centerY())
        mMatrix.mapRect(mFrame)
        mFrame.round(mTempFrame)
        layout(mTempFrame.left, mTempFrame.top, mTempFrame.right, mTempFrame.bottom)
    }

    override fun addScale(scale: Float) {
        setScale(getScale() * scale)
    }

    private val contentLayoutParams: LayoutParams
        get() = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    private val anchorLayoutParams: LayoutParams
        get() = LayoutParams(ANCHOR_SIZE, ANCHOR_SIZE)

    override fun draw(canvas: Canvas) {
        if (isShowing()) {
            canvas.drawRect(
                ANCHOR_SIZE_HALF.toFloat(), ANCHOR_SIZE_HALF.toFloat(),
                (
                        width - ANCHOR_SIZE_HALF).toFloat(),
                (
                        height - ANCHOR_SIZE_HALF).toFloat(), PAINT
            )
        }
        super.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = childCount
        var maxHeight = 0
        var maxWidth = 0
        var childState = 0
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(widthMeasureSpec, heightMeasureSpec)
                maxWidth =
                    max(maxWidth.toFloat(), child.measuredWidth * child.scaleX).roundToInt()
                maxHeight =
                    max(maxHeight.toFloat(), child.measuredHeight * child.scaleY).roundToInt()
                childState = combineMeasuredStates(childState, child.measuredState)
            }
        }
        maxHeight = max(maxHeight, suggestedMinimumHeight)
        maxWidth = max(maxWidth, suggestedMinimumWidth)
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                maxHeight,
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mFrame[left.toFloat(), top.toFloat(), right.toFloat()] = bottom.toFloat()
        val count = childCount
        if (count == 0) {
            return
        }
        mRemoveView?.let {
            it.layout(0, 0, it.measuredWidth, it.measuredHeight)
        }
        mAdjustView?.let {
            it.layout(
                right - left - it.measuredWidth,
                bottom - top - it.measuredHeight,
                right - left, bottom - top
            )
        }
        val centerX = right - left shr 1
        val centerY = bottom - top shr 1
        val hw = (mContentView?.measuredWidth ?: 0) shr 1
        val hh = (mContentView?.measuredHeight ?: 0) shr 1
        mContentView?.layout(centerX - hw, centerY - hh, centerX + hw, centerY + hh)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return isShowing() && super.drawChild(canvas, child, drawingTime)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isShowing() && ev.action == MotionEvent.ACTION_DOWN) {
            mDownShowing = 0
            show()
            return true
        }
        return isShowing() && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = mMoveHelper?.onTouch(this, event) == true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> mDownShowing++
            MotionEvent.ACTION_UP -> if (mDownShowing > 1 && event.eventTime - event.downTime < ViewConfiguration.getTapTimeout()) {
                onContentTap()
                return true
            }
        }
        return handled or super.onTouchEvent(event)
    }

    override fun onClick(v: View) {
        if (v === mRemoveView) {
            onRemove()
        }
    }

    fun onRemove() {
        mStickerHelper?.remove()
    }

    open fun onContentTap() {}
    override fun show(): Boolean {
        return mStickerHelper?.show() == true
    }

    override fun remove(): Boolean {
        return mStickerHelper?.remove() == true
    }

    override fun dismiss(): Boolean {
        return mStickerHelper?.dismiss() == true
    }

    override fun isShowing(): Boolean {
        return mStickerHelper?.isShowing() == true
    }

    override fun getFrame(): RectF {
        return mStickerHelper?.getFrame() ?: RectF()
    }

    override fun onSticker(canvas: Canvas) {
        canvas.translate(mContentView?.x ?: 0f, mContentView?.y ?: 0f)
        mContentView?.draw(canvas)
    }

    override fun registerCallback(callback: IMGStickerPortrait.Callback) {
        mStickerHelper?.registerCallback(callback)
    }

    override fun unregisterCallback(callback: IMGStickerPortrait.Callback) {
        mStickerHelper?.unregisterCallback(callback)
    }

    companion object {
        private const val TAG = "IMGStickerView"
        private const val MAX_SCALE_VALUE = 4f
        private const val ANCHOR_SIZE = 48
        private const val ANCHOR_SIZE_HALF = ANCHOR_SIZE shr 1
        private const val STROKE_WIDTH = 3f
    }
}
