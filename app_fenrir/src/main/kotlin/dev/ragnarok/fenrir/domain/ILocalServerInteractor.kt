package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.FileRemote
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Video
import kotlinx.coroutines.flow.Flow

interface ILocalServerInteractor {
    fun getVideos(offset: Int, count: Int, reverse: Boolean): Flow<List<Video>>
    fun getAudios(offset: Int, count: Int, reverse: Boolean): Flow<List<Audio>>
    fun getDiscography(offset: Int, count: Int, reverse: Boolean): Flow<List<Audio>>
    fun getPhotos(offset: Int, count: Int, reverse: Boolean): Flow<List<Photo>>
    fun searchVideos(q: String?, offset: Int, count: Int, reverse: Boolean): Flow<List<Video>>
    fun searchAudios(q: String?, offset: Int, count: Int, reverse: Boolean): Flow<List<Audio>>
    fun searchDiscography(
        q: String?,
        offset: Int,
        count: Int,
        reverse: Boolean
    ): Flow<List<Audio>>

    fun searchPhotos(q: String?, offset: Int, count: Int, reverse: Boolean): Flow<List<Photo>>
    fun update_time(hash: String?): Flow<Int>
    fun delete_media(hash: String?): Flow<Int>
    fun get_file_name(hash: String?): Flow<String>
    fun update_file_name(hash: String?, name: String?): Flow<Int>
    fun rebootPC(type: String?): Flow<Int>
    fun fsGet(dir: String?): Flow<List<FileRemote>>
    fun uploadAudio(hash: String?): Flow<Int>
}