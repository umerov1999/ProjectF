package me.minetsh.imaging.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import me.minetsh.imaging.core.IMGImage
import me.minetsh.imaging.core.IMGMode
import me.minetsh.imaging.core.IMGPath
import me.minetsh.imaging.core.IMGText
import me.minetsh.imaging.core.anim.IMGHomingAnimator
import me.minetsh.imaging.core.homing.IMGHoming
import me.minetsh.imaging.core.sticker.IMGSticker
import me.minetsh.imaging.core.sticker.IMGStickerPortrait
import kotlin.math.roundToInt

/**
 * Created by felix on 2017/11/14 下午6:43.
 */
// TODO clip外不加入path
class IMGView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Runnable, OnScaleGestureListener,
    AnimatorUpdateListener, IMGStickerPortrait.Callback, Animator.AnimatorListener {
    private val mImage: IMGImage = IMGImage()
    private val mPen = Pen()
    private val mDoodlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mMosaicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mPreMode = IMGMode.NONE
    private var mGDetector: GestureDetector? = null
    private var mSGDetector: ScaleGestureDetector? = null
    private var mHomingAnimator: IMGHomingAnimator? = null
    private var mPointerCount = 0

    init {
        // 涂鸦画刷
        mDoodlePaint.style = Paint.Style.STROKE
        mDoodlePaint.strokeWidth = IMGPath.BASE_DOODLE_WIDTH
        mDoodlePaint.color = Color.RED
        mDoodlePaint.setPathEffect(CornerPathEffect(IMGPath.BASE_DOODLE_WIDTH))
        mDoodlePaint.strokeCap = Paint.Cap.ROUND
        mDoodlePaint.strokeJoin = Paint.Join.ROUND

        // 马赛克画刷
        mMosaicPaint.style = Paint.Style.STROKE
        mMosaicPaint.strokeWidth = IMGPath.BASE_MOSAIC_WIDTH
        mMosaicPaint.color = Color.BLACK
        mMosaicPaint.setPathEffect(CornerPathEffect(IMGPath.BASE_MOSAIC_WIDTH))
        mMosaicPaint.strokeCap = Paint.Cap.ROUND
        mMosaicPaint.strokeJoin = Paint.Join.ROUND

        initialize(context)
    }

    private fun initialize(context: Context) {
        mPen.mode = mImage.getMode()
        mGDetector = GestureDetector(context, MoveAdapter())
        mSGDetector = ScaleGestureDetector(context, this)
    }

    fun setImageBitmap(image: Bitmap?) {
        mImage.setBitmap(image)
        invalidate()
    }

    val isHoming: Boolean
        /**
         * 是否真正修正归位
         */
        get() = (mHomingAnimator?.isRunning == true)

    private fun onHoming() {
        invalidate()
        stopHoming()
        startHoming(
            mImage.getStartHoming(scrollX.toFloat(), scrollY.toFloat()),
            mImage.getEndHoming(scrollX.toFloat(), scrollY.toFloat())
        )
    }

    private fun startHoming(sHoming: IMGHoming, eHoming: IMGHoming) {
        if (mHomingAnimator == null) {
            mHomingAnimator = IMGHomingAnimator()
            mHomingAnimator?.addUpdateListener(this)
            mHomingAnimator?.addListener(this)
        }
        mHomingAnimator?.setHomingValues(sHoming, eHoming)
        mHomingAnimator?.start()
    }

    private fun stopHoming() {
        mHomingAnimator?.cancel()
    }

    fun doRotate() {
        if (!isHoming) {
            mImage.rotate(-90)
            onHoming()
        }
    }

    fun resetClip() {
        mImage.resetClip()
        onHoming()
    }

    fun doClip() {
        mImage.clip(scrollX.toFloat(), scrollY.toFloat())
        mode = mPreMode
        onHoming()
    }

    fun cancelClip() {
        mImage.toBackupClip()
        mode = mPreMode
    }

    fun setPenColor(color: Int) {
        mPen.color = color
    }

    val isDoodleEmpty: Boolean
        get() = mImage.isDoodleEmpty

    fun undoDoodle() {
        mImage.undoDoodle()
        invalidate()
    }

    val isMosaicEmpty: Boolean
        get() = mImage.isMosaicEmpty

    fun undoMosaic() {
        mImage.undoMosaic()
        invalidate()
    }

    var mode: IMGMode?
        get() = mImage.getMode()
        set(mode) {
            // 保存现在的编辑模式
            mPreMode = mImage.getMode()

            // 设置新的编辑模式
            if (mode != null) {
                mImage.setMode(mode)
            }
            if (mode != null) {
                mPen.mode = mode
            }

            // 矫正区域
            onHoming()
        }

    override fun onDraw(canvas: Canvas) {
        onDrawImages(canvas)
    }

    private fun onDrawImages(canvas: Canvas) {
        canvas.withSave {

            // clip 中心旋转
            val clipFrame = mImage.getClipFrame()
            rotate(mImage.getRotate(), clipFrame.centerX(), clipFrame.centerY())

            // 图片
            mImage.onDrawImage(this)

            // 马赛克
            if (!mImage.isMosaicEmpty || mImage.getMode() == IMGMode.MOSAIC && !mPen.isEmpty) {
                val count = mImage.onDrawMosaicsPath(this)
                if (mImage.getMode() == IMGMode.MOSAIC && !mPen.isEmpty) {
                    mDoodlePaint.strokeWidth = IMGPath.BASE_MOSAIC_WIDTH
                    withSave {
                        val frame = mImage.getClipFrame()
                        rotate(-mImage.getRotate(), frame.centerX(), frame.centerY())
                        translate(scrollX.toFloat(), scrollY.toFloat())
                        drawPath(mPen.path, mDoodlePaint)
                    }
                }
                mImage.onDrawMosaic(this, count)
            }

            // 涂鸦
            mImage.onDrawDoodles(this)
            if (mImage.getMode() === IMGMode.DOODLE && !mPen.isEmpty) {
                mDoodlePaint.color = mPen.color
                mDoodlePaint.strokeWidth = IMGPath.BASE_DOODLE_WIDTH * mImage.getScale()
                withSave {
                    val frame = mImage.getClipFrame()
                    rotate(-mImage.getRotate(), frame.centerX(), frame.centerY())
                    translate(scrollX.toFloat(), scrollY.toFloat())
                    drawPath(mPen.path, mDoodlePaint)
                }
            }

            // TODO
            if (mImage.isFreezing()) {
                // 文字贴片
                mImage.onDrawStickers(this)
            }
            mImage.onDrawShade(this)
        }

        // TODO
        if (!mImage.isFreezing()) {
            // 文字贴片
            mImage.onDrawStickerClip(canvas)
            mImage.onDrawStickers(canvas)
        }

        // 裁剪
        if (mImage.getMode() == IMGMode.CLIP) {
            canvas.withTranslation(scrollX.toFloat(), scrollY.toFloat()) {
                mImage.onDrawClip(this)
            }
        }
    }

    fun saveBitmap(): Bitmap {
        mImage.stickAll()
        val scale = 1f / mImage.getScale()
        val frame = RectF(mImage.getClipFrame())

        // 旋转基画布
        val m = Matrix()
        m.setRotate(mImage.getRotate(), frame.centerX(), frame.centerY())
        m.mapRect(frame)

        // 缩放基画布
        m.setScale(scale, scale, frame.left, frame.top)
        m.mapRect(frame)
        val bitmap = createBitmap(
            frame.width().roundToInt(),
            frame.height().roundToInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        // 平移到基画布原点&缩放到原尺寸
        canvas.translate(-frame.left, -frame.top)
        canvas.scale(scale, scale, frame.left, frame.top)
        onDrawImages(canvas)
        return bitmap
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            mImage.onWindowChanged((right - left).toFloat(), (bottom - top).toFloat())
        }
    }

    fun <V> addStickerView(
        stickerView: V?,
        params: LayoutParams?
    ) where V : View?, V : IMGSticker? {
        if (stickerView != null) {
            addView(stickerView, params)
            stickerView.registerCallback(this)
            mImage.addSticker<V>(stickerView)
        }
    }

    fun addStickerText(text: IMGText?) {
        val textView = IMGStickerTextView(context)
        textView.text = text
        val layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        // Center of the drawing window.
        layoutParams.gravity = Gravity.CENTER
        textView.x = scrollX.toFloat()
        textView.y = scrollY.toFloat()
        addStickerView(textView, layoutParams)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            onInterceptTouch() || super.onInterceptTouchEvent(ev)
        } else super.onInterceptTouchEvent(ev)
    }

    fun onInterceptTouch(): Boolean {
        return if (isHoming) {
            stopHoming()
            true
        } else mImage.getMode() == IMGMode.CLIP
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> removeCallbacks(this)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> postDelayed(this, 1200)
        }
        return onTouch(event)
    }

    fun onTouch(event: MotionEvent): Boolean {
        if (isHoming) {
            // Homing
            return false
        }
        mPointerCount = event.pointerCount
        var handled = mSGDetector?.onTouchEvent(event) == true
        val mode = mImage.getMode()
        handled = if (mode == IMGMode.NONE || mode == IMGMode.CLIP) {
            handled or onTouchNONE(event)
        } else if (mPointerCount > 1) {
            onPathDone()
            handled or onTouchNONE(event)
        } else {
            handled or onTouchPath(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> mImage.onTouchDown(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mImage.onTouchUp()
                onHoming()
            }
        }
        return handled
    }

    private fun onTouchNONE(event: MotionEvent): Boolean {
        return mGDetector?.onTouchEvent(event) == true
    }

    private fun onTouchPath(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onPathBegin(event)
            MotionEvent.ACTION_MOVE -> onPathMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mPen.isIdentity(
                event.getPointerId(
                    0
                )
            ) && onPathDone()

            else -> false
        }
    }

    private fun onPathBegin(event: MotionEvent): Boolean {
        mPen.reset(event.x, event.y)
        mPen.setIdentity(event.getPointerId(0))
        return true
    }

    private fun onPathMove(event: MotionEvent): Boolean {
        if (mPen.isIdentity(event.getPointerId(0))) {
            mPen.lineTo(event.x, event.y)
            invalidate()
            return true
        }
        return false
    }

    private fun onPathDone(): Boolean {
        if (mPen.isEmpty) {
            return false
        }
        mImage.addPath(mPen.toPath(), scrollX.toFloat(), scrollY.toFloat())
        mPen.reset()
        invalidate()
        return true
    }

    override fun run() {
        // 稳定触发
        if (!onSteady()) {
            postDelayed(this, 500)
        }
    }

    fun onSteady(): Boolean {
        if (DEBUG) {
            Log.d(TAG, "onSteady: isHoming=$isHoming")
        }
        if (!isHoming) {
            mImage.onSteady()
            onHoming()
            return true
        }
        return false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(this)
        mImage.release()
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (mPointerCount > 1) {
            mImage.onScale(
                detector.scaleFactor,
                scrollX + detector.focusX,
                scrollY + detector.focusY
            )
            invalidate()
            return true
        }
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        if (mPointerCount > 1) {
            mImage.onScaleBegin()
            return true
        }
        return false
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        mImage.onScaleEnd()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        mImage.onHoming(animation.animatedFraction)
        toApplyHoming(animation.animatedValue as IMGHoming)
    }

    private fun toApplyHoming(homing: IMGHoming) {
        mImage.setScale(homing.scale)
        mImage.setRotate(homing.rotate)
        if (!onScrollTo(homing.x.roundToInt(), homing.y.roundToInt())) {
            invalidate()
        }
    }

    private fun onScrollTo(x: Int, y: Int): Boolean {
        if (scrollX != x || scrollY != y) {
            scrollTo(x, y)
            return true
        }
        return false
    }

    override fun <V> onDismiss(stickerView: V) where V : View, V : IMGSticker {
        mImage.onDismiss(stickerView)
        invalidate()
    }

    override fun <V> onShowing(stickerView: V) where V : View, V : IMGSticker {
        mImage.onShowing(stickerView)
        invalidate()
    }

    override fun <V> onRemove(stickerView: V): Boolean where V : View, V : IMGSticker {
        mImage.onRemoveSticker(stickerView)
        stickerView.unregisterCallback(this)
        val parent = stickerView.parent
        if (parent != null) {
            (parent as ViewGroup).removeView(stickerView)
        }
        return true
    }

    override fun onAnimationStart(animation: Animator) {
        if (DEBUG) {
            Log.d(TAG, "onAnimationStart")
        }
        mImage.onHomingStart()
    }

    override fun onAnimationEnd(animation: Animator) {
        if (DEBUG) {
            Log.d(TAG, "onAnimationEnd")
        }
        if (mImage.onHomingEnd()) {
            toApplyHoming(mImage.clip(scrollX.toFloat(), scrollY.toFloat()))
        }
    }

    override fun onAnimationCancel(animation: Animator) {
        if (DEBUG) {
            Log.d(TAG, "onAnimationCancel")
        }
        mImage.onHomingCancel()
    }

    override fun onAnimationRepeat(animation: Animator) {
        // empty implementation.
    }

    fun onScroll(dx: Float, dy: Float): Boolean {
        val homing = mImage.onScroll(scrollX.toFloat(), scrollY.toFloat(), -dx, -dy)
        if (homing != null) {
            toApplyHoming(homing)
            return true
        }
        return onScrollTo(scrollX + dx.roundToInt(), scrollY + dy.roundToInt())
    }

    internal class Pen : IMGPath() {
        private var identity = Int.MIN_VALUE
        fun reset() {
            path.reset()
            identity = Int.MIN_VALUE
        }

        fun reset(x: Float, y: Float) {
            path.reset()
            path.moveTo(x, y)
            identity = Int.MIN_VALUE
        }

        fun setIdentity(identity: Int) {
            this.identity = identity
        }

        fun isIdentity(identity: Int): Boolean {
            return this.identity == identity
        }

        fun lineTo(x: Float, y: Float) {
            path.lineTo(x, y)
        }

        val isEmpty: Boolean
            get() = path.isEmpty

        fun toPath(): IMGPath {
            return IMGPath(Path(path), mode, color, width)
        }
    }

    internal inner class MoveAdapter : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return this@IMGView.onScroll(distanceX, distanceY)
        }

    }

    companion object {
        private const val TAG = "IMGView"
        private const val DEBUG = true
    }
}
