package dev.ragnarok.fenrir.module.animation

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.Keep
import dev.ragnarok.fenrir.module.BuildConfig
import dev.ragnarok.fenrir.module.DispatchQueue
import java.io.File
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class AnimatedFileDrawable(
    file: File,
    seekTo: Long,
    private val defaultWidth: Int,
    private val defaultHeight: Int,
    private var fadeAnimating: Boolean,
    decoderListener: DecoderListener
) : Drawable(), Animatable {

    private external fun createDecoder(src: String, params: IntArray): Long
    private external fun destroyDecoder(ptr: Long)
    private external fun stopDecoder(ptr: Long)
    private external fun getVideoFrame(
        ptr: Long,
        bitmap: Bitmap?,
        params: IntArray,
        stride: Int,
        preview: Boolean,
        startTimeSeconds: Float,
        endTimeSeconds: Float
    ): Int

    private external fun seekToMs(ptr: Long, ms: Long, precise: Boolean)
    private external fun getFrameAtTime(
        ptr: Long,
        ms: Long,
        bitmap: Bitmap?,
        data: IntArray,
        stride: Int
    ): Int

    private external fun prepareToSeek(ptr: Long)


    private val metaData = IntArray(5)
    private val decoderCreated: Boolean
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val sync = Any()
    private val actualDrawRect = RectF()
    private val dstRect = Rect()
    private val scaleFactor = 1f
    private val mStartTask = Runnable { invalidateInternal() }

    @Volatile
    var nativePtr: Long = 0
    private var lastFrameTime: Long = 0
    private var lastTimeStamp = 0
    private var invalidateAfter = 50
    private var loadFrameTask: Runnable? = null
    private var renderingBitmap: Bitmap? = null
    private var renderingBitmapTime = 0
    private var nextRenderingBitmap: Bitmap? = null
    private var nextRenderingBitmapTime = 0
    var backgroundBitmap: Bitmap? = null
        private set
    private var backgroundBitmapTime = 0
    private var destroyWhenDone = false
    private var decodeSingleFrame = false
    private var singleFrameDecoded = false
    private var forceDecodeAfterNextFrame = false

    @Volatile
    private var pendingSeekTo: Long = -1

    @Volatile
    private var pendingSeekToUI: Long = -1
    private var lastFrameDecodeTime: Long = 0
    private var scaleX = 1.0f
    private var scaleY = 1.0f
    private var applyTransformation = false

    @Volatile
    private var isRunning = false

    @Volatile
    var isRecycled = false
        private set
    private var decodeQueue: DispatchQueue? = null
    private var startTime = 0f
    private var endTime = 0f
    private var startTimeMillis: Long = 0
    private var useSharedQueue = true
    private val uiRunnableNoFrame = Runnable {
        if (destroyWhenDone && nativePtr != 0L) {
            destroyDecoder(nativePtr)
            nativePtr = 0
        }
        if (nativePtr == 0L) {
            recycleResources()
            return@Runnable
        }
        loadFrameTask = null
        scheduleNextGetFrame()
    }
    private val uiRunnable = Runnable {
        if (destroyWhenDone && nativePtr != 0L) {
            destroyDecoder(nativePtr)
            nativePtr = 0
        }
        if (nativePtr == 0L) {
            recycleResources()
            return@Runnable
        }
        if (!forceDecodeAfterNextFrame) {
            singleFrameDecoded = true
        } else {
            forceDecodeAfterNextFrame = false
        }
        loadFrameTask = null
        nextRenderingBitmap = backgroundBitmap
        nextRenderingBitmapTime = backgroundBitmapTime
        if (metaData[3] < lastTimeStamp) {
            lastTimeStamp = if (startTime > 0) (startTime * 1000).toInt() else 0
        }
        if (metaData[3] - lastTimeStamp != 0) {
            invalidateAfter = metaData[3] - lastTimeStamp
        }
        if (pendingSeekToUI >= 0 && pendingSeekTo == -1L) {
            pendingSeekToUI = -1
            invalidateAfter = 0
        }
        lastTimeStamp = metaData[3]
        invalidateInternal()
        scheduleNextGetFrame()
    }
    private val loadFrameRunnable = Runnable {
        if (!isRecycled) {
            try {
                if (nativePtr != 0L || metaData[0] == 0 || metaData[1] == 0) {
                    if (backgroundBitmap == null && metaData[0] > 0 && metaData[1] > 0) {
                        try {
                            backgroundBitmap = Bitmap.createBitmap(
                                (metaData[0] * scaleFactor).toInt(),
                                (metaData[1] * scaleFactor).toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                        } catch (e: Throwable) {
                            if (BuildConfig.DEBUG) {
                                e.printStackTrace()
                            }
                        }
                    }
                    var seekWas = false
                    if (pendingSeekTo >= 0) {
                        metaData[3] = pendingSeekTo.toInt()
                        val pSeekTo = pendingSeekTo
                        synchronized(sync) { pendingSeekTo = -1 }
                        seekWas = true
                        seekToMs(nativePtr, pSeekTo, true)
                    }
                    backgroundBitmap?.let {
                        lastFrameDecodeTime = System.currentTimeMillis()
                        if (getVideoFrame(
                                nativePtr,
                                backgroundBitmap,
                                metaData,
                                it.rowBytes,
                                false,
                                startTime,
                                endTime
                            ) == 0
                        ) {
                            DispatchQueue.runOnUIThread(uiRunnableNoFrame)
                            return@Runnable
                        }
                        if (seekWas) {
                            lastTimeStamp = metaData[3]
                        }
                        backgroundBitmapTime = metaData[3]
                    }
                } else {
                    DispatchQueue.runOnUIThread(uiRunnableNoFrame)
                    return@Runnable
                }
            } catch (e: Throwable) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        }
        DispatchQueue.runOnUIThread(uiRunnable)
    }

    private fun invalidateInternal() {
        if (callback != null) {
            invalidateSelf()
        }
    }

    private fun recycleResources() {
        try {
            if (renderingBitmap != null) {
                renderingBitmap?.recycle()
                renderingBitmap = null
            }
            if (backgroundBitmap != null) {
                backgroundBitmap?.recycle()
                backgroundBitmap = null
            }
            if (nextRenderingBitmap != null) {
                nextRenderingBitmap?.recycle()
                nextRenderingBitmap = null
            }
            if (decodeQueue != null) {
                decodeQueue?.recycle()
                decodeQueue = null
            }
            invalidateInternal()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            renderingBitmap = null
            backgroundBitmap = null
            nextRenderingBitmap = null
            decodeQueue = null
        }
    }

    fun getFrameAtTime(ms: Long): Bitmap? {
        return getFrameAtTime(ms, false)
    }

    private fun getFrameAtTime(ms: Long, precise: Boolean): Bitmap? {
        if (!decoderCreated || nativePtr == 0L) {
            return null
        }
        if (!precise) {
            seekToMs(nativePtr, ms, precise)
        }
        if (backgroundBitmap == null) {
            backgroundBitmap = Bitmap.createBitmap(
                (metaData[0] * scaleFactor).toInt(),
                (metaData[1] * scaleFactor).toInt(),
                Bitmap.Config.ARGB_8888
            )
        }
        backgroundBitmap?.let {
            val result: Int = if (precise) {
                getFrameAtTime(
                    nativePtr,
                    ms,
                    backgroundBitmap,
                    metaData,
                    it.rowBytes
                )
            } else {
                getVideoFrame(
                    nativePtr,
                    backgroundBitmap,
                    metaData,
                    it.rowBytes,
                    true,
                    0f,
                    0f
                )
            }
            return if (result != 0) backgroundBitmap else null
        } ?: return null
    }

    fun setAllowDecodeSingleFrame(value: Boolean) {
        decodeSingleFrame = value
        if (decodeSingleFrame) {
            scheduleNextGetFrame()
        }
    }

    @JvmOverloads
    fun seekTo(ms: Long, force: Boolean = false) {
        synchronized(sync) {
            pendingSeekTo = ms
            pendingSeekToUI = ms
            if (nativePtr != 0L) {
                prepareToSeek(nativePtr)
            }
            if (force && decodeSingleFrame) {
                singleFrameDecoded = false
                if (loadFrameTask == null) {
                    scheduleNextGetFrame()
                } else {
                    forceDecodeAfterNextFrame = true
                }
            }
        }
    }

    fun recycle() {
        isRunning = false
        isRecycled = true
        if (loadFrameTask == null) {
            if (nativePtr != 0L) {
                destroyDecoder(nativePtr)
                nativePtr = 0
            }
            recycleResources()
            paint.shader = null
        } else {
            destroyWhenDone = true
        }
    }

    fun resetStream(stop: Boolean) {
        if (nativePtr != 0L) {
            if (stop) {
                stopDecoder(nativePtr)
            } else {
                prepareToSeek(nativePtr)
            }
        }
    }

    fun setUseSharedQueue(value: Boolean) {
        useSharedQueue = value
    }

    protected fun finalize() {
        try {
            recycle()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    private fun hasParentView(): Boolean {
        return callback != null
    }

    @Deprecated("", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun start() {
        if (isRunning && !hasParentView()) {
            return
        }
        startTimeMillis = SystemClock.uptimeMillis()
        isRunning = true
        scheduleNextGetFrame()
        runOnUiThread(mStartTask)
    }

    val currentProgress: Float
        get() {
            if (metaData[4] == 0) {
                return 0f
            }
            return if (pendingSeekToUI >= 0) {
                pendingSeekToUI / metaData[4].toFloat()
            } else metaData[3] / metaData[4].toFloat()
        }
    private val currentProgressMs: Int
        get() {
            if (pendingSeekToUI >= 0) {
                return pendingSeekToUI.toInt()
            }
            return if (nextRenderingBitmapTime != 0) nextRenderingBitmapTime else renderingBitmapTime
        }
    val durationMs: Int
        get() = metaData[4]

    private fun scheduleNextGetFrame() {
        if (loadFrameTask != null || nativePtr == 0L && decoderCreated || destroyWhenDone || !isRunning && (!decodeSingleFrame || singleFrameDecoded)) {
            return
        }
        var ms: Long = 0
        if (lastFrameDecodeTime != 0L) {
            ms = invalidateAfter.toLong().coerceAtMost(
                0L.coerceAtLeast(invalidateAfter - (System.currentTimeMillis() - lastFrameDecodeTime))
            )
        }
        if (useSharedQueue) {
            executor.schedule(
                loadFrameRunnable.also { loadFrameTask = it },
                ms,
                TimeUnit.MILLISECONDS
            )
        } else {
            if (decodeQueue == null) {
                decodeQueue = DispatchQueue("decodeQueue$this")
            }
            decodeQueue?.postRunnable(loadFrameRunnable.also { loadFrameTask = it }, ms)
        }
    }

    override fun stop() {
        isRunning = false
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun getIntrinsicHeight(): Int {
        var height =
            if (decoderCreated) if (metaData[2] == 90 || metaData[2] == 270) metaData[0] else metaData[1] else 0
        height *= if (height == 0) {
            return defaultHeight
        } else {
            scaleFactor.toInt()
        }
        return height
    }

    override fun getIntrinsicWidth(): Int {
        var width =
            if (decoderCreated) if (metaData[2] == 90 || metaData[2] == 270) metaData[1] else metaData[0] else 0
        width *= if (width == 0) {
            return defaultWidth
        } else {
            scaleFactor.toInt()
        }
        return width
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        applyTransformation = true
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateInternal()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateInternal()
    }

    override fun draw(canvas: Canvas) {
        if (nativePtr == 0L && decoderCreated || destroyWhenDone) {
            return
        }
        val now = System.currentTimeMillis()
        if (isRunning) {
            if (fadeAnimating) {
                var normalized = (SystemClock.uptimeMillis() - startTimeMillis) / 400f
                if (normalized >= 1f) {
                    fadeAnimating = false
                    normalized = 1f
                }
                paint.alpha = (0xFF * normalized).toInt()
            }
            if (renderingBitmap == null && nextRenderingBitmap == null) {
                scheduleNextGetFrame()
            } else if (nextRenderingBitmap != null && (renderingBitmap == null || abs(now - lastFrameTime) >= invalidateAfter)) {
                renderingBitmap = nextRenderingBitmap
                renderingBitmapTime = nextRenderingBitmapTime
                nextRenderingBitmap = null
                nextRenderingBitmapTime = 0
                lastFrameTime = now
            }
        } else if (!isRunning && decodeSingleFrame && abs(now - lastFrameTime) >= invalidateAfter && nextRenderingBitmap != null) {
            renderingBitmap = nextRenderingBitmap
            renderingBitmapTime = nextRenderingBitmapTime
            nextRenderingBitmap = null
            nextRenderingBitmapTime = 0
            lastFrameTime = now
        }
        renderingBitmap?.let {
            if (applyTransformation) {
                var bitmapW = it.width
                var bitmapH = it.height
                if (metaData[2] == 90 || metaData[2] == 270) {
                    val temp = bitmapW
                    bitmapW = bitmapH
                    bitmapH = temp
                }
                dstRect.set(bounds)
                scaleX = dstRect.width().toFloat() / bitmapW
                scaleY = dstRect.height().toFloat() / bitmapH
                applyTransformation = false
            }
            canvas.translate(dstRect.left.toFloat(), dstRect.top.toFloat())
            when {
                metaData[2] == 90 -> {
                    canvas.rotate(90f)
                    canvas.translate(0f, -dstRect.width().toFloat())
                }
                metaData[2] == 180 -> {
                    canvas.rotate(180f)
                    canvas.translate(-dstRect.width().toFloat(), -dstRect.height().toFloat())
                }
                metaData[2] == 270 -> {
                    canvas.rotate(270f)
                    canvas.translate(-dstRect.height().toFloat(), 0f)
                }
            }
            canvas.scale(scaleX, scaleY)
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    override fun getMinimumHeight(): Int {
        val height =
            if (decoderCreated) if (metaData[2] == 90 || metaData[2] == 270) metaData[0] else metaData[1] else 0
        return if (height == 0) {
            defaultHeight
        } else height
    }

    override fun getMinimumWidth(): Int {
        val width =
            if (decoderCreated) if (metaData[2] == 90 || metaData[2] == 270) metaData[1] else metaData[0] else 0
        return if (width == 0) {
            defaultWidth
        } else width
    }

    val isDecoded: Boolean
        get() = decoderCreated && nativePtr != 0L
    val animatedBitmap: Bitmap?
        get() {
            if (renderingBitmap != null) {
                return renderingBitmap
            } else if (nextRenderingBitmap != null) {
                return nextRenderingBitmap
            }
            return null
        }

    fun setActualDrawRect(x: Float, y: Float, width: Float, height: Float) {
        val bottom = y + height
        val right = x + width
        if (actualDrawRect.left != x || actualDrawRect.top != y || actualDrawRect.right != right || actualDrawRect.bottom != bottom) {
            actualDrawRect[x, y, right] = bottom
        }
        invalidateInternal()
    }

    fun hasBitmap(): Boolean {
        return nativePtr != 0L && (renderingBitmap != null || nextRenderingBitmap != null)
    }

    val orientation: Int
        get() = metaData[2]

    fun setStartEndTime(startTime: Long, endTime: Long) {
        this.startTime = startTime / 1000f
        this.endTime = endTime / 1000f
        if (currentProgressMs < startTime) {
            seekTo(startTime, true)
        }
    }

    fun getStartTime(): Long {
        return (startTime * 1000).toLong()
    }

    @Keep
    interface DecoderListener {
        fun onError()
    }

    companion object {
        private val uiHandler = Handler(Looper.getMainLooper())
        private val executor = ScheduledThreadPoolExecutor(8, ThreadPoolExecutor.DiscardPolicy())

        internal fun runOnUiThread(task: Runnable) {
            if (Looper.myLooper() == uiHandler.looper) {
                task.run()
            } else {
                uiHandler.post(task)
            }
        }
    }

    init {
        nativePtr = createDecoder(file.absolutePath, metaData)
        if (nativePtr != 0L && (metaData[0] > 3840 || metaData[1] > 3840)) {
            destroyDecoder(nativePtr)
            nativePtr = 0
        }
        decoderCreated = nativePtr != 0L
        if (decoderCreated && seekTo != 0L) {
            seekTo(seekTo, false)
        }
        if (!decoderCreated) {
            decoderListener.onError()
        }
    }
}
