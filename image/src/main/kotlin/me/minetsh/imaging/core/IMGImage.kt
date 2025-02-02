package me.minetsh.imaging.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withSave
import me.minetsh.imaging.core.clip.IMGClip
import me.minetsh.imaging.core.clip.IMGClipWindow
import me.minetsh.imaging.core.homing.IMGHoming
import me.minetsh.imaging.core.sticker.IMGSticker
import me.minetsh.imaging.core.util.IMGUtils.fill
import me.minetsh.imaging.core.util.IMGUtils.fillHoming
import me.minetsh.imaging.core.util.IMGUtils.fitHoming
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by felix on 2017/11/21 下午10:03.
 */
class IMGImage {
    /**
     * 完整图片边框
     */
    private val mFrame = RectF()

    /**
     * 裁剪图片边框（显示的图片区域）
     */
    private val mClipFrame = RectF()
    private val mTempClipFrame = RectF()

    /**
     * 裁剪模式前状态备份
     */
    private val mBackupClipFrame = RectF()
    private val mShade = Path()

    /**
     * 裁剪窗口
     */
    private val mClipWin = IMGClipWindow()

    /**
     * 可视区域，无Scroll 偏移区域
     */
    private val mWindow = RectF()

    /**
     * 为被选中贴片
     */
    private val mBackStickers: MutableList<IMGSticker> = ArrayList()

    /**
     * 涂鸦路径
     */
    private val mDoodles: MutableList<IMGPath> = ArrayList()

    /**
     * 马赛克路径
     */
    private val mMosaics: MutableList<IMGPath> = ArrayList()
    private var mPaint: Paint
    private val M = Matrix()
    private var mImage: Bitmap?
    private var mMosaicImage: Bitmap? = null
    private var mBackupClipRotate = 0f
    private var mRotate = 0f
    private var mTargetRotate = 0f
    private var isRequestToBaseFitting = false
    private var isAnimCanceled = false

    /**
     * 裁剪模式时当前触摸锚点
     */
    private var mAnchor: IMGClip.Anchor? = null
    private var isSteady = true
    private var isDrawClip = false

    /**
     * 编辑模式
     */
    private var mMode = IMGMode.NONE
    private var isFreezing = mMode === IMGMode.CLIP

    /**
     * 是否初始位置
     */
    private var isInitialHoming = false

    /**
     * 当前选中贴片
     */
    private var mForeSticker: IMGSticker? = null
    private var mMosaicPaint: Paint? = null
    private var mShadePaint: Paint? = null

    val DEFAULT_IMAGE: Bitmap = createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    init {
        mShade.fillType = Path.FillType.WINDING

        // Doodle&Mosaic 's paint
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = IMGPath.BASE_DOODLE_WIDTH
        mPaint.color = Color.RED
        mPaint.setPathEffect(CornerPathEffect(IMGPath.BASE_DOODLE_WIDTH))
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeJoin = Paint.Join.ROUND

        mImage = DEFAULT_IMAGE
        if (mMode === IMGMode.CLIP) {
            initShadePaint()
        }
    }

    fun setBitmap(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) {
            return
        }
        mImage = bitmap

        // 清空马赛克图层
        mMosaicImage?.recycle()
        mMosaicImage = null
        makeMosaicBitmap()
        onImageChanged()
    }

    fun getMode(): IMGMode {
        return mMode
    }

    fun setMode(mode: IMGMode) {
        if (mMode === mode) return
        moveToBackground(mForeSticker)
        if (mode === IMGMode.CLIP) {
            setFreezing(true)
        }
        mMode = mode
        if (mMode === IMGMode.CLIP) {

            // 初始化Shade 画刷
            initShadePaint()

            // 备份裁剪前Clip 区域
            mBackupClipRotate = getRotate()
            mBackupClipFrame.set(mClipFrame)
            val scale: Float = 1 / getScale()
            M.setTranslate(-mFrame.left, -mFrame.top)
            M.postScale(scale, scale)
            M.mapRect(mBackupClipFrame)

            // 重置裁剪区域
            mClipWin.reset(mClipFrame, getTargetRotate())
        } else {
            if (mMode === IMGMode.MOSAIC) {
                makeMosaicBitmap()
            }
            mClipWin.isClipping = false
        }
    }

    // TODO
    private fun rotateStickers(rotate: Float) {
        M.setRotate(rotate, mClipFrame.centerX(), mClipFrame.centerY())
        for (sticker in mBackStickers) {
            M.mapRect(sticker.getFrame())
            sticker.setRotation(sticker.getRotation() + rotate)
            sticker.setX(sticker.getFrame().centerX() - sticker.getPivotX())
            sticker.setY(sticker.getFrame().centerY() - sticker.getPivotY())
        }
    }

    private fun initShadePaint() {
        if (mShadePaint == null) {
            mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mShadePaint?.color = COLOR_SHADE
            mShadePaint?.style = Paint.Style.FILL
        }
    }

    val isMosaicEmpty: Boolean
        get() = mMosaics.isEmpty()
    val isDoodleEmpty: Boolean
        get() = mDoodles.isEmpty()

    fun undoDoodle() {
        if (mDoodles.isNotEmpty()) {
            mDoodles.removeAt(mDoodles.size - 1)
        }
    }

    fun undoMosaic() {
        if (mMosaics.isNotEmpty()) {
            mMosaics.removeAt(mMosaics.size - 1)
        }
    }

    fun getClipFrame(): RectF {
        return mClipFrame
    }

    /**
     * 裁剪区域旋转回原始角度后形成新的裁剪区域，旋转中心发生变化，
     * 因此需要将视图窗口平移到新的旋转中心位置。
     */
    fun clip(scrollX: Float, scrollY: Float): IMGHoming {
        val frame = mClipWin.getOffsetFrame(scrollX, scrollY)
        M.setRotate(-getRotate(), mClipFrame.centerX(), mClipFrame.centerY())
        M.mapRect(mClipFrame, frame)
        return IMGHoming(
            scrollX + (mClipFrame.centerX() - frame.centerX()),
            scrollY + (mClipFrame.centerY() - frame.centerY()),
            getScale(), getRotate()
        )
    }

    fun toBackupClip() {
        M.setScale(getScale(), getScale())
        M.postTranslate(mFrame.left, mFrame.top)
        M.mapRect(mClipFrame, mBackupClipFrame)
        setTargetRotate(mBackupClipRotate)
        isRequestToBaseFitting = true
    }

    fun resetClip() {
        // TODO 就近旋转
        setTargetRotate(getRotate() - getRotate() % 360)
        mClipFrame.set(mFrame)
        mClipWin.reset(mClipFrame, getTargetRotate())
    }

    private fun makeMosaicBitmap() {
        if (mMosaicImage != null || mImage == null) {
            return
        }
        if (mMode === IMGMode.MOSAIC) {
            var w = ((mImage?.width ?: 0) / 64f).roundToInt()
            var h = ((mImage?.height ?: 0) / 64f).roundToInt()
            w = max(w, 8)
            h = max(h, 8)

            // 马赛克画刷
            if (mMosaicPaint == null) {
                mMosaicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                mMosaicPaint?.isFilterBitmap = false
                mMosaicPaint?.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
            }
            mImage?.let {
                mMosaicImage = it.scale(w, h, false)
            }
        }
    }

    private fun onImageChanged() {
        isInitialHoming = false
        onWindowChanged(mWindow.width(), mWindow.height())
        if (mMode === IMGMode.CLIP) {
            mClipWin.reset(mClipFrame, getTargetRotate())
        }
    }

    fun getFrame(): RectF {
        return mFrame
    }

    fun onClipHoming(): Boolean {
        return mClipWin.homing()
    }

    fun getStartHoming(scrollX: Float, scrollY: Float): IMGHoming {
        return IMGHoming(scrollX, scrollY, getScale(), getRotate())
    }

    fun getEndHoming(scrollX: Float, scrollY: Float): IMGHoming {
        val homing = IMGHoming(scrollX, scrollY, getScale(), getTargetRotate())
        if (mMode === IMGMode.CLIP) {
            val frame = RectF(mClipWin.targetFrame)
            frame.offset(scrollX, scrollY)
            if (mClipWin.isResetting) {
                val clipFrame = RectF()
                M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY())
                M.mapRect(clipFrame, mClipFrame)
                homing.rConcat(fill(frame, clipFrame))
            } else {
                val cFrame = RectF()

                // cFrame要是一个暂时clipFrame
                if (mClipWin.isHoming) {
//
//                    M.mapRect(cFrame, mClipFrame);

//                    mClipWin
                    // TODO 偏移中心
                    M.setRotate(
                        getTargetRotate() - getRotate(),
                        mClipFrame.centerX(),
                        mClipFrame.centerY()
                    )
                    M.mapRect(cFrame, mClipWin.getOffsetFrame(scrollX, scrollY))
                    homing.rConcat(
                        fitHoming(
                            frame,
                            cFrame,
                            mClipFrame.centerX(),
                            mClipFrame.centerY()
                        )
                    )
                } else {
                    M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY())
                    M.mapRect(cFrame, mFrame)
                    homing.rConcat(
                        fillHoming(
                            frame,
                            cFrame,
                            mClipFrame.centerX(),
                            mClipFrame.centerY()
                        )
                    )
                }
            }
        } else {
            val clipFrame = RectF()
            M.setRotate(getTargetRotate(), mClipFrame.centerX(), mClipFrame.centerY())
            M.mapRect(clipFrame, mClipFrame)
            val win = RectF(mWindow)
            win.offset(scrollX, scrollY)
            homing.rConcat(fitHoming(win, clipFrame, isRequestToBaseFitting))
            isRequestToBaseFitting = false
        }
        return homing
    }

    fun <S : IMGSticker?> addSticker(sticker: S?) {
        sticker?.let { moveToForeground(it) }
    }

    fun addPath(path: IMGPath?, sx: Float, sy: Float) {
        if (path == null) return
        val scale: Float = 1f / getScale()
        M.setTranslate(sx, sy)
        M.postRotate(-getRotate(), mClipFrame.centerX(), mClipFrame.centerY())
        M.postTranslate(-mFrame.left, -mFrame.top)
        M.postScale(scale, scale)
        path.transform(M)
        when (path.mode) {
            IMGMode.DOODLE -> mDoodles.add(path)
            IMGMode.MOSAIC -> {
                path.width *= scale
                mMosaics.add(path)
            }

            else -> {}
        }
    }

    private fun moveToForeground(sticker: IMGSticker?) {
        if (sticker == null) return
        moveToBackground(mForeSticker)
        if (sticker.isShowing()) {
            mForeSticker = sticker
            // 从BackStickers中移除
            mBackStickers.remove(sticker)
        } else sticker.show()
    }

    private fun moveToBackground(sticker: IMGSticker?) {
        if (sticker == null) return
        if (!sticker.isShowing()) {
            // 加入BackStickers中
            if (!mBackStickers.contains(sticker)) {
                mBackStickers.add(sticker)
            }
            if (mForeSticker === sticker) {
                mForeSticker = null
            }
        } else sticker.dismiss()
    }

    fun stickAll() {
        moveToBackground(mForeSticker)
    }

    fun onDismiss(sticker: IMGSticker?) {
        moveToBackground(sticker)
    }

    fun onShowing(sticker: IMGSticker) {
        if (mForeSticker !== sticker) {
            moveToForeground(sticker)
        }
    }

    fun onRemoveSticker(sticker: IMGSticker) {
        if (mForeSticker === sticker) {
            mForeSticker = null
        } else {
            mBackStickers.remove(sticker)
        }
    }

    fun onWindowChanged(width: Float, height: Float) {
        if (width == 0f || height == 0f) {
            return
        }
        mWindow[0f, 0f, width] = height
        if (!isInitialHoming) {
            onInitialHoming(width, height)
        } else {

            // Pivot to fit window.
            M.setTranslate(
                mWindow.centerX() - mClipFrame.centerX(),
                mWindow.centerY() - mClipFrame.centerY()
            )
            M.mapRect(mFrame)
            M.mapRect(mClipFrame)
        }
        mClipWin.setClipWinSize(width, height)
    }

    private fun onInitialHoming(width: Float, height: Float) {
        mFrame[0f, 0f, (mImage?.width?.toFloat() ?: 0f)] = (mImage?.height?.toFloat() ?: 0f)
        mClipFrame.set(mFrame)
        mClipWin.setClipWinSize(width, height)
        if (mClipFrame.isEmpty) {
            return
        }
        toBaseHoming()
        isInitialHoming = true
        onInitialHomingDone()
    }

    private fun toBaseHoming() {
        if (mClipFrame.isEmpty) {
            // Bitmap invalidate.
            return
        }
        val scale = min(
            mWindow.width() / mClipFrame.width(),
            mWindow.height() / mClipFrame.height()
        )

        // Scale to fit window.
        M.setScale(scale, scale, mClipFrame.centerX(), mClipFrame.centerY())
        M.postTranslate(
            mWindow.centerX() - mClipFrame.centerX(),
            mWindow.centerY() - mClipFrame.centerY()
        )
        M.mapRect(mFrame)
        M.mapRect(mClipFrame)
    }

    private fun onInitialHomingDone() {
        if (mMode === IMGMode.CLIP) {
            mClipWin.reset(mClipFrame, getTargetRotate())
        }
    }

    fun onDrawImage(canvas: Canvas) {

        // 裁剪区域
        canvas.clipRect(if (mClipWin.isClipping) mFrame else mClipFrame)

        // 绘制图片
        mImage?.let { canvas.drawBitmap(it, null, mFrame, null) }
        if (DEBUG) {
            // Clip 区域
            mPaint.color = Color.RED
            mPaint.strokeWidth = 6f
            canvas.drawRect(mFrame, mPaint)
            canvas.drawRect(mClipFrame, mPaint)
        }
    }

    fun onDrawMosaicsPath(canvas: Canvas): Int {
        val layerCount = canvas.saveLayer(mFrame, null)
        if (!isMosaicEmpty) {
            canvas.withSave {
                val scale: Float = getScale()
                translate(mFrame.left, mFrame.top)
                scale(scale, scale)
                for (path in mMosaics) {
                    path.onDrawMosaic(this, mPaint)
                }
            }
        }
        return layerCount
    }

    fun onDrawMosaic(canvas: Canvas, layerCount: Int) {
        mMosaicImage?.let { canvas.drawBitmap(it, null, mFrame, mMosaicPaint) }
        canvas.restoreToCount(layerCount)
    }

    fun onDrawDoodles(canvas: Canvas) {
        if (!isDoodleEmpty) {
            canvas.withSave {
                val scale: Float = getScale()
                translate(mFrame.left, mFrame.top)
                scale(scale, scale)
                for (path in mDoodles) {
                    path.onDrawDoodle(this, mPaint)
                }
            }
        }
    }

    fun onDrawStickerClip(canvas: Canvas) {
        M.setRotate(getRotate(), mClipFrame.centerX(), mClipFrame.centerY())
        M.mapRect(mTempClipFrame, if (mClipWin.isClipping) mFrame else mClipFrame)
        canvas.clipRect(mTempClipFrame)
    }

    fun onDrawStickers(canvas: Canvas) {
        if (mBackStickers.isEmpty()) return
        canvas.withSave {
            for (sticker in mBackStickers) {
                if (!sticker.isShowing()) {
                    val tPivotX = sticker.getX() + sticker.getPivotX()
                    val tPivotY = sticker.getY() + sticker.getPivotY()
                    withSave {
                        M.setTranslate(sticker.getX(), sticker.getY())
                        M.postScale(sticker.getScale(), sticker.getScale(), tPivotX, tPivotY)
                        M.postRotate(sticker.getRotation(), tPivotX, tPivotY)
                        concat(M)
                        sticker.onSticker(this)
                    }
                }
            }
        }
    }

    fun onDrawShade(canvas: Canvas) {
        if (mMode === IMGMode.CLIP && isSteady) {
            mShade.reset()
            mShade.addRect(
                mFrame.left - 2,
                mFrame.top - 2,
                mFrame.right + 2,
                mFrame.bottom + 2,
                Path.Direction.CW
            )
            mShade.addRect(mClipFrame, Path.Direction.CCW)
            mShadePaint?.let { canvas.drawPath(mShade, it) }
        }
    }

    fun onDrawClip(canvas: Canvas?) {
        if (mMode === IMGMode.CLIP) {
            if (canvas != null) {
                mClipWin.onDraw(canvas)
            }
        }
    }

    fun onTouchDown(x: Float, y: Float) {
        isSteady = false
        moveToBackground(mForeSticker)
        if (mMode === IMGMode.CLIP) {
            mAnchor = mClipWin.getAnchor(x, y)
        }
    }

    fun onTouchUp() {
        if (mAnchor != null) {
            mAnchor = null
        }
    }

    fun onSteady() {
        isSteady = true
        onClipHoming()
        mClipWin.isShowShade = true
    }

    fun onScaleBegin() {}
    fun onScroll(scrollX: Float, scrollY: Float, dx: Float, dy: Float): IMGHoming? {
        if (mMode === IMGMode.CLIP) {
            mClipWin.isShowShade = false
            mAnchor?.let {
                mClipWin.onScroll(it, dx, dy)
                val clipFrame = RectF()
                M.setRotate(getRotate(), mClipFrame.centerX(), mClipFrame.centerY())
                M.mapRect(clipFrame, mFrame)
                val frame = mClipWin.getOffsetFrame(scrollX, scrollY)
                val homing = IMGHoming(scrollX, scrollY, getScale(), getTargetRotate())
                homing.rConcat(
                    fillHoming(
                        frame,
                        clipFrame,
                        mClipFrame.centerX(),
                        mClipFrame.centerY()
                    )
                )
                return homing
            }
        }
        return null
    }

    fun getTargetRotate(): Float {
        return mTargetRotate
    }

    fun setTargetRotate(targetRotate: Float) {
        mTargetRotate = targetRotate
    }

    /**
     * 在当前基础上旋转
     */
    fun rotate(rotate: Int) {
        mTargetRotate = (((mRotate + rotate) / 90f).roundToInt() * 90).toFloat()
        mClipWin.reset(mClipFrame, getTargetRotate())
    }

    fun getRotate(): Float {
        return mRotate
    }

    fun setRotate(rotate: Float) {
        mRotate = rotate
    }

    fun getScale(): Float {
        return mFrame.width() / (mImage?.width ?: 1)
    }

    fun setScale(scale: Float) {
        setScale(scale, mClipFrame.centerX(), mClipFrame.centerY())
    }

    fun setScale(scale: Float, focusX: Float, focusY: Float) {
        onScale(scale / getScale(), focusX, focusY)
    }

    fun onScale(factorF: Float, focusX: Float, focusY: Float) {
        var factor = factorF
        if (factor == 1f) return
        if (max(mClipFrame.width(), mClipFrame.height()) >= MAX_SIZE
            || min(mClipFrame.width(), mClipFrame.height()) <= MIN_SIZE
        ) {
            factor += (1 - factor) / 2
        }
        M.setScale(factor, factor, focusX, focusY)
        M.mapRect(mFrame)
        M.mapRect(mClipFrame)
        for (sticker in mBackStickers) {
            M.mapRect(sticker.getFrame())
            val tPivotX = sticker.getX() + sticker.getPivotX()
            val tPivotY = sticker.getY() + sticker.getPivotY()
            sticker.addScale(factor)
            sticker.setX(sticker.getX() + sticker.getFrame().centerX() - tPivotX)
            sticker.setY(sticker.getY() + sticker.getFrame().centerY() - tPivotY)
        }
    }

    fun onScaleEnd() {}
    fun onHomingStart() {
        isAnimCanceled = false
        isDrawClip = true
    }

    fun onHoming(fraction: Float) {
        mClipWin.homing(fraction)
    }

    fun onHomingEnd(): Boolean {
        isDrawClip = true
        if (mMode === IMGMode.CLIP) {
            // 开启裁剪模式
            val clip = !isAnimCanceled
            mClipWin.isHoming = false
            mClipWin.isClipping = true
            mClipWin.isResetting = false
            return clip
        } else {
            if (isFreezing && !isAnimCanceled) {
                setFreezing(false)
            }
        }
        return false
    }

    fun isFreezing(): Boolean {
        return isFreezing
    }

    private fun setFreezing(freezing: Boolean) {
        if (freezing != isFreezing) {
            rotateStickers(if (freezing) -getRotate() else getTargetRotate())
            isFreezing = freezing
        }
    }

    fun onHomingCancel() {
        isAnimCanceled = true
        Log.d(TAG, "Homing cancel")
    }

    fun release() {
        if (mImage?.isRecycled == false) {
            mImage?.recycle()
        }
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        DEFAULT_IMAGE.recycle()
    }

    companion object {
        private const val TAG = "IMGImage"
        private const val MIN_SIZE = 500
        private const val MAX_SIZE = 10000
        private const val DEBUG = false
        private const val COLOR_SHADE = -0x34000000
    }
}
