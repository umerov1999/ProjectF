package dev.ragnarok.fenrir.module.rlottie

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.Keep
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.DispatchQueuePool
import java.io.File

class RLottie2Gif internal constructor(private val builder: Builder) {

    private external fun lottie2gif(
        ptr: Long,
        bitmap: Bitmap,
        w: Int,
        h: Int,
        stride: Int,
        bgColor: Int,
        transparent: Boolean,
        gifPath: String,
        bitDepth: Int,
        dither: Boolean,
        listener: Lottie2GifListener
    ): Boolean

    var isRunning = false
        private set
    var isSuccessful = false
        private set
    var currentFrame = 0
        private set
    var totalFrame = 0
        private set
    private val listener: Lottie2GifListener = object : Lottie2GifListener {
        override fun onStarted() {
            isRunning = true
            builder.listener?.onStarted()
        }

        override fun onProgress(frame: Int, totalFrame: Int) {
            currentFrame = frame
            this@RLottie2Gif.totalFrame = totalFrame
            builder.listener?.onProgress(frame, totalFrame)
        }

        override fun onFinished() {
            isRunning = false
            builder.listener?.onFinished()
        }
    }
    var converter: Runnable = Runnable {
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(builder.w, builder.h, Bitmap.Config.ARGB_8888)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        bitmap?.let {
            isSuccessful = lottie2gif(
                builder.jsonString.pointer,
                it,
                builder.w,
                builder.h,
                it.rowBytes,
                builder.bgColor,
                builder.bgColor == Color.TRANSPARENT,
                builder.gifPath.absolutePath,
                builder.bitDepth,
                builder.dither,
                listener
            )
        } ?: run { isSuccessful = false }
    }

    fun buildAgain(): Boolean {
        if (isRunning) return false
        build()
        return isSuccessful
    }

    private fun build() {
        if (builder.async) {
            runnableQueue?.execute(converter)
        } else {
            converter.run()
        }
    }

    fun getBuilder(): Builder {
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RLottie2Gif) return false
        return other.builder.gifPath == builder.gifPath
    }

    override fun toString(): String {
        return builder.gifPath.absolutePath
    }

    override fun hashCode(): Int {
        return builder.hashCode()
    }

    @Keep
    interface Lottie2GifListener {
        fun onStarted()
        fun onProgress(frame: Int, totalFrame: Int)
        fun onFinished()
    }

    class Builder(var jsonString: BufferWriteNative) {
        var w = 256
        var h = 256
        var bgColor = Color.WHITE
        var listener: Lottie2GifListener? = null
        lateinit var gifPath: File
        var async = true
        var bitDepth = 8
        var dither = false
        var cancelable = false

        /**
         * set the output gif background color
         */
        fun setBackgroundColor(bgColor: Int): Builder {
            this.bgColor = bgColor
            return this
        }

        /**
         * set the output gif width and height
         */
        fun setSize(width: Int, height: Int): Builder {
            w = width
            h = height
            return this
        }

        fun setListener(listener: Lottie2GifListener?): Builder {
            this.listener = listener
            return this
        }

        /**
         * set the output gif path
         */
        fun setOutputPath(gif: File): Builder {
            gifPath = gif
            return this
        }

        /**
         * set the output gif path
         */
        fun setOutputPath(gif: String): Builder {
            gifPath = File(gif)
            return this
        }

        fun setBackgroundTask(enabled: Boolean): Builder {
            async = enabled
            return this
        }

        /**
         * Implements Floyd-Steinberg dithering, writes palette value to alpha
         */
        fun setDithering(enabled: Boolean): Builder {
            dither = enabled
            return this
        }

        fun setBitDepth(bit: Int): Builder {
            bitDepth = bit
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun build(): RLottie2Gif {
            if (w <= 0 || h <= 0) {
                throw RuntimeException("output gif width and height must be > 0")
            }
            return RLottie2Gif(this)
        }
    }

    companion object {
        private var runnableQueue: DispatchQueuePool? = null

        fun create(jsonString: BufferWriteNative): Builder {
            return Builder(jsonString)
        }
    }

    init {
        if (runnableQueue == null) runnableQueue = DispatchQueuePool(2)
        build()
    }
}