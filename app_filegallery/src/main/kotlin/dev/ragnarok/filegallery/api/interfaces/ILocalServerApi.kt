package dev.ragnarok.filegallery.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileRemote
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import io.reactivex.rxjava3.core.Single
import java.io.InputStream

interface ILocalServerApi {
    @CheckResult
    fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Single<List<Video>>

    @CheckResult
    fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Single<List<Audio>>

    @CheckResult
    fun getDiscography(offset: Int?, count: Int?, reverse: Boolean): Single<List<Audio>>

    @CheckResult
    fun getPhotos(offset: Int?, count: Int?, reverse: Boolean): Single<MutableList<Photo>>

    @CheckResult
    fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<List<Video>>

    @CheckResult
    fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<List<Audio>>

    @CheckResult
    fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<List<Audio>>

    @CheckResult
    fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<List<Photo>>

    @CheckResult
    fun update_time(hash: String?): Single<Int>

    @CheckResult
    fun delete_media(hash: String?): Single<Int>

    @CheckResult
    fun get_file_name(hash: String?): Single<String>

    @CheckResult
    fun update_file_name(hash: String?, name: String?): Single<Int>

    @CheckResult
    fun fsGet(dir: String?): Single<List<FileRemote>>

    @CheckResult
    fun rebootPC(type: String?): Single<Int>

    @CheckResult
    fun remotePlayAudioRx(
        server: String?,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<BaseResponse<Int>>
}