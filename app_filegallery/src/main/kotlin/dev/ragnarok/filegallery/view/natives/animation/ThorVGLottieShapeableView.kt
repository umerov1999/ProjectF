package dev.ragnarok.filegallery.view.natives.animation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.RawRes
import androidx.core.content.withStyledAttributes
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.animation.LoadedFrom
import dev.ragnarok.fenrir.module.animation.thorvg.ThorVGLottieDrawable
import dev.ragnarok.fenrir.module.animation.thorvg.ThorVGLottieDrawable.Companion.RESTART
import dev.ragnarok.fenrir.module.animation.thorvg.ThorVGLottieDrawable.LottieAnimationListener
import dev.ragnarok.fenrir.module.animation.thorvg.ThorVGLottieDrawable.RepeatMode
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

@SuppressLint("CustomViewStyleable")
class ThorVGLottieShapeableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    ShapeableImageView(context, attrs) {
    private val cache: ThorVGLottieNetworkCache = ThorVGLottieNetworkCache(context)
    private var mDisposable = CancelableJob()
    private var animatedDrawable: ThorVGLottieDrawable? = null
    private var mListener: LottieAnimationListener? = null

    @LoadedFrom
    private var loadedFrom = LoadedFrom.NO
    private var colorReplacementTmp: IntArray? = null
    private var useMoveColorTmp: Boolean = false
    private var deleteInvalidFileTmp: Boolean = false
    private var filePathTmp: String? = null

    @RawRes
    private var rawResTmp: Int? = null
    private var isPlaying: Boolean? = null
    private var repeatTmp: Boolean? = null

    @RepeatMode
    private var repeatModeTmp: Int? = null

    private var mOnAttached = false

    private fun createLottieDrawable() {
        if (FenrirNative.isNativeLoaded && mOnAttached && loadedFrom != LoadedFrom.NO && animatedDrawable == null) {
            when (loadedFrom) {
                LoadedFrom.RES -> {
                    animatedDrawable = rawResTmp?.let {
                        ThorVGLottieDrawable(
                            it,
                            colorReplacementTmp,
                            useMoveColorTmp
                        )
                    }
                }

                LoadedFrom.FILE -> {
                    animatedDrawable = filePathTmp?.let {
                        ThorVGLottieDrawable(
                            it,
                            deleteInvalidFileTmp,
                            colorReplacementTmp,
                            useMoveColorTmp
                        )
                    }
                }
            }
            if (animatedDrawable == null) {
                return
            }
            repeatModeTmp?.let { animatedDrawable?.setRepeatMode(it) }
            repeatTmp?.let { animatedDrawable?.setRepeatCount(if (it) Int.MAX_VALUE else 1) }
            super.setImageDrawable(animatedDrawable)
            animatedDrawable?.callback = this
            animatedDrawable?.setAnimationListener(mListener)
            if (isPlaying == true) {
                startAnimation()
            }
        }
    }

    private fun setAnimationByUrlCache(
        url: String,
        autoPlay: Boolean,
        colorReplacement: IntArray? = null,
        useMoveColor: Boolean = false
    ) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        val ch = cache.fetch(url)
        if (ch == null) {
            return
        }
        if (filePathTmp == ch.absolutePath && deleteInvalidFileTmp == true && colorReplacementTmp == colorReplacement && useMoveColorTmp == useMoveColor && loadedFrom == LoadedFrom.FILE) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = false)
        deleteInvalidFileTmp = true
        colorReplacementTmp = colorReplacement
        useMoveColorTmp = useMoveColor
        loadedFrom = LoadedFrom.FILE
        isPlaying = autoPlay
        filePathTmp = ch.absolutePath
        if (mOnAttached) {
            createLottieDrawable()
        }
    }

    fun fromNet(
        url: String?,
        client: OkHttpClient.Builder,
        autoPlay: Boolean,
        colorReplacement: IntArray? = null,
        useMoveColor: Boolean = false
    ) {
        if (!FenrirNative.isNativeLoaded || url.isNullOrEmpty()) {
            if (loadedFrom == LoadedFrom.NET) {
                loadedFrom = LoadedFrom.NO
            }
            return
        }
        if (filePathTmp == url && colorReplacementTmp == colorReplacement && useMoveColorTmp == useMoveColor && loadedFrom == LoadedFrom.NET) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)

        colorReplacementTmp = colorReplacement
        useMoveColorTmp = useMoveColor
        loadedFrom = LoadedFrom.NET
        filePathTmp = url
        isPlaying = autoPlay

        if (cache.isCachedFile(url)) {
            setAnimationByUrlCache(url, autoPlay, colorReplacement, useMoveColor)
            return
        }
        mDisposable.set(flow {
            var call: Call? = null
            try {
                val request: Request = Request.Builder()
                    .url(url)
                    .build()
                call = client.build().newCall(request)
                val response: Response = call.execute()
                if (!response.isSuccessful) {
                    emit(false)
                    return@flow
                }
                cache.writeTempCacheFile(url, response.body.source())
                response.close()
                cache.renameTempFile(url)
                emit(true)
            } catch (e: CancellationException) {
                call?.cancel()
                throw e
            }
        }.fromIOToMain {
            if (it) {
                setAnimationByUrlCache(url, autoPlay, colorReplacement, useMoveColor)
            }
        })
    }

    fun fromRes(
        @RawRes resId: Int,
        colorReplacement: IntArray? = null,
        useMoveColor: Boolean = false
    ) {
        if (!FenrirNative.isNativeLoaded || resId == -1) {
            return
        }
        if (rawResTmp == resId && colorReplacementTmp == colorReplacement && useMoveColorTmp == useMoveColor && loadedFrom == LoadedFrom.RES) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
        colorReplacementTmp = colorReplacement
        useMoveColorTmp = useMoveColor
        loadedFrom = LoadedFrom.RES
        rawResTmp = resId
        if (mOnAttached) {
            createLottieDrawable()
        }
    }

    fun fromFile(
        file: File,
        deleteInvalidFile: Boolean,
        colorReplacement: IntArray? = null,
        useMoveColor: Boolean = false
    ) {
        if (!FenrirNative.isNativeLoaded || !file.exists()) {
            return
        }
        if (filePathTmp == file.absolutePath && deleteInvalidFileTmp == deleteInvalidFile && colorReplacementTmp == colorReplacement && useMoveColorTmp == useMoveColor && loadedFrom == LoadedFrom.FILE) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
        colorReplacementTmp = colorReplacement
        useMoveColorTmp = useMoveColor
        loadedFrom = LoadedFrom.FILE
        filePathTmp = file.absolutePath
        deleteInvalidFileTmp = deleteInvalidFile
        if (mOnAttached) {
            createLottieDrawable()
        }
    }

    fun isPlaying(): Boolean {
        return animatedDrawable != null && animatedDrawable?.isRunning == true
    }

    fun setRepeat(repeat: Boolean) {
        animatedDrawable?.setRepeatCount(if (repeat) Int.MAX_VALUE else 1)
        repeatTmp = repeat
    }

    fun setRepeatMode(@RepeatMode repeatMode: Int) {
        animatedDrawable?.setRepeatMode(repeatMode)
        repeatModeTmp = repeatMode
    }

    fun startAnimation() {
        animatedDrawable?.start()
        isPlaying = true
    }

    fun stopAnimation() {
        animatedDrawable?.let {
            it.stop()
            isPlaying = false
        }
    }

    fun pauseAnimation() {
        animatedDrawable?.pause()
    }

    fun resumeAnimation() {
        animatedDrawable?.resume()
    }

    fun clearAnimationDrawable(callSuper: Boolean, clearState: Boolean, cancelTask: Boolean) {
        if (cancelTask) {
            mDisposable.cancel()
        }
        if (animatedDrawable != null) {
            animatedDrawable?.callback = null
            animatedDrawable?.release()
            animatedDrawable = null
        }
        if (callSuper) {
            super.setImageDrawable(null)
        }
        if (clearState) {
            isPlaying = false
            loadedFrom = LoadedFrom.NO
            filePathTmp = null
            rawResTmp = null
            deleteInvalidFileTmp = false
        }
    }

    override fun setImageDrawable(dr: Drawable?) {
        super.setImageDrawable(dr)
        clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = true)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = true)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = true)
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth <= 0 || measuredHeight <= 0) {
            return
        }
        animatedDrawable?.setSize(measuredWidth, measuredHeight)
    }

    override fun onAttachedToWindow() {
        mOnAttached = true

        super.onAttachedToWindow()

        if (loadedFrom == LoadedFrom.NET) {
            filePathTmp?.let {
                fromNet(
                    it,
                    Utils.createOkHttp(Constants.PICASSO_TIMEOUT),
                    isPlaying == true,
                    colorReplacementTmp,
                    useMoveColorTmp
                )
                return
            }
            clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = false)
        } else if (loadedFrom != LoadedFrom.NO) {
            createLottieDrawable()
        }
    }

    override fun onDetachedFromWindow() {
        mOnAttached = false
        super.onDetachedFromWindow()
        if (loadedFrom != LoadedFrom.NO) {
            clearAnimationDrawable(callSuper = true, clearState = false, cancelTask = true)
        }
    }

    fun setAnimationListener(listener: LottieAnimationListener?) {
        mListener = listener
    }

    protected override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    protected override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.ThorVGLottieView) {
            rawResTmp = getResourceId(R.styleable.ThorVGLottieView_fromRes, 0)
            if (rawResTmp == 0) {
                rawResTmp = null
            }
            repeatTmp = getBoolean(R.styleable.ThorVGLottieView_loopAnimation, false)
            repeatModeTmp = getInt(R.styleable.ThorVGLottieView_loopMode, RESTART)
        }
        if (FenrirNative.isNativeLoaded && rawResTmp != null) {
            loadedFrom = LoadedFrom.RES
            isPlaying = true
        } else {
            rawResTmp = null
        }
    }
}
