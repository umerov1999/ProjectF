package dev.ragnarok.fenrir.module.animation.thorvg

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RawRes
import androidx.core.graphics.createBitmap
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.BuildConfig
import dev.ragnarok.fenrir.module.FenrirNative.appContext
import dev.ragnarok.fenrir.module.FenrirNative.density
import java.io.File
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ThorVGLottieDrawable : Drawable, Animatable {
    @IntDef(RESTART, REVERSE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RepeatMode

    @IntDef(LoadedFrom.NET, LoadedFrom.NO, LoadedFrom.FILE, LoadedFrom.RES)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadedFrom {
        companion object {
            const val NET = -1
            const val NO = 0
            const val FILE = 1
            const val RES = 3
        }
    }

    private var mLottieState: LottieDrawableState

    private var mListener: LottieAnimationListener? = null

    private var mRunning = false

    private var mEnded = false

    private var mStarted = false

    private var mRepeated = 0

    private var mFrame = 0

    private val mHandler = Handler(Looper.getMainLooper())

    private val mNextFrameRunnable = Runnable { this.invalidateSelf() }

    private val mTmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var mMutated = false

    private var startTime = 0L

    constructor(
        filePath: String,
        canDeleteError: Boolean,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ) {
        mLottieState = LottieDrawableState()
        mLottieState.mLottie =
            Lottie().init(filePath, canDeleteError, colorReplacement, useMoveColor)
        mLottieState.updateFrameInterval()
    }

    constructor(
        @RawRes rawRes: Int,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ) {
        mLottieState = LottieDrawableState()
        mLottieState.mLottie = Lottie().init(rawRes, colorReplacement, useMoveColor)
        mLottieState.updateFrameInterval()
    }

    private constructor(state: LottieDrawableState) {
        mLottieState = state
    }

    fun release() {
        mLottieState.releaseLottie()
    }

    protected fun finalize() {
        try {
            release()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    override fun mutate(): Drawable {
        if (!mMutated && super.mutate() === this) {
            mLottieState = LottieDrawableState(mLottieState)
            mMutated = true
        }
        return this
    }

    override fun draw(canvas: Canvas) {
        if (!mEnded && mLottieState.valid() && mRunning) {
            if (!mStarted) {
                mStarted = true
                startTime = System.nanoTime()
                dispatchAnimationStart()
            }

            if (mRepeated >= mLottieState.mRepeatCount) {
                if (!mEnded) {
                    mEnded = true
                    dispatchAnimationEnd()
                }
                getFrame(if (mLottieState.mFramesPerUpdate >= 0) mLottieState.getLastFrame() else mLottieState.mFirstFrame)?.let {
                    canvas.drawBitmap(it, 0f, 0f, mTmpPaint)
                }
            } else {
                getFrame(mFrame)?.let {
                    canvas.drawBitmap(it, 0f, 0f, mTmpPaint)
                }

                var resetFrame = false
                mFrame += mLottieState.mFramesPerUpdate
                if (mFrame > mLottieState.getLastFrame()) {
                    if (getRepeatMode() == REVERSE) {
                        mLottieState.mFramesPerUpdate = -1
                        mFrame--
                    } else {
                        mFrame = mLottieState.mFirstFrame
                    }
                    resetFrame = true
                } else if (mFrame < mLottieState.mFirstFrame) {
                    if (getRepeatMode() == REVERSE) {
                        mLottieState.mFramesPerUpdate = 1
                        mFrame++
                    } else {
                        mFrame = mLottieState.getLastFrame()
                    }
                    resetFrame = true
                }
                if (resetFrame) {
                    mRepeated++
                    dispatchAnimationRepeat()
                }
                val endTime = System.nanoTime()
                mHandler.postDelayed(
                    mNextFrameRunnable, mLottieState.mFrameInterval
                            - ((endTime - startTime) / 1000000)
                )
                startTime = System.nanoTime()
            }
        } else if (mLottieState.valid()) {
            getFrame(mFrame)?.let {
                canvas.drawBitmap(it, 0f, 0f, mTmpPaint)
            }
        }
    }

    fun getFrame(frame: Int): Bitmap? {
        return mLottieState.getLottieBuffer(frame)
    }

    override fun setAlpha(alpha: Int) {
        mTmpPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mTmpPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return mLottieState.mLottie?.mWidth ?: 0
    }

    override fun getIntrinsicHeight(): Int {
        return mLottieState.mLottie?.mHeight ?: 0
    }

    fun setRepeatCount(count: Int) {
        mLottieState.mRepeatCount = count
        mRepeated = 0
    }

    fun getRepeatCount(): Int {
        return mLottieState.mRepeatCount
    }

    fun setRepeatMode(@RepeatMode mode: Int) {
        mLottieState.setRepeatMode(mode)
    }

    @RepeatMode
    fun getRepeatMode(): Int {
        return mLottieState.mRepeatMode
    }

    fun setFirstFrame(frame: Int) {
        mLottieState.setFirstFrame(frame)
    }

    fun getFirstFrame(): Int {
        return mLottieState.mFirstFrame
    }

    fun setLastFrame(frame: Int) {
        mLottieState.setLastFrame(frame)
    }

    fun getLastFrame(): Int {
        return mLottieState.getLastFrame()
    }

    fun getDuration(): Long {
        return mLottieState.mLottie?.mDuration ?: 0
    }

    fun getAnimationHeight(): Int {
        return mLottieState.mLottie?.mHeight ?: 0
    }

    fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
        mLottieState.setSpeed(speed)
    }

    @FloatRange(from = 0.0)
    fun getSpeed(): Float {
        return mLottieState.mSpeed
    }

    fun setSize(width: Int, height: Int) {
        require(width > 0) { "LottieDrawable requires width > 0" }
        require(height > 0) { "LottieDrawable requires height > 0" }
        mLottieState.setLottieSize(width, height)
    }

    override fun isRunning(): Boolean {
        return mRunning
    }

    override fun start() {
        mRunning = true
        mEnded = false
        mStarted = false
        mRepeated = 0
        mFrame = mLottieState.mFirstFrame
        invalidateSelf()
    }

    override fun stop() {
        mRunning = false
        mHandler.removeCallbacks(mNextFrameRunnable)
    }

    fun pause() {
        mRunning = false
        mHandler.removeCallbacks(mNextFrameRunnable)
    }

    fun resume() {
        startTime = System.nanoTime()
        mRunning = true
        invalidateSelf()
    }

    fun setAnimationListener(listener: LottieAnimationListener?) {
        mListener = listener
    }

    fun dispatchAnimationStart() {
        mListener?.onAnimationStart()
    }

    fun dispatchAnimationRepeat() {
        mListener?.onAnimationRepeat()
    }

    fun dispatchAnimationEnd() {
        mListener?.onAnimationEnd()
    }

    inner class LottieDrawableState : ConstantState {
        var mLottie: Lottie? = null

        var mRepeatMode = RESTART

        var mRepeatCount = 1

        var mSpeed = 1.0f

        var mFirstFrame = 0
        private var mLastFrame = -1

        var mFrameInterval: Long = 0

        var mFramesPerUpdate = 1

        fun getLastFrame(): Int {
            if (mLastFrame < 0) {
                return mLottie?.mFrameCount ?: 0
            }
            return mLastFrame
        }

        constructor(copy: LottieDrawableState?) {
            if (copy != null) {
                mLottie = Lottie(copy.mLottie)
                mRepeatCount = copy.mRepeatCount
                mRepeatMode = copy.mRepeatMode
                mFramesPerUpdate = copy.mFramesPerUpdate
                mSpeed = copy.mSpeed
                updateFrameInterval()
            }
        }

        fun releaseLottie() {
            mLottie?.destroy()
            mLottie = null
        }

        fun valid(): Boolean {
            mLottie?.let {
                return it.mNativePtr != 0L
            }
            return false
        }

        fun setLottieSize(width: Int, height: Int) {
            mLottie?.let {
                if (width != it.mWidth || height != it.mHeight || it.mBuffer == null) {
                    it.mWidth = width
                    it.mHeight = height
                    mLottie?.setBufferSize(width, height)
                    invalidateSelf()
                }
            }
        }

        fun getLottieBuffer(frame: Int): Bitmap? {
            return mLottie?.getBuffer(frame)
        }

        fun setRepeatMode(@RepeatMode mode: Int) {
            mRepeatMode = mode
        }

        fun setSpeed(@FloatRange(from = 0.0) speed: Float) {
            mSpeed = speed
            updateFrameInterval()
        }

        fun setFirstFrame(frame: Int) {
            mFirstFrame = min(frame, getLastFrame())
            updateFrameInterval()
        }

        fun setLastFrame(frame: Int) {
            mLastFrame = min(frame, mLottie?.mFrameCount ?: 0)
            updateFrameInterval()
        }

        fun updateFrameInterval() {
            val frameCount = getLastFrame() - mFirstFrame
            mFrameInterval = 16

            val duration = mLottie?.mDuration?.toDouble()
            if (frameCount <= 0 || duration == null || duration <= 0) {
                return
            }
            mFrameInterval = (duration / max(frameCount.toDouble(), 1.0)).toLong()
        }

        internal constructor()

        override fun newDrawable(): Drawable {
            return ThorVGLottieDrawable(this)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    inner class Lottie {
        internal var mNativePtr: Long = 0
        internal var mFrameCount = 0
        internal var mDuration: Long = 0
        internal var mWidth: Int = 0
        internal var mHeight: Int = 0
        internal var mBuffer: Bitmap? = null
        private var colorReplacementTmp: IntArray? = null
        private var useMoveColorTmp: Boolean = false

        @LoadedFrom
        private var loadedFrom: Int = LoadedFrom.NO
        private var filePathTmp: String? = null

        @RawRes
        private var rawResTmp: Int? = null

        constructor()

        internal constructor(copy: Lottie?) {
            if (copy == null || copy.loadedFrom == LoadedFrom.NO) {
                mNativePtr = 0
                loadedFrom = LoadedFrom.NO
                return
            }
            filePathTmp = copy.filePathTmp
            rawResTmp = copy.rawResTmp
            loadedFrom = copy.loadedFrom
            useMoveColorTmp = copy.useMoveColorTmp
            colorReplacementTmp = copy.colorReplacementTmp
            when (copy.loadedFrom) {
                LoadedFrom.RES -> {
                    rawResTmp?.let {
                        init(it, colorReplacementTmp, useMoveColorTmp)
                    }
                }

                LoadedFrom.FILE -> {
                    filePathTmp?.let {
                        init(it, false, colorReplacementTmp, useMoveColorTmp)
                    }
                }
            }
            if (mWidth != copy.mWidth || mHeight != copy.mHeight) {
                mWidth = copy.mWidth
                mHeight = copy.mHeight
            }
            if (copy.mBuffer != null) {
                setBufferSize(mWidth, mHeight)
            }
        }

        fun init(
            filePath: String,
            canDeleteError: Boolean,
            colorReplacement: IntArray?,
            useMoveColor: Boolean
        ): Lottie {
            mNativePtr = 0
            val file = File(filePath)
            if (!file.exists()) {
                return this
            }
            val outValues = IntArray(LOTTIE_INFO_COUNT)
            mNativePtr = nLoadFromFile(filePath, outValues, colorReplacement, useMoveColor)
            if (mNativePtr == 0L) {
                if (canDeleteError) {
                    file.delete()
                }
                return this
            }
            loadedFrom = LoadedFrom.FILE
            filePathTmp = file.absolutePath
            colorReplacementTmp = colorReplacement
            useMoveColorTmp = useMoveColor
            mFrameCount = outValues[LOTTIE_INFO_FRAME_COUNT]
            mDuration = outValues[LOTTIE_INFO_DURATION] * 1000L

            mWidth = dp(outValues[LOTTIE_INFO_WIDTH])
            mHeight = dp(outValues[LOTTIE_INFO_HEIGHT])
            return this
        }

        fun init(
            @RawRes rawRes: Int,
            colorReplacement: IntArray?,
            useMoveColor: Boolean
        ): Lottie {
            mNativePtr = 0
            val jsonString = readRes(rawRes) ?: return this

            val outValues = IntArray(LOTTIE_INFO_COUNT)
            mNativePtr =
                nLoadFromMemory(jsonString.pointer, outValues, colorReplacement, useMoveColor)
            if (mNativePtr == 0L) {
                return this
            }
            loadedFrom = LoadedFrom.RES
            rawResTmp = rawRes
            colorReplacementTmp = colorReplacement
            useMoveColorTmp = useMoveColor
            mFrameCount = outValues[LOTTIE_INFO_FRAME_COUNT]
            mDuration = outValues[LOTTIE_INFO_DURATION] * 1000L

            mWidth = dp(outValues[LOTTIE_INFO_WIDTH])
            mHeight = dp(outValues[LOTTIE_INFO_HEIGHT])
            return this
        }

        fun destroy() {
            if (mBuffer != null) {
                mBuffer?.recycle()
                mBuffer = null
            }
            nDestroy(mNativePtr)
        }

        fun setBufferSize(width: Int, height: Int) {
            mBuffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mBuffer?.let { nSetBufferSize(mNativePtr, it, width.toFloat(), height.toFloat()) }
        }

        fun getBuffer(frame: Int): Bitmap? {
            mBuffer?.let { nGetFrame(mNativePtr, it, frame) }
            return mBuffer
        }
    }

    interface LottieAnimationListener {
        fun onAnimationStart()
        fun onAnimationEnd()
        fun onAnimationRepeat()
    }

    companion object {
        const val RESTART: Int = 1
        const val REVERSE: Int = 2
        private const val LOTTIE_INFO_FRAME_COUNT = 0
        private const val LOTTIE_INFO_DURATION = 1
        private const val LOTTIE_INFO_WIDTH = 2
        private const val LOTTIE_INFO_HEIGHT = 3
        private const val LOTTIE_INFO_COUNT = 4

        internal fun dp(value: Int): Int {
            return if (value == 0) {
                0
            } else ceil((density * value.toFloat()).toDouble())
                .toInt()
        }

        internal fun readRes(@RawRes rawRes: Int): BufferWriteNative? {
            var inputStream: InputStream? = null
            return try {
                inputStream = appContext.resources.openRawResource(rawRes)
                val res = BufferWriteNative.fromStreamEndlessNull(inputStream)
                if (res.bufferSize() <= 0) {
                    inputStream.close()
                    return null
                }
                res
            } catch (_: Throwable) {
                return null
            } finally {
                try {
                    inputStream?.close()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private external fun nLoadFromFile(
        src: String?,
        params: IntArray?,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ): Long

    private external fun nLoadFromMemory(
        json: Long,
        params: IntArray?,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ): Long

    private external fun nSetBufferSize(
        ptr: Long,
        bitmap: Bitmap,
        width: Float,
        height: Float
    )

    private external fun nGetFrame(ptr: Long, bitmap: Bitmap, frame: Int)
    private external fun nDestroy(ptr: Long)
}
