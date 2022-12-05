package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.FileRemote
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Video
import io.reactivex.rxjava3.core.Single

interface ILocalServerInteractor {
    fun getVideos(offset: Int, count: Int, reverse: Boolean): Single<List<Video>>
    fun getAudios(offset: Int, count: Int, reverse: Boolean): Single<List<Audio>>
    fun getDiscography(offset: Int, count: Int, reverse: Boolean): Single<List<Audio>>
    fun getPhotos(offset: Int, count: Int, reverse: Boolean): Single<List<Photo>>
    fun searchVideos(q: String?, offset: Int, count: Int, reverse: Boolean): Single<List<Video>>
    fun searchAudios(q: String?, offset: Int, count: Int, reverse: Boolean): Single<List<Audio>>
    fun searchDiscography(
        q: String?,
        offset: Int,
        count: Int,
        reverse: Boolean
    ): Single<List<Audio>>

    fun searchPhotos(q: String?, offset: Int, count: Int, reverse: Boolean): Single<List<Photo>>
    fun update_time(hash: String?): Single<Int>
    fun delete_media(hash: String?): Single<Int>
    fun get_file_name(hash: String?): Single<String>
    fun update_file_name(hash: String?, name: String?): Single<Int>
    fun rebootPC(type: String?): Single<Int>
    fun fsGet(dir: String?): Single<List<FileRemote>>
    fun uploadAudio(hash: String?): Single<Int>
}