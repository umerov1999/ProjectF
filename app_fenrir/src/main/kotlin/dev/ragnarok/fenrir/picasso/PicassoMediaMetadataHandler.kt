package dev.ragnarok.fenrir.picasso

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.squareup.picasso3.Picasso
import com.squareup.picasso3.Request
import com.squareup.picasso3.RequestHandler
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import java.io.ByteArrayInputStream

class PicassoMediaMetadataHandler : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri!!.path != null && data.uri!!.lastPathSegment != null && data.uri!!.scheme != null && data.uri!!.scheme!!
            .contains("share_")
    }

    private fun getMetadataAudioThumbnail(uri: Uri): Bitmap? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        return try {
            mediaMetadataRetriever.setDataSource(provideApplicationContext(), uri)
            val cover = mediaMetadataRetriever.embeddedPicture ?: return null
            BitmapFactory.decodeStream(ByteArrayInputStream(cover))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun load(picasso: Picasso, request: Request, callback: Callback) {
        val target =
            getMetadataAudioThumbnail(Uri.parse(request.uri.toString().replace("share_", "")))
        if (target == null) {
            callback.onError(Throwable("Picasso Thumb Not Support"))
            return
        }
        callback.onSuccess(Result.Bitmap(target, Picasso.LoadedFrom.DISK))
    }
}