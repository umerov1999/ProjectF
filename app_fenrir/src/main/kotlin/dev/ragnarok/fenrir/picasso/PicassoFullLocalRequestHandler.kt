package dev.ragnarok.fenrir.picasso

import android.content.Context
import android.net.Uri
import com.squareup.picasso3.BitmapUtils.decodeStream
import com.squareup.picasso3.Picasso
import com.squareup.picasso3.Request
import com.squareup.picasso3.RequestHandler
import com.yalantis.ucrop.util.BitmapLoadUtils.getExifOrientation
import okio.source
import java.io.IOException

class PicassoFullLocalRequestHandler(val context: Context) : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri!!.path != null && data.uri!!.lastPathSegment != null && data.uri!!.scheme != null && data.uri!!.scheme?.contains(
            "full_"
        ) == true
    }

    override fun load(picasso: Picasso, request: Request, callback: Callback) {
        val requestUri = Uri.parse(
            checkNotNull(request.uri) { "request.uri == null" }.toString()
                .replace("full_", "")
        )
        try {
            val exifOrientation = getExifOrientation(context, requestUri)
            val i = context.contentResolver.openInputStream(requestUri) ?: return
            val bitmap = decodeStream(i.source(), request)
            i.close()
            callback.onSuccess(Result.Bitmap(bitmap, Picasso.LoadedFrom.DISK, exifOrientation))
        } catch (e: IOException) {
            callback.onError(e)
        }
    }
}