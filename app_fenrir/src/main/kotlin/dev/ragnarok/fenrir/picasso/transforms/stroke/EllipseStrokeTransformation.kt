package dev.ragnarok.fenrir.picasso.transforms.stroke

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.picasso.transforms.stroke.ImageWithStrokeHelper.getEllipseBitmap

class EllipseStrokeTransformation(@ColorInt private val strokeFirst: Int) : Transformation {
    override fun key(): String {
        return "$TAG($strokeFirst)"
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else getEllipseBitmap(strokeFirst, source, 0.35f)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            getEllipseBitmap(strokeFirst, source.bitmap, 0.35f)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = EllipseStrokeTransformation::class.java.simpleName
    }
}