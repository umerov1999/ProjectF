package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.model.FileRemote
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ILocalServerService {
    @FormUrlEncoded
    @POST("audio.get")
    fun getAudios(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("discography.get")
    fun getDiscography(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("photos.get")
    fun getPhotos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("video.get")
    fun getVideos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiVideo>>>

    @FormUrlEncoded
    @POST("audio.search")
    fun searchAudios(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("discography.search")
    fun searchDiscography(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("video.search")
    fun searchVideos(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiVideo>>>

    @FormUrlEncoded
    @POST("photos.search")
    fun searchPhotos(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("reverse") reverse: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

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

    @FormUrlEncoded
    @POST("audio.upload")
    fun uploadAudio(
        @Field("hash") hash: String?,
        @Field("access_token") access_token: String?,
        @Field("user_agent") user_agent: String?
    ): Single<BaseResponse<Int>>
}