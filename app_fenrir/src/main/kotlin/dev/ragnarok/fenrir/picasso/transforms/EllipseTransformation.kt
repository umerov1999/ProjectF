package dev.ragnarok.fenrir.picasso.transforms

import android.graphics.Bitmap
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.picasso.transforms.ImageHelper.getEllipseBitmap

class EllipseTransformation : Transformation {
    override fun key(): String {
        return "$TAG()"
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else getEllipseBitmap(source, 0.35f)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            getEllipseBitmap(source.bitmap, 0.35f)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = EllipseTransformation::class.simpleName.orEmpty()
    }
}