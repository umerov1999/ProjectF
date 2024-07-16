package dev.ragnarok.fenrir.db.interfaces

import android.graphics.Bitmap
import android.net.Uri
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.picasso.Content_Local
import kotlinx.coroutines.flow.Flow

interface ILocalMediaStorage : IStorage {
    fun getPhotos(albumId: Long): Flow<List<LocalPhoto>>
    val photos: Flow<List<LocalPhoto>>
    val imageAlbums: Flow<List<LocalImageAlbum>>
    val audioAlbums: Flow<List<LocalImageAlbum>>
    val videos: Flow<List<LocalVideo>>
    fun getAudios(accountId: Long): Flow<List<Audio>>
    fun getAudios(accountId: Long, albumId: Long): Flow<List<Audio>>
    fun getOldThumbnail(@Content_Local type: Int, content_Id: Long): Bitmap?
    fun getThumbnail(uri: Uri?, x: Int, y: Int): Bitmap?
}