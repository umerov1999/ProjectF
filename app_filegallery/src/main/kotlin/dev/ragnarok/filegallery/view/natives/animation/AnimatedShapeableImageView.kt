package dev.ragnarok.filegallery.view.natives.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.RawRes
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.animation.AnimatedFileDrawable
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.view.natives.animation.AnimationNetworkCache.Companion.filenameForRes
import dev.ragnarok.filegallery.view.natives.animation.AnimationNetworkCache.Companion.parentResDir
import kotlinx.coroutines.flow.flow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

open class AnimatedShapeableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ShapeableImageView(context, attrs) {
    private val cache: AnimationNetworkCache = AnimationNetworkCache(context)
    private val defaultWidth: Int
    private val defaultHeight: Int
    private var animatedDrawable: AnimatedFileDrawable? = null
    private var attachedToWindow = false
    private var playing = false
    private var decoderCallback: OnDecoderInit? = null
    private var mDisposable = CancelableJob()
    fun setDecoderCallback(decoderCallback: OnDecoderInit?) {
        this.decoderCallback = decoderCallback
    }

    private fun setAnimationByUrlCache(url: String, fade: Boolean) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        val ch = cache.fetch(url)
        if (ch == null) {
            setImageDrawable(null)
            decoderCallback?.onLoaded(false)
            return
        }
        setAnimation(
            AnimatedFileDrawable(
                ch,
                0,
                defaultWidth,
                defaultHeight,
                fade,
                object : AnimatedFileDrawable.DecoderListener {
                    override fun onError() {
                        decoderCallback?.onLoaded(false)
                    }

                })
        )
        playAnimation()
    }

    private fun setAnimationByResCache(@RawRes res: Int, fade: Boolean) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        val ch = cache.fetch(res)
        if (ch == null) {
            setImageDrawable(null)
            decoderCallback?.onLoaded(false)
            return
        }
        setAnimation(
            AnimatedFileDrawable(
                ch,
                0,
                defaultWidth,
                defaultHeight,
                fade,
                object : AnimatedFileDrawable.DecoderListener {
                    override fun onError() {
                        decoderCallback?.onLoaded(false)
                    }

                })
        )
        playAnimation()
    }

    fun fromNet(key: String, url: String?, client: OkHttpClient.Builder) {
        if (!FenrirNative.isNativeLoaded || url.isNullOrEmpty()) {
            decoderCallback?.onLoaded(false)
            return
        }
        clearAnimationDrawable()
        if (cache.isCachedFile(key)) {
            setAnimationByUrlCache(key, true)
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
                cache.writeTempCacheFile(key, response.body.source())
                response.close()
                cache.renameTempFile(key)
                emit(true)
            } catch (e: CancellationException) {
                call?.cancel()
                throw e
            }
        }.fromIOToMain({ u ->
            if (u) {
                setAnimationByUrlCache(key, true)
            } else {
                decoderCallback?.onLoaded(false)
            }
        }, {
            decoderCallback?.onLoaded(false)
        }))
    }

    fun fromRes(@RawRes res: Int) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        clearAnimationDrawable()
        if (cache.isCachedRes(res)) {
            setAnimationByResCache(res, true)
            return
        }
        mDisposable.set(flow {
            try {
                if (!copyRes(res)) {
                    emit(false)
                    return@flow
                }
                cache.renameTempFile(res)
            } catch (e: Exception) {
                emit(false)
                return@flow
            }
            emit(true)
        }.fromIOToMain {
            if (it) {
                setAnimationByResCache(res, true)
            } else {
                decoderCallback?.onLoaded(false)
            }
        })
    }

    private fun setAnimation(videoDrawable: AnimatedFileDrawable) {
        decoderCallback?.onLoaded(videoDrawable.isDecoded)
        if (!videoDrawable.isDecoded) return
        animatedDrawable = videoDrawable
        animatedDrawable?.setAllowDecodeSingleFrame(true)
        setImageDrawable(animatedDrawable)
    }

    fun fromFile(file: File) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        clearAnimationDrawable()
        setAnimation(
            AnimatedFileDrawable(
                file,
                0,
                defaultWidth,
                defaultHeight,
                false,
                object : AnimatedFileDrawable.DecoderListener {
                    override fun onError() {
                        decoderCallback?.onLoaded(false)
                    }

                })
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
        animatedDrawable?.callback = this
        if (playing) {
            animatedDrawable?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDisposable.cancel()
        attachedToWindow = false
        animatedDrawable?.stop()
        animatedDrawable?.callback = null
    }

    fun isPlaying(): Boolean {
        return animatedDrawable != null && animatedDrawable?.isRunning == true
    }

    override fun setImageDrawable(dr: Drawable?) {
        super.setImageDrawable(dr)
        if (dr !is AnimatedFileDrawable) {
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
        if (animatedDrawable == null) {
            return
        }
        playing = true
        if (attachedToWindow) {
            animatedDrawable?.start()
        }
    }

    fun resetFrame() {
        if (animatedDrawable == null) {
            return
        }
        playing = true
        if (attachedToWindow) {
            animatedDrawable?.seekTo(0, true)
        }
    }

    fun stopAnimation() {
        if (animatedDrawable == null) {
            return
        }
        playing = false
        if (attachedToWindow) {
            animatedDrawable?.stop()
        }
    }

    private fun copyRes(@RawRes rawRes: Int): Boolean {
        try {
            context.resources.openRawResource(rawRes).use { inputStream ->
                val out = File(
                    parentResDir(
                        context
                    ), filenameForRes(rawRes, true)
                )
                val o = FileOutputStream(out)
                var buffer = bufferLocal.get()
                if (buffer == null) {
                    buffer = ByteArray(4096)
                    bufferLocal.set(buffer)
                }
                while (inputStream.read(buffer, 0, buffer.size) >= 0) {
                    o.write(buffer)
                }
                o.flush()
                o.close()
            }
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
            return false
        }
        return true
    }

    interface OnDecoderInit {
        fun onLoaded(success: Boolean)
    }

    companion object {
        private val bufferLocal = ThreadLocal<ByteArray>()
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AnimatedShapeableImageView)
        defaultWidth =
            a.getDimension(R.styleable.AnimatedShapeableImageView_default_width, 100f).toInt()
        defaultHeight =
            a.getDimension(R.styleable.AnimatedShapeableImageView_default_height, 100f).toInt()
        a.recycle()
    }
}
