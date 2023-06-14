package dev.ragnarok.fenrir.picasso

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.squareup.picasso3.BitmapUtils.decodeStream
import com.squareup.picasso3.Picasso
import com.squareup.picasso3.Request
import com.squareup.picasso3.RequestHandler
import okio.source
import java.io.FileNotFoundException
import java.io.IOException

class PicassoFullLocalRequestHandler(val context: Context) : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri!!.path != null && data.uri!!.lastPathSegment != null && data.uri!!.scheme != null && data.uri!!.scheme?.contains(
            "full_"
        ) == true
    }

    private fun getExifOrientation(uri: Uri): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return 0
        }
        val contentResolver = context.contentResolver
        contentResolver.openInputStream(uri)?.use { input ->
            return ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: throw FileNotFoundException("can't open input stream, uri: $uri")
    }

    override fun load(picasso: Picasso, request: Request, callback: Callback) {
        val requestUri = Uri.parse(
            checkNotNull(request.uri) { "request.uri == null" }.toString()
                .replace("full_", "")
        )
        try {
            val i = context.contentResolver.openInputStream(requestUri) ?: return
            val bitmap = decodeStream(i.source(), request)
            i.close()
            val exifOrientation = getExifOrientation(requestUri)
            callback.onSuccess(Result.Bitmap(bitmap, Picasso.LoadedFrom.DISK, exifOrientation))
        } catch (e: IOException) {
            callback.onError(e)
        }
    }
}
