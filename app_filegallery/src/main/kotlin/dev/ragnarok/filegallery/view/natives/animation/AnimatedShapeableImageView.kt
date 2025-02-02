package dev.ragnarok.filegallery.view.natives.animation

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
import dev.ragnarok.fenrir.module.animation.AnimatedFileDrawable
import dev.ragnarok.fenrir.module.animation.AnimatedFileDrawable.LoadedFrom
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.util.Utils
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
    private var defaultWidth: Int = 100
    private var defaultHeight: Int = 100
    private var animatedDrawable: AnimatedFileDrawable? = null
    private var attachedToWindow = false
    private var decoderCallback: OnDecoderInit? = null
    private var mDisposable = CancelableJob()

    @LoadedFrom
    private var loadedFrom = LoadedFrom.NO
    private var filePathTmp: String? = null
    private var keyTmp: String? = null

    @RawRes
    private var rawResTmp: Int? = null
    private var isPlaying: Boolean? = null
    private var tmpFade: Boolean? = null

    fun setDecoderCallback(decoderCallback: OnDecoderInit?) {
        this.decoderCallback = decoderCallback
    }

    private fun setAnimationByUrlCache(key: String, autoPlay: Boolean, fade: Boolean) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        val ch = cache.fetch(key)
        if (ch == null) {
            decoderCallback?.onLoaded(false)
            return
        }
        if (filePathTmp == ch.absolutePath && loadedFrom == LoadedFrom.FILE) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = false)
        loadedFrom = LoadedFrom.FILE
        filePathTmp = ch.absolutePath
        tmpFade = fade
        isPlaying = autoPlay

        if (attachedToWindow) {
            createAnimationDrawable()
        }
    }

    private fun setAnimationByResCache(@RawRes res: Int, autoPlay: Boolean, fade: Boolean) {
        if (!FenrirNative.isNativeLoaded) {
            decoderCallback?.onLoaded(false)
            return
        }
        val ch = cache.fetch(res)
        if (ch == null) {
            decoderCallback?.onLoaded(false)
            return
        }
        if (filePathTmp == ch.absolutePath && loadedFrom == LoadedFrom.FILE) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = false)
        loadedFrom = LoadedFrom.FILE
        filePathTmp = ch.absolutePath
        tmpFade = fade
        isPlaying = autoPlay

        if (attachedToWindow) {
            createAnimationDrawable()
        }
    }

    fun fromNet(key: String, url: String?, client: OkHttpClient.Builder, autoPlay: Boolean) {
        if (!FenrirNative.isNativeLoaded || url.isNullOrEmpty()) {
            if (loadedFrom == LoadedFrom.NET) {
                loadedFrom = LoadedFrom.NO
            }
            decoderCallback?.onLoaded(false)
            return
        }
        if (filePathTmp == url && keyTmp == key && loadedFrom == LoadedFrom.NET) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
        loadedFrom = LoadedFrom.NET
        filePathTmp = url
        keyTmp = key
        isPlaying = autoPlay
        tmpFade = true

        if (cache.isCachedFile(key)) {
            setAnimationByUrlCache(key, autoPlay, true)
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
                setAnimationByUrlCache(key, autoPlay, true)
            } else {
                decoderCallback?.onLoaded(false)
            }
        }, {
            decoderCallback?.onLoaded(false)
        }))
    }

    fun fromRes(@RawRes resId: Int, autoPlay: Boolean) {
        if (!FenrirNative.isNativeLoaded || resId == -1) {
            if (loadedFrom == LoadedFrom.RES) {
                loadedFrom = LoadedFrom.NO
            }
            decoderCallback?.onLoaded(false)
            return
        }
        if (rawResTmp == resId && loadedFrom == LoadedFrom.RES) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
        loadedFrom = LoadedFrom.RES
        rawResTmp = resId
        tmpFade = true
        isPlaying = autoPlay

        if (cache.isCachedRes(resId)) {
            setAnimationByResCache(resId, autoPlay, true)
            return
        }
        mDisposable.set(flow {
            try {
                if (!copyRes(resId)) {
                    emit(false)
                    return@flow
                }
                cache.renameTempFile(resId)
            } catch (_: Exception) {
                emit(false)
                return@flow
            }
            emit(true)
        }.fromIOToMain {
            if (it) {
                setAnimationByResCache(resId, autoPlay, true)
            } else {
                decoderCallback?.onLoaded(false)
            }
        })
    }

    private fun createAnimationDrawable() {
        if (FenrirNative.isNativeLoaded && attachedToWindow && loadedFrom != LoadedFrom.NO && animatedDrawable == null && filePathTmp != null) {
            animatedDrawable = AnimatedFileDrawable(
                filePathTmp ?: "",
                0,
                defaultWidth,
                defaultHeight,
                tmpFade == true,
                object : AnimatedFileDrawable.DecoderListener {
                    override fun onError() {
                        decoderCallback?.onLoaded(false)
                    }

                })
            decoderCallback?.onLoaded(animatedDrawable?.isDecoded == true)
            if (animatedDrawable?.isDecoded != true) {
                clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
                return
            }
            tmpFade = false
            animatedDrawable?.setAllowDecodeSingleFrame(true)
            super.setImageDrawable(animatedDrawable)
            if (isPlaying == true) {
                playAnimation()
            }
        }
    }

    fun fromFile(file: File) {
        if (!FenrirNative.isNativeLoaded || !file.exists()) {
            decoderCallback?.onLoaded(false)
            return
        }
        if (filePathTmp == file.absolutePath && loadedFrom == LoadedFrom.FILE) {
            return
        }
        clearAnimationDrawable(callSuper = true, clearState = true, cancelTask = true)
        loadedFrom = LoadedFrom.FILE
        filePathTmp = file.absolutePath
        tmpFade = false
        if (attachedToWindow) {
            createAnimationDrawable()
        }
    }

    fun clearAnimationDrawable(callSuper: Boolean, clearState: Boolean, cancelTask: Boolean) {
        if (cancelTask) {
            mDisposable.cancel()
        }
        if (animatedDrawable != null) {
            animatedDrawable?.callback = null
            animatedDrawable?.recycle()
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
            tmpFade = null
        }
    }

    override fun onAttachedToWindow() {
        attachedToWindow = true
        super.onAttachedToWindow()
        when {
            loadedFrom == LoadedFrom.NET -> {
                filePathTmp?.let {
                    keyTmp?.let { s ->
                        fromNet(
                            s,
                            it,
                            Utils.createOkHttp(Constants.PICASSO_TIMEOUT),
                            isPlaying == true,
                        )
                    }
                    return
                }
                clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = false)
            }

            loadedFrom == LoadedFrom.RES -> {
                rawResTmp?.let {
                    fromRes(
                        it,
                        isPlaying == true,
                    )
                    return
                }
                clearAnimationDrawable(callSuper = false, clearState = true, cancelTask = false)
            }

            loadedFrom != LoadedFrom.NO -> {
                createAnimationDrawable()
            }
        }
    }

    override fun onDetachedFromWindow() {
        attachedToWindow = false
        super.onDetachedFromWindow()
        if (loadedFrom != LoadedFrom.NO) {
            clearAnimationDrawable(callSuper = true, clearState = false, cancelTask = true)
        }
    }

    fun isPlaying(): Boolean {
        return animatedDrawable != null && animatedDrawable?.isRunning == true
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

    fun playAnimation() {
        animatedDrawable?.start()
        isPlaying = true
    }

    fun resetFrame() {
        animatedDrawable?.seekTo(0, true)
    }

    fun stopAnimation() {
        animatedDrawable?.let {
            it.stop()
            isPlaying = false
        }
    }

    protected override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    protected override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
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
        context.withStyledAttributes(attrs, R.styleable.AnimatedShapeableImageView) {
            defaultWidth =
                getDimension(R.styleable.AnimatedShapeableImageView_default_width, 100f).toInt()
            defaultHeight =
                getDimension(R.styleable.AnimatedShapeableImageView_default_height, 100f).toInt()
        }
    }
}
