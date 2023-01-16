package dev.ragnarok.fenrir.db.interfaces

import android.graphics.Bitmap
import android.net.Uri
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.picasso.Content_Local
import io.reactivex.rxjava3.core.Single

interface ILocalMediaStorage : IStorage {
    fun getPhotos(albumId: Long): Single<List<LocalPhoto>>
    val photos: Single<List<LocalPhoto>>
    val imageAlbums: Single<List<LocalImageAlbum>>
    val audioAlbums: Single<List<LocalImageAlbum>>
    val videos: Single<List<LocalVideo>>
    fun getAudios(accountId: Long): Single<List<Audio>>
    fun getAudios(accountId: Long, albumId: Long): Single<List<Audio>>
    fun getOldThumbnail(@Content_Local type: Int, content_Id: Long): Bitmap?
    fun getThumbnail(uri: Uri?, x: Int, y: Int): Bitmap?
}