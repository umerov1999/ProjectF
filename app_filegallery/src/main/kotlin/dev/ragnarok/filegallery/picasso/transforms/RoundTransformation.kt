package dev.ragnarok.filegallery.picasso.transforms

import android.graphics.Bitmap
import com.squareup.picasso3.RequestHandler
import com.squareup.picasso3.Transformation
import dev.ragnarok.filegallery.picasso.transforms.ImageHelper.getRoundedBitmap

class RoundTransformation : Transformation {
    override fun key(): String {
        return "$TAG()"
    }

    override fun localTransform(source: Bitmap?): Bitmap? {
        return if (source == null) {
            null
        } else getRoundedBitmap(source)
    }

    override fun transform(source: RequestHandler.Result.Bitmap): RequestHandler.Result.Bitmap {
        return RequestHandler.Result.Bitmap(
            getRoundedBitmap(source.bitmap)!!,
            source.loadedFrom,
            source.exifRotation
        )
    }

    companion object {
        private val TAG = RoundTransformation::class.java.simpleName
    }
}