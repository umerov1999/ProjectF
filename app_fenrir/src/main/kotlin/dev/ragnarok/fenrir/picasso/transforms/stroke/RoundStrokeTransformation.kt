package dev.ragnarok.fenrir.picasso.transforms.stroke

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.picasso.transforms.stroke.ImageWithStrokeHelper.getRoundedBitmap

class RoundStrokeTransformation(@ColorInt private val strokeFirst: Int) : Transformation {
    override fun key(): String {
        return "${TAG}($strokeFirst)"
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else getRoundedBitmap(strokeFirst, source)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            getRoundedBitmap(strokeFirst, source.bitmap)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = RoundStrokeTransformation::class.java.simpleName
    }
}