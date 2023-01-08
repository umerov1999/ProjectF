package dev.ragnarok.fenrir.db.impl

import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.db.interfaces.ILocalMediaStorage
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicassoNew
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("DEPRECATION")
internal class LocalMediaStorage(mRepositoryContext: AppStorages) : AbsStorage(mRepositoryContext),
    ILocalMediaStorage {
    override val videos: Single<List<LocalVideo>>
        get() = Single.create {
            val cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                VIDEO_PROJECTION, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val data = ArrayList<LocalVideo>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    data.add(mapVideo(cursor))
                }
                cursor.close()
            }
            it.onSuccess(data)
        }

    override fun getAudios(accountId: Int): Single<List<Audio>> {
        return Single.create { e: SingleEmitter<List<Audio>> ->
            val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AUDIO_PROJECTION, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val data = ArrayList<Audio>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val audio = mapAudio(accountId, cursor) ?: continue
                    data.add(audio)
                }
                cursor.close()
            }
            e.onSuccess(data)
        }
    }

    override fun getAudios(accountId: Int, albumId: Long): Single<List<Audio>> {
        return Single.create { e: SingleEmitter<List<Audio>> ->
            val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AUDIO_PROJECTION,
                MediaStore.MediaColumns.BUCKET_ID + " = ?",
                arrayOf(albumId.toString()),
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val data = ArrayList<Audio>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val audio = mapAudio(accountId, cursor) ?: continue
                    data.add(audio)
                }
                cursor.close()
            }
            e.onSuccess(data)
        }
    }

    override fun getPhotos(albumId: Long): Single<List<LocalPhoto>> {
        return Single.create { e: SingleEmitter<List<LocalPhoto>> ->
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, MediaStore.MediaColumns.BUCKET_ID + " = ?", arrayOf(albumId.toString()),
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val result = ArrayList<LocalPhoto>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) break
                    val imageId = cursor.getLong(BaseColumns._ID)
                    val data =
                        cursor.getString(MediaStore.MediaColumns.DATA)
                    result.add(
                        LocalPhoto()
                            .setImageId(imageId)
                            .setFullImageUri(Uri.parse(data))
                    )
                }
                cursor.close()
            }
            e.onSuccess(result)
        }
    }

    override val photos: Single<List<LocalPhoto>>
        get() = Single.create { e: SingleEmitter<List<LocalPhoto>> ->
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, null, null,
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val result = ArrayList<LocalPhoto>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) break
                    val imageId = cursor.getLong(BaseColumns._ID)
                    val data =
                        cursor.getString(MediaStore.MediaColumns.DATA)
                    result.add(
                        LocalPhoto()
                            .setImageId(imageId)
                            .setFullImageUri(Uri.parse(data))
                    )
                }
                cursor.close()
            }
            e.onSuccess(result)
        }

    private fun hasAlbumById(albumId: Int, albums: List<LocalImageAlbum>): Boolean {
        for (i in albums) {
            if (i.getId() == albumId) {
                i.setPhotoCount(i.getPhotoCount() + 1)
                return true
            }
        }
        return false
    }

    override val audioAlbums: Single<List<LocalImageAlbum>>
        get() = Single.create {
            val album = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
            val albumId = MediaStore.MediaColumns.BUCKET_ID
            val coverId = BaseColumns._ID
            val projection = arrayOf(album, albumId, coverId)
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val albums: MutableList<LocalImageAlbum> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (it.isDisposed) break
                    if (!hasAlbumById(cursor.getInt(1), albums)) {
                        albums.add(
                            LocalImageAlbum()
                                .setId(cursor.getInt(1))
                                .setName(cursor.getString(0))
                                .setCoverId(cursor.getLong(2))
                                .setPhotoCount(1)
                        )
                    }
                }
                cursor.close()
            }
            it.onSuccess(albums)
        }
    override val imageAlbums: Single<List<LocalImageAlbum>>
        get() = Single.create {
            val album = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
            val albumId = MediaStore.MediaColumns.BUCKET_ID
            val coverId = BaseColumns._ID
            val projection = arrayOf(album, albumId, coverId)
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            val albums: MutableList<LocalImageAlbum> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (it.isDisposed) break
                    if (!hasAlbumById(cursor.getInt(1), albums)) {
                        albums.add(
                            LocalImageAlbum()
                                .setId(cursor.getInt(1))
                                .setName(cursor.getString(0))
                                .setCoverId(cursor.getLong(2))
                                .setPhotoCount(1)
                        )
                    }
                }
                cursor.close()
            }
            it.onSuccess(albums)
        }

    override fun getOldThumbnail(@Content_Local type: Int, content_Id: Long): Bitmap? {
        if (type == Content_Local.PHOTO) {
            return MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                content_Id, MediaStore.Images.Thumbnails.MINI_KIND, null
            )
        } else if (type == Content_Local.AUDIO) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            val oo =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, content_Id)
            return try {
                mediaMetadataRetriever.setDataSource(context, oo)
                val cover = mediaMetadataRetriever.embeddedPicture ?: return null
                val inputStream: InputStream = ByteArrayInputStream(cover)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, false)
                }
                bitmap
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                null
            }
        }
        return MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            content_Id, MediaStore.Video.Thumbnails.MINI_KIND, null
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun getThumbnail(uri: Uri?, x: Int, y: Int): Bitmap? {
        uri ?: return null
        return try {
            context.contentResolver.loadThumbnail(uri, Size(x, y), null)
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
            null
        }
    }

    companion object {
        private val PROJECTION = arrayOf(BaseColumns._ID, MediaStore.MediaColumns.DATA)
        private val VIDEO_PROJECTION = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DISPLAY_NAME
        )
        private val AUDIO_PROJECTION = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.DISPLAY_NAME
        )

        internal fun mapVideo(cursor: Cursor): LocalVideo {
            return LocalVideo(
                cursor.getLong(BaseColumns._ID),
                buildUriForPicassoNew(
                    Content_Local.VIDEO,
                    cursor.getLong(BaseColumns._ID)
                )
            )
                .setDuration(cursor.getInt(MediaStore.MediaColumns.DURATION))
                .setSize(cursor.getLong(MediaStore.MediaColumns.SIZE))
                .setTitle(cursor.getString(MediaStore.MediaColumns.DISPLAY_NAME))
        }

        internal fun mapAudio(accountId: Int, cursor: Cursor): Audio? {
            val id = cursor.getLong(BaseColumns._ID)
            val data = buildUriForPicassoNew(Content_Local.AUDIO, id).toString()
            if (cursor.getString(MediaStore.MediaColumns.DISPLAY_NAME)
                    .isNullOrEmpty()
            ) {
                return null
            }
            var TrackName =
                cursor.getString(MediaStore.MediaColumns.DISPLAY_NAME)?.replace(".mp3", "")
                    .orEmpty()
            var Artist = ""
            val arr = TrackName.split(Regex(" - ")).toTypedArray()
            if (arr.size > 1) {
                Artist = arr[0]
                TrackName = TrackName.replace("$Artist - ", "")
            }
            var dur = cursor.getInt(MediaStore.MediaColumns.DURATION)
            if (dur != 0) {
                dur /= 1000
            }
            val ret =
                Audio().setIsLocal().setId(data.hashCode()).setOwnerId(accountId).setDuration(dur)
                    .setUrl(data).setTitle(TrackName).setArtist(Artist)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ret.setThumb_image_big(data).setThumb_image_little(data)
            } else {
                val uri = buildUriForPicasso(Content_Local.AUDIO, id).toString()
                ret.setThumb_image_big(uri).setThumb_image_little(uri)
            }
        }
    }
}