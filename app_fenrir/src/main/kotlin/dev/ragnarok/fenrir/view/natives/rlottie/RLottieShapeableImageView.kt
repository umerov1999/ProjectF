package dev.ragnarok.fenrir.view.natives.rlottie

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.RawRes
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.module.BufferWriteNative
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.rlottie.RLottieDrawable
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class RLottieShapeableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ShapeableImageView(context, attrs) {
    private val cache: RLottieNetworkCache = RLottieNetworkCache(context)
    private var layerColors: HashMap<String, Int>? = null
    private var animatedDrawable: RLottieDrawable? = null
    private var autoRepeat: Boolean
    private var attachedToWindow = false
    private var playing = false
    private var mDisposable = CancelableJob()
    fun clearLayerColors() {
        layerColors?.clear()
    }

    fun setLayerColor(layer: String, color: Int) {
        if (layerColors == null) {
            layerColors = HashMap()
        }
        (layerColors ?: return)[layer] = color
        animatedDrawable?.setLayerColor(layer, color)
    }

    fun replaceColors(colors: IntArray?) {
        animatedDrawable?.replaceColors(colors)
    }

    private fun setAnimationByUrlCache(url: String, w: Int, h: Int) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        val ch = cache.fetch(url)
        if (ch == null) {
            setImageDrawable(null)
            return
        }
        autoRepeat = false
        setAnimation(
            RLottieDrawable(
                ch, true, w, h,
                limitFps = false,
                colorReplacement = null,
                useMoveColor = false
            )
        )
        playAnimation()
    }

    fun fromNet(url: String?, client: OkHttpClient.Builder, w: Int, h: Int) {
        if (!FenrirNative.isNativeLoaded || url.isNullOrEmpty()) {
            return
        }
        clearAnimationDrawable()
        if (cache.isCachedFile(url)) {
            setAnimationByUrlCache(url, w, h)
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
                setAnimationByUrlCache(url, w, h)
            }
        })
    }

    private fun setAnimation(rLottieDrawable: RLottieDrawable) {
        animatedDrawable = rLottieDrawable
        animatedDrawable?.setAutoRepeat(if (autoRepeat) 1 else 0)
        if (layerColors != null) {
            animatedDrawable?.beginApplyLayerColors()
            for ((key, value) in layerColors ?: return) {
                animatedDrawable?.setLayerColor(key, value)
            }
            animatedDrawable?.commitApplyLayerColors()
        }
        animatedDrawable?.setAllowDecodeSingleFrame(true)
        animatedDrawable?.setCurrentParentView(this)
        setImageDrawable(animatedDrawable)
    }

    @JvmOverloads
    fun fromRes(
        @RawRes resId: Int,
        w: Int,
        h: Int,
        colorReplacement: IntArray? = null,
        useMoveColor: Boolean = false
    ) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        clearAnimationDrawable()
        setAnimation(
            RLottieDrawable(
                resId,
                w,
                h,
                false,
                colorReplacement,
                useMoveColor
            )
        )
    }

    fun fromFile(file: File, w: Int, h: Int) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        clearAnimationDrawable()
        setAnimation(
            RLottieDrawable(
                file, false, w, h,
                limitFps = false,
                colorReplacement = null,
                useMoveColor = false
            )
        )
    }

    fun fromString(jsonString: BufferWriteNative, w: Int, h: Int) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        clearAnimationDrawable()
        setAnimation(
            RLottieDrawable(
                jsonString, w, h,
                limitFps = false,
                colorReplacement = null,
                useMoveColor = false
            )
        )
    }

    fun clearAnimationDrawable() {
        mDisposable.cancel()
        animatedDrawable?.let {
            it.stop()
            it.callback = null
            it.recycle()
            animatedDrawable = null
        }
        setImageDrawable(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        animatedDrawable?.setCurrentParentView(this)
        if (playing) {
            animatedDrawable?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDisposable.cancel()
        attachedToWindow = false
        animatedDrawable?.stop()
        animatedDrawable?.setCurrentParentView(null)
    }

    fun isPlaying(): Boolean {
        return animatedDrawable != null && animatedDrawable?.isRunning == true
    }

    fun setAutoRepeat(repeat: Boolean) {
        autoRepeat = repeat
    }

    fun setProgress(progress: Float) {
        animatedDrawable?.setProgress(progress)
    }

    override fun setImageDrawable(dr: Drawable?) {
        super.setImageDrawable(dr)
        if (dr !is RLottieDrawable) {
            mDisposable.cancel()
            animatedDrawable?.let {
                it.stop()
                it.callback = null
                it.recycle()
                animatedDrawable = null
            }
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        mDisposable.cancel()
        animatedDrawable?.let {
            it.stop()
            it.callback = null
            it.recycle()
            animatedDrawable = null
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        mDisposable.cancel()
        animatedDrawable?.let {
            it.stop()
            it.callback = null
            it.recycle()
            animatedDrawable = null
        }
    }

    fun playAnimation() {
        playing = true
        if (attachedToWindow) {
            animatedDrawable?.start()
        }
    }

    fun replayAnimation() {
        if (animatedDrawable == null) {
            return
        }
        playing = true
        if (attachedToWindow) {
            animatedDrawable?.stop()
            animatedDrawable?.setAutoRepeat(1)
            animatedDrawable?.start()
        }
    }

    fun resetFrame() {
        playing = true
        if (attachedToWindow) {
            animatedDrawable?.setProgress(0f)
        }
    }

    fun stopAnimation() {
        playing = false
        if (attachedToWindow) {
            animatedDrawable?.stop()
        }
    }

    init {
        @SuppressLint("CustomViewStyleable") val a =
            context.obtainStyledAttributes(attrs, R.styleable.RLottieImageView)
        val animRes = a.getResourceId(R.styleable.RLottieImageView_fromRes, 0)
        autoRepeat = a.getBoolean(R.styleable.RLottieImageView_loopAnimation, false)
        val width = a.getDimension(R.styleable.RLottieImageView_w, 28f).toInt()
        val height = a.getDimension(R.styleable.RLottieImageView_h, 28f).toInt()
        a.recycle()
        if (FenrirNative.isNativeLoaded && animRes != 0) {
            setAnimation(RLottieDrawable(animRes, width, height, false, null, false))
            playAnimation()
        }
    }
}
