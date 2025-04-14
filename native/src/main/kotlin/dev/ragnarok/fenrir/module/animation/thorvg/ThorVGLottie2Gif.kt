package dev.ragnarok.fenrir.module.animation.thorvg

import android.graphics.Color
import androidx.annotation.Keep
import dev.ragnarok.fenrir.module.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.io.File

class ThorVGLottie2Gif internal constructor(private val builder: Builder) {
    private external fun lottie2gif(
        filePath: String,
        w: Int,
        h: Int,
        bgColor: Int,
        transparent: Boolean,
        fps: Int,
        gifPath: String,
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

    private fun doConvert() {
        isSuccessful = lottie2gif(
            builder.file.absolutePath,
            builder.w,
            builder.h,
            builder.bgColor,
            builder.bgColor == Color.TRANSPARENT,
            builder.fps,
            builder.gifPath.absolutePath,
            object : Lottie2GifListener {
                override fun onStarted() {
                    inMainThread {
                        isRunning = true
                        builder.listener?.onStarted()
                    }
                }

                override fun onProgress(frame: Int, totalFrame: Int) {
                    inMainThread {
                        currentFrame = frame
                        this@ThorVGLottie2Gif.totalFrame = totalFrame
                        builder.listener?.onProgress(frame, totalFrame)
                    }
                }

                override fun onFinished() {
                    inMainThread {
                        isRunning = false
                        builder.listener?.onFinished()
                    }
                }
            }
        )
    }

    fun buildAgain(): Boolean {
        if (isRunning) return false
        build()
        return isSuccessful
    }

    private fun build() {
        if (builder.async) {
            CoroutineScope(coroutineDispatcher).launch {
                doConvert()
            }
        } else {
            doConvert()
        }
    }

    fun getBuilder(): Builder {
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThorVGLottie2Gif) return false
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

    class Builder(var file: File) {
        var w = 256
        var h = 256
        var fps = 30
        var bgColor = Color.TRANSPARENT
        var listener: Lottie2GifListener? = null
        lateinit var gifPath: File
        var async = true
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

        fun setFPS(fps: Int): Builder {
            this.fps = fps
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun build(): ThorVGLottie2Gif {
            if (w <= 0 || h <= 0) {
                throw RuntimeException("output gif width and height must be > 0")
            }
            return ThorVGLottie2Gif(this)
        }
    }

    companion object {
        private val coroutineDispatcher = newFixedThreadPoolContext(2, "convertingQueue$this")

        private val coroutineExceptionHandlerEmpty = CoroutineExceptionHandler { _, throwable ->
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace()
            }
        }

        internal inline fun inMainThread(crossinline function: () -> Unit): Job {
            return CoroutineScope(Dispatchers.Main + coroutineExceptionHandlerEmpty).launch {
                function.invoke()
            }
        }

        fun create(file: File): Builder {
            return Builder(file)
        }
    }

    init {
        build()
    }
}
