package dev.ragnarok.filegallery.picasso.transforms

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.FloatRange
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation

@Suppress("DEPRECATION")
class BlurTransformation(
    @param:FloatRange(
        from = 0.0,
        to = 25.0
    ) private val mRadius: Float, private val mContext: Context
) : Transformation {
    override fun key(): String {
        return "$TAG(radius=$mRadius)"
    }

    private val TAG = BlurTransformation::class.java.simpleName

    private fun blurRenderScriptDeprecated(
        context: Context?,
        inputBitmap: Bitmap?,
        @FloatRange(from = 0.0, to = 25.0) radius: Float
    ): Bitmap? {
        if (radius <= 0 || inputBitmap == null) {
            return inputBitmap
        }
        val outputBitmap = inputBitmap.copy(inputBitmap.config, true)
        val renderScript = android.renderscript.RenderScript.create(context)
        val blurInput = android.renderscript.Allocation.createFromBitmap(
            renderScript,
            inputBitmap,
            android.renderscript.Allocation.MipmapControl.MIPMAP_NONE,
            android.renderscript.Allocation.USAGE_SCRIPT
        )
        val blurOutput =
            android.renderscript.Allocation.createFromBitmap(renderScript, outputBitmap)
        val blur = android.renderscript.ScriptIntrinsicBlur.create(
            renderScript,
            android.renderscript.Element.U8_4(renderScript)
        )
        blur.setInput(blurInput)
        blur.setRadius(radius) // radius must be 0 < r <= 25
        blur.forEach(blurOutput)
        blurOutput.copyTo(outputBitmap)
        renderScript.destroy()
        return outputBitmap
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        var src = source.bitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RequestHandler.Result.Bitmap(
                BlurTransformationNew.blur(mRadius, src)!!,
                source.loadedFrom,
                source.exifRotation
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && src.config == Bitmap.Config.HARDWARE) {
            val tmpSource = src.copy(Bitmap.Config.ARGB_8888, true)
            src.recycle()
            src = tmpSource
        }
        val bitmap = blurRenderScriptDeprecated(
            mContext, src, mRadius
        )
        if (src != bitmap) {
            src.recycle()
        }
        return RequestHandler.Result.Bitmap(bitmap!!, source.loadedFrom, source.exifRotation)
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        var src = source ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return BlurTransformationNew.blur(mRadius, src)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && src.config == Bitmap.Config.HARDWARE) {
            val tmpSource = src.copy(Bitmap.Config.ARGB_8888, true)
            src.recycle()
            src = tmpSource
        }
        val bitmap = blurRenderScriptDeprecated(
            mContext, src, mRadius
        )
        if (src != bitmap) {
            src.recycle()
        }
        return bitmap
    }
}