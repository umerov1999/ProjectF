package dev.ragnarok.filegallery.api.services

import dev.ragnarok.filegallery.api.model.Items
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileRemote
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import retrofit2.http.*

interface ILocalServerService {
    @FormUrlEncoded
    @POST("audio.get")
    fun getAudios(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Audio>>>

    @FormUrlEncoded
    @POST("discography.get")
    fun getDiscography(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Audio>>>

    @FormUrlEncoded
    @POST("photos.get")
    fun getPhotos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Photo>>>

    @FormUrlEncoded
    @POST("video.get")
    fun getVideos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Video>>>

    @FormUrlEncoded
    @POST("audio.search")
    fun searchAudios(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Audio>>>

    @FormUrlEncoded
    @POST("discography.search")
    fun searchDiscography(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Audio>>>

    @FormUrlEncoded
    @POST("video.search")
    fun searchVideos(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Video>>>

    @FormUrlEncoded
    @POST("photos.search")
    fun searchPhotos(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<Photo>>>

    @FormUrlEncoded
    @POST("update_time")
    fun update_time(@Field("hash") hash: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("delete_media")
    fun delete_media(@Field("hash") hash: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("get_file_name")
    fun get_file_name(@Field("hash") hash: String?): Single<BaseResponse<String>>

    @FormUrlEncoded
    @POST("update_file_name")
    fun update_file_name(
        @Field("hash") hash: String?,
        @Field("name") name: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fs.get")
    fun fsGet(
        @Field("dir") dir: String?
    ): Single<BaseResponse<Items<FileRemote>>>

    @FormUrlEncoded
    @POST("rebootPC")
    fun rebootPC(
        @Field("type") type: String?
    ): Single<BaseResponse<Int>>

    @Multipart
    @POST
    fun remotePlayAudioRx(
        @Url server: String?,
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<Int>>
}