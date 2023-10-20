package me.minetsh.imaging.core.clip

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import me.minetsh.imaging.core.clip.IMGClip.Companion.CLIP_CORNERS
import me.minetsh.imaging.core.clip.IMGClip.Companion.CLIP_CORNER_SIZES
import me.minetsh.imaging.core.clip.IMGClip.Companion.CLIP_CORNER_STEPS
import me.minetsh.imaging.core.clip.IMGClip.Companion.CLIP_SIZE_RATIO
import me.minetsh.imaging.core.util.IMGUtils
import kotlin.experimental.and
import kotlin.math.abs

/**
 * Created by felix on 2017/11/29 下午5:41.
 */
class IMGClipWindow : IMGClip {
    /**
     * 裁剪区域
     */
    val frame = RectF()
    private val mBaseFrame = RectF()
    val targetFrame = RectF()

    /**
     * 裁剪窗口
     */
    private val winFrame = RectF()
    private val mWin = RectF()
    private val mCells = FloatArray(16)
    private val mCorners = FloatArray(32)
    private val mBaseSizes = Array(2) { FloatArray(4) }
    private val M = Matrix()
    private val mShadePath = Path()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 是否在裁剪中
     */
    var isClipping = false

    var isResetting = true

    var isShowShade = false

    var isHoming = false

    init {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.SQUARE
    }

    /**
     * 计算裁剪窗口区域
     */
    fun setClipWinSize(width: Float, height: Float) {
        mWin[0f, 0f, width] = height
        winFrame[0f, 0f, width] = height * VERTICAL_RATIO
        if (!frame.isEmpty) {
            IMGUtils.center(winFrame, frame)
            targetFrame.set(frame)
        }
    }

    fun reset(clipImage: RectF, rotate: Float) {
        val imgRect = RectF()
        M.setRotate(rotate, clipImage.centerX(), clipImage.centerY())
        M.mapRect(imgRect, clipImage)
        reset(imgRect.width(), imgRect.height())
    }

    /**
     * 重置裁剪
     */
    private fun reset(clipWidth: Float, clipHeight: Float) {
        isResetting = true
        frame[0f, 0f, clipWidth] = clipHeight
        IMGUtils.fitCenter(winFrame, frame, IMGClip.CLIP_MARGIN)
        targetFrame.set(frame)
    }

    fun homing(): Boolean {
        mBaseFrame.set(frame)
        targetFrame.set(frame)
        IMGUtils.fitCenter(winFrame, targetFrame, IMGClip.CLIP_MARGIN)
        return !(targetFrame == mBaseFrame).also { isHoming = it }
    }

    fun homing(fraction: Float) {
        if (isHoming) {
            frame[mBaseFrame.left + (targetFrame.left - mBaseFrame.left) * fraction, mBaseFrame.top + (targetFrame.top - mBaseFrame.top) * fraction, mBaseFrame.right + (targetFrame.right - mBaseFrame.right) * fraction] =
                mBaseFrame.bottom + (targetFrame.bottom - mBaseFrame.bottom) * fraction
        }
    }

    fun getOffsetFrame(offsetX: Float, offsetY: Float): RectF {
        val frame = RectF(frame)
        frame.offset(offsetX, offsetY)
        return frame
    }

    fun getOffsetTargetFrame(offsetX: Float, offsetY: Float): RectF {
        val targetFrame = RectF(frame)
        targetFrame.offset(offsetX, offsetY)
        return targetFrame
    }

    fun onDraw(canvas: Canvas) {
        if (isResetting) {
            return
        }
        val size = floatArrayOf(frame.width(), frame.height())
        for (i in mBaseSizes.indices) {
            for (j in mBaseSizes[i].indices) {
                mBaseSizes[i][j] = size[i] * CLIP_SIZE_RATIO[j]
            }
        }
        for (i in mCells.indices) {
            mCells[i] = mBaseSizes[i and 1][IMGClip.CLIP_CELL_STRIDES ushr (i shl 1) and 3]
        }
        for (i in mCorners.indices) {
            mCorners[i] = (mBaseSizes[i and 1][IMGClip.CLIP_CORNER_STRIDES ushr i and 1]
                    + CLIP_CORNER_SIZES[(CLIP_CORNERS[i] and 3).toInt()] + CLIP_CORNER_STEPS[CLIP_CORNERS[i].toInt()
                .shr(2)])
        }
        canvas.translate(frame.left, frame.top)
        mPaint.style = Paint.Style.STROKE
        mPaint.color = COLOR_CELL
        mPaint.strokeWidth = IMGClip.CLIP_THICKNESS_CELL
        canvas.drawLines(mCells, mPaint)
        canvas.translate(-frame.left, -frame.top)
        mPaint.color = COLOR_FRAME
        mPaint.strokeWidth = IMGClip.CLIP_THICKNESS_FRAME
        canvas.drawRect(frame, mPaint)
        canvas.translate(frame.left, frame.top)
        mPaint.color = COLOR_CORNER
        mPaint.strokeWidth = IMGClip.CLIP_THICKNESS_SEWING
        canvas.drawLines(mCorners, mPaint)
    }

    fun onDrawShade(canvas: Canvas) {
        if (!isShowShade) return

        // 计算遮罩图形
        mShadePath.reset()
        mShadePath.fillType = Path.FillType.WINDING
        mShadePath.addRect(
            frame.left + 100,
            frame.top + 100,
            frame.right - 100,
            frame.bottom - 100,
            Path.Direction.CW
        )
        mPaint.color = COLOR_SHADE
        mPaint.style = Paint.Style.FILL
        canvas.drawPath(mShadePath, mPaint)
    }

    fun getAnchor(x: Float, y: Float): IMGClip.Anchor? {
        if (IMGClip.Anchor.isCohesionContains(frame, -IMGClip.CLIP_CORNER_SIZE, x, y)
            && !IMGClip.Anchor.isCohesionContains(frame, IMGClip.CLIP_CORNER_SIZE, x, y)
        ) {
            var v = 0
            val cohesion = IMGClip.Anchor.cohesion(
                frame, 0f
            )
            val pos = floatArrayOf(x, y)
            for (i in cohesion.indices) {
                if (abs(cohesion[i] - pos[i shr 1]) < IMGClip.CLIP_CORNER_SIZE) {
                    v = v or (1 shl i)
                }
            }
            val anchor = IMGClip.Anchor.valueOf(v)
            if (anchor != null) {
                isHoming = false
            }
            return anchor
        }
        return null
    }

    fun onScroll(anchor: IMGClip.Anchor, dx: Float, dy: Float) {
        anchor.move(winFrame, frame, dx, dy)
    }

    companion object {
        /**
         * 垂直窗口比例
         */
        private const val VERTICAL_RATIO = 0.8f
        private const val COLOR_CELL = -0x7f000001
        private const val COLOR_FRAME = Color.WHITE
        private const val COLOR_CORNER = Color.WHITE
        private const val COLOR_SHADE = -0x34000000
    }
}
