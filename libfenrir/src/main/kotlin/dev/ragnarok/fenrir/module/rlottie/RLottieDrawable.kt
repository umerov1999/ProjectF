package dev.ragnarok.fenrir.module.rlottie

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RawRes
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.BuildConfig
import dev.ragnarok.fenrir.module.DispatchQueuePool
import dev.ragnarok.fenrir.module.FenrirNative.appContext
import dev.ragnarok.fenrir.module.FenrirNative.density
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import kotlin.math.abs
import kotlin.math.ceil

class RLottieDrawable : Drawable, Animatable {
    private external fun create(
        src: String?,
        params: IntArray?,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ): Long

    private external fun createWithJson(
        json: Long,
        params: IntArray?,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ): Long

    private external fun destroy(ptr: Long)

    private external fun setLayerColor(ptr: Long, layer: String, color: Int)
    private external fun replaceColors(ptr: Long, colorReplacement: IntArray)
    private external fun getFrame(
        ptr: Long,
        frame: Int,
        bitmap: Bitmap?,
        w: Int,
        h: Int,
        stride: Int,
        clear: Boolean
    ): Int

    private val metaData = IntArray(2)
    private val newColorUpdates = HashMap<String, Int>()
    private val pendingColorUpdates = HashMap<String, Int>()
    private val dstRect = Rect()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var width: Int = 0
    private var height: Int = 0
    private var timeBetweenFrames: Int = 0
    var customEndFrame = -1
        private set
    private var playInDirectionOfCustomEndFrame = false
    private var onFinishCallback: WeakReference<Runnable>? = null
    private var autoRepeat = 1
    private var autoRepeatPlayCount = 0

    @Volatile
    private var nextFrameIsLast = false
    private var loadFrameTask: Runnable? = null

    @Volatile
    var renderingBitmap: Bitmap? = null
        private set

    @Volatile
    var nextRenderingBitmap: Bitmap? = null
        private set

    @Volatile
    var backgroundBitmap: Bitmap? = null
        private set
    private var waitingForNextTask = false
    private var frameWaitSync: CountDownLatch? = null
    private var destroyWhenDone = false
    private var currentFrame = 0

    @Volatile
    private var isRunning = false

    @Volatile
    private var isRecycled = false

    @Volatile
    private var nativePtr: Long = 0

    private var loadingInBackground = false
    private var secondLoadingInBackground = false
    private var destroyAfterLoading = false
    private var newReplaceColors: IntArray? = null
    private var pendingReplaceColors: IntArray? = null
    private var vibrationPattern: HashMap<Int, Int>? = null
    private var finishFrame = 0
    private var currentParentView: WeakReference<View?>? = null
    private var lastFrameTime: Long = 0
    private var decodeSingleFrame = false
    private var singleFrameDecoded = false
    private var uiRunnableNoFrame = Runnable {
        loadFrameTask = null
        decodeFrameFinishedInternal()
    }
    private var uiRunnable = Runnable {
        singleFrameDecoded = true
        invalidateInternal()
        decodeFrameFinishedInternal()
    }
    private var forceFrameRedraw = false
    private var applyingLayerColors = false
    private var shouldLimitFps = false
    private var loadFrameRunnable = Runnable {
        if (isRecycled) {
            return@Runnable
        }
        if (nativePtr == 0L) {
            frameWaitSync?.countDown()
            uiHandler.post(uiRunnableNoFrame)
            return@Runnable
        }
        if (backgroundBitmap == null) {
            try {
                backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } catch (e: Throwable) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        }
        backgroundBitmap?.let {
            try {
                if (pendingColorUpdates.isNotEmpty()) {
                    for ((key, value) in pendingColorUpdates) {
                        setLayerColor(nativePtr, key, value)
                    }
                    pendingColorUpdates.clear()
                }
            } catch (ignore: Exception) {
            }
            pendingReplaceColors?.let { pit ->
                replaceColors(nativePtr, pit)
                pendingReplaceColors = null
            }
            try {
                val result = getFrame(
                    nativePtr,
                    currentFrame,
                    it,
                    width,
                    height,
                    it.rowBytes,
                    true
                )
                if (result == -1) {
                    uiHandler.post(uiRunnableNoFrame)
                    frameWaitSync?.countDown()
                    return@Runnable
                }
                nextRenderingBitmap = backgroundBitmap
                val framesPerUpdates = if (shouldLimitFps) 2 else 1
                if (customEndFrame >= 0 && playInDirectionOfCustomEndFrame) {
                    if (currentFrame > customEndFrame) {
                        if (currentFrame - framesPerUpdates >= customEndFrame) {
                            currentFrame -= framesPerUpdates
                            nextFrameIsLast = false
                        } else {
                            nextFrameIsLast = true
                        }
                    } else {
                        if (currentFrame + framesPerUpdates < customEndFrame) {
                            currentFrame += framesPerUpdates
                            nextFrameIsLast = false
                        } else {
                            nextFrameIsLast = true
                        }
                    }
                } else {
                    if (currentFrame + framesPerUpdates < (if (customEndFrame >= 0) customEndFrame else metaData[0])) {
                        if (autoRepeat == 3) {
                            nextFrameIsLast = true
                            autoRepeatPlayCount++
                        } else {
                            currentFrame += framesPerUpdates
                            nextFrameIsLast = false
                        }
                    } else if (autoRepeat == 1) {
                        currentFrame = 0
                        nextFrameIsLast = false
                    } else if (autoRepeat == 2) {
                        currentFrame = 0
                        nextFrameIsLast = true
                        autoRepeatPlayCount++
                    } else {
                        nextFrameIsLast = true
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                } else {
                    return@Runnable
                }
            }
        }
        uiHandler.post(uiRunnable)
        frameWaitSync?.countDown()
    }

    private var scaleX = 1.0f
    private var scaleY = 1.0f
    private var applyTransformation = false
    private var needScale = false
    private var invalidateOnProgressSet = false
    private var isInvalid = false
    private var doNotRemoveInvalidOnFrameReady = false

    constructor(
        file: File,
        canDeleteError: Boolean,
        w: Int,
        h: Int,
        limitFps: Boolean,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ) {
        width = w
        height = h
        shouldLimitFps = limitFps
        nativePtr = create(
            file.absolutePath,
            metaData,
            colorReplacement,
            useMoveColor
        )
        if (nativePtr == 0L && canDeleteError) {
            file.delete()
        }
        if (shouldLimitFps && metaData[1] < 60) {
            shouldLimitFps = false
        }
        timeBetweenFrames =
            if (shouldLimitFps) 33 else 16.coerceAtLeast((1000.0f / metaData[1]).toInt())
    }

    constructor(
        jsonString: BufferWriteNative,
        w: Int,
        h: Int,
        limitFps: Boolean,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ) {
        width = w
        height = h
        shouldLimitFps = limitFps
        nativePtr = createWithJson(jsonString.pointer, metaData, colorReplacement, useMoveColor)
        jsonString.release()
        if (shouldLimitFps && metaData[1] < 60) {
            shouldLimitFps = false
        }
        timeBetweenFrames =
            if (shouldLimitFps) 33 else 16.coerceAtLeast((1000.0f / metaData[1]).toInt())
    }

    constructor(
        @RawRes rawRes: Int,
        w: Int,
        h: Int,
        startDecode: Boolean,
        colorReplacement: IntArray?,
        useMoveColor: Boolean
    ) {
        width = w
        height = h
        autoRepeat = 0
        val jsonString = readRes(rawRes) ?: return
        nativePtr = createWithJson(jsonString.pointer, metaData, colorReplacement, useMoveColor)
        jsonString.release()
        timeBetweenFrames = 16.coerceAtLeast((1000.0f / metaData[1]).toInt())
        if (startDecode) {
            setAllowDecodeSingleFrame(true)
        }
    }

    private fun checkRunningTasks() {
        if (!hasParentView() && nextRenderingBitmap != null && loadFrameTask != null) {
            loadFrameTask = null
            nextRenderingBitmap = null
        }
    }

    private fun decodeFrameFinishedInternal() {
        if (destroyWhenDone) {
            checkRunningTasks()
            if (loadFrameTask == null && nativePtr != 0L) {
                destroy(nativePtr)
                nativePtr = 0
            }
        }
        if (nativePtr == 0L) {
            recycleResources()
            return
        }
        waitingForNextTask = true
        if (!hasParentView()) {
            stop()
        }
        scheduleNextGetFrame()
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
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            nextRenderingBitmap = null
            renderingBitmap = null
            backgroundBitmap = null
        }
        invalidateInternal()
    }

    fun setOnFinishCallback(callback: Runnable?, frame: Int) {
        if (callback != null) {
            onFinishCallback = WeakReference(callback)
            finishFrame = frame
        } else if (onFinishCallback != null) {
            onFinishCallback = null
        }
    }

    fun getCurrentFrame(): Int {
        return currentFrame
    }

    fun setCurrentFrame(frame: Int) {
        setCurrentFrame(frame, true)
    }

    val duration: Long
        get() = (metaData[0] / metaData[1].toFloat() * 1000).toLong()

    fun setPlayInDirectionOfCustomEndFrame(value: Boolean) {
        playInDirectionOfCustomEndFrame = value
    }

    fun setCustomEndFrame(frame: Int): Boolean {
        if (customEndFrame == frame || frame > metaData[0]) {
            return false
        }
        customEndFrame = frame
        return true
    }

    val framesCount: Int
        get() = metaData[0]

    private fun hasParentView(): Boolean {
        return callback != null
    }

    private fun invalidateInternal() {
        if (callback != null) {
            invalidateSelf()
        }
    }

    fun setAllowDecodeSingleFrame(value: Boolean) {
        decodeSingleFrame = value
        if (decodeSingleFrame) {
            scheduleNextGetFrame()
        }
    }

    fun recycle() {
        isRunning = false
        isRecycled = true
        checkRunningTasks()
        if (loadingInBackground || secondLoadingInBackground) {
            destroyAfterLoading = true
        } else if (loadFrameTask == null) {
            if (nativePtr != 0L) {
                destroy(nativePtr)
                nativePtr = 0
            }
            recycleResources()
        } else {
            destroyWhenDone = true
        }
    }

    fun setAutoRepeat(value: Int) {
        if (autoRepeat == 2 && value == 3 && currentFrame != 0) {
            return
        }
        autoRepeat = value
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

    @Deprecated("", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun start() {
        if (isRunning || autoRepeat >= 2 && autoRepeatPlayCount != 0 || !hasParentView()) {
            return
        }
        isRunning = true
        if (invalidateOnProgressSet) {
            isInvalid = true
            if (loadFrameTask != null) {
                doNotRemoveInvalidOnFrameReady = true
            }
        }
        scheduleNextGetFrame()
        invalidateInternal()
    }

    fun restart(): Boolean {
        if (autoRepeat < 2 || autoRepeatPlayCount == 0) {
            return false
        }
        autoRepeatPlayCount = 0
        autoRepeat = 2
        start()
        return true
    }

    fun setVibrationPattern(pattern: HashMap<Int, Int>?) {
        vibrationPattern = pattern
    }

    fun beginApplyLayerColors() {
        applyingLayerColors = true
    }

    fun commitApplyLayerColors() {
        if (!applyingLayerColors) {
            return
        }
        applyingLayerColors = false
        if (!isRunning && decodeSingleFrame) {
            if (currentFrame <= 2) {
                currentFrame = 0
            }
            nextFrameIsLast = false
            singleFrameDecoded = false
            if (!scheduleNextGetFrame()) {
                forceFrameRedraw = true
            }
        }
        invalidateInternal()
    }

    fun replaceColors(colors: IntArray?) {
        newReplaceColors = colors
        requestRedrawColors()
    }

    fun setLayerColor(layerName: String, color: Int) {
        newColorUpdates[layerName] = color
        requestRedrawColors()
    }

    private fun requestRedrawColors() {
        if (!applyingLayerColors && !isRunning && decodeSingleFrame) {
            if (currentFrame <= 2) {
                currentFrame = 0
            }
            nextFrameIsLast = false
            singleFrameDecoded = false
            if (!scheduleNextGetFrame()) {
                forceFrameRedraw = true
            }
        }
        invalidateInternal()
    }

    private fun scheduleNextGetFrame(): Boolean {
        if (loadFrameTask != null || nextRenderingBitmap != null || nativePtr == 0L || loadingInBackground || destroyWhenDone || !isRunning && (!decodeSingleFrame || singleFrameDecoded)) {
            return false
        }
        if (newColorUpdates.isNotEmpty()) {
            pendingColorUpdates.putAll(newColorUpdates)
            newColorUpdates.clear()
        }
        if (newReplaceColors != null) {
            pendingReplaceColors = newReplaceColors
            newReplaceColors = null
        }
        loadFrameRunnableQueue.execute(loadFrameRunnable.also { loadFrameTask = it })
        return true
    }

    override fun stop() {
        isRunning = false
    }

    fun setCurrentFrame(frame: Int, async: Boolean) {
        setCurrentFrame(frame, async, false)
    }

    fun setCurrentFrame(frame: Int, async: Boolean, resetFrame: Boolean) {
        if (frame < 0 || frame > metaData[0] || currentFrame == frame) {
            return
        }
        currentFrame = frame
        nextFrameIsLast = false
        singleFrameDecoded = false
        if (invalidateOnProgressSet) {
            isInvalid = true
            if (loadFrameTask != null) {
                doNotRemoveInvalidOnFrameReady = true
            }
        }
        if ((!async || resetFrame) && waitingForNextTask && nextRenderingBitmap != null) {
            backgroundBitmap = nextRenderingBitmap
            nextRenderingBitmap = null
            loadFrameTask = null
            waitingForNextTask = false
        }
        if (!async) {
            if (loadFrameTask == null) {
                frameWaitSync = CountDownLatch(1)
            }
        }
        if (scheduleNextGetFrame()) {
            if (!async) {
                try {
                    frameWaitSync?.await()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
                frameWaitSync = null
            }
        } else {
            forceFrameRedraw = true
        }
        invalidateSelf()
    }

    fun setProgressMs(ms: Long) {
        val frameNum = (0L.coerceAtLeast(ms) / timeBetweenFrames % metaData[0]).toInt()
        setCurrentFrame(frameNum, async = true, resetFrame = true)
    }

    fun setProgress(progress: Float) {
        setProgress(progress, true)
    }

    fun setProgress(pProgress: Float, async: Boolean) {
        var progress = pProgress
        if (progress < 0.0f) {
            progress = 0.0f
        } else if (progress > 1.0f) {
            progress = 1.0f
        }
        setCurrentFrame((metaData[0] * progress).toInt(), async)
    }

    fun setCurrentParentView(view: View?) {
        currentParentView = WeakReference(view)
        callback = view
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        applyTransformation = true
    }

    private fun setCurrentFrame(now: Long, timeDiff: Long, timeCheck: Long, force: Boolean) {
        backgroundBitmap = renderingBitmap
        renderingBitmap = nextRenderingBitmap
        nextRenderingBitmap = null
        if (nextFrameIsLast) {
            stop()
        }
        loadFrameTask = null
        if (doNotRemoveInvalidOnFrameReady) {
            doNotRemoveInvalidOnFrameReady = false
        } else if (isInvalid) {
            isInvalid = false
        }
        singleFrameDecoded = true
        waitingForNextTask = false
        lastFrameTime =
            if (screenRefreshRate <= 60) {
                now
            } else {
                now - 16L.coerceAtMost(timeDiff - timeCheck)
            }
        if (force && forceFrameRedraw) {
            singleFrameDecoded = false
            forceFrameRedraw = false
        }
        if (onFinishCallback != null && currentFrame >= finishFrame) {
            val runnable = onFinishCallback?.get()
            runnable?.run()
        }
        scheduleNextGetFrame()
    }

    override fun draw(canvas: Canvas) {
        if (nativePtr == 0L || destroyWhenDone) {
            return
        }
        updateCurrentFrame()
        if (!isInvalid) {
            renderingBitmap?.let {
                if (applyTransformation) {
                    dstRect.set(bounds)
                    scaleX = dstRect.width().toFloat() / width
                    scaleY = dstRect.height().toFloat() / height
                    applyTransformation = false
                    needScale =
                        !(abs(dstRect.width() - width) < dp(1f) && abs(dstRect.width() - width) < dp(
                            1f
                        ))
                }
                if (!needScale) {
                    canvas.drawBitmap(
                        it,
                        dstRect.left.toFloat(),
                        dstRect.top.toFloat(),
                        paint
                    )
                } else {
                    canvas.save()
                    canvas.translate(dstRect.left.toFloat(), dstRect.top.toFloat())
                    canvas.scale(scaleX, scaleY)
                    canvas.drawBitmap(it, 0f, 0f, paint)
                    canvas.restore()
                }
                if (isRunning) {
                    invalidateInternal()
                }
            }
        }
    }

    fun updateCurrentFrame() {
        val now = SystemClock.elapsedRealtime()
        val timeDiff = abs(now - lastFrameTime)
        val timeCheck: Int = if (screenRefreshRate <= 60) {
            timeBetweenFrames - 6
        } else {
            timeBetweenFrames
        }
        if (isRunning) {
            if (renderingBitmap == null && nextRenderingBitmap == null) {
                scheduleNextGetFrame()
            } else if (nextRenderingBitmap != null && (renderingBitmap == null || timeDiff >= timeCheck)) {
                if (vibrationPattern != null && currentParentView != null && currentParentView?.get() != null) {
                    val force = vibrationPattern?.get(currentFrame - 1)
                    if (force != null) {
                        currentParentView?.get()?.performHapticFeedback(
                            if (force == 1) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        )
                    }
                }
                setCurrentFrame(now, timeDiff, timeCheck.toLong(), false)
            }
        } else if ((forceFrameRedraw || decodeSingleFrame && timeDiff >= timeCheck) && nextRenderingBitmap != null) {
            setCurrentFrame(now, timeDiff, timeCheck.toLong(), true)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getMinimumHeight(): Int {
        return height
    }

    override fun getMinimumWidth(): Int {
        return width
    }

    val animatedBitmap: Bitmap?
        get() {
            if (renderingBitmap != null) {
                return renderingBitmap
            } else if (nextRenderingBitmap != null) {
                return nextRenderingBitmap
            }
            return null
        }

    fun hasBitmap(): Boolean {
        return nativePtr != 0L && (renderingBitmap != null || nextRenderingBitmap != null) && !isInvalid
    }

    fun setInvalidateOnProgressSet(value: Boolean) {
        invalidateOnProgressSet = value
    }

    companion object {
        private val uiHandler = Handler(Looper.getMainLooper())
        private val loadFrameRunnableQueue = DispatchQueuePool(4)
        private var screenRefreshRate = 60

        fun updateScreenRefreshRate(rate: Int) {
            screenRefreshRate = rate
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
            } catch (e: Throwable) {
                return null
            } finally {
                try {
                    inputStream?.close()
                } catch (ignore: Throwable) {
                }
            }
        }

        internal fun dp(value: Float): Int {
            return if (value == 0f) {
                0
            } else ceil((density * value).toDouble())
                .toInt()
        }
    }
}
