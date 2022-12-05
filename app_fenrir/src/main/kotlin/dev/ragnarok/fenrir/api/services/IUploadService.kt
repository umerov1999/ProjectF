package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.upload.*
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface IUploadService {
    @Multipart
    @POST
    fun uploadDocumentRx(@Url server: String?, @Part file: MultipartBody.Part): Single<UploadDocDto>

    @Multipart
    @POST
    fun uploadAudioRx(@Url server: String?, @Part file: MultipartBody.Part): Single<UploadAudioDto>

    @Multipart
    @POST
    fun remotePlayAudioRx(
        @Url server: String?,
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<Int>>

    @Multipart
    @POST
    fun uploadStoryRx(
        @Url server: String?,
        @Part file: MultipartBody.Part
    ): Single<BaseResponse<UploadStoryDto>>

    @Multipart
    @POST
    fun uploadVideoRx(@Url server: String?, @Part file: MultipartBody.Part): Single<UploadVideoDto>

    @Multipart
    @POST
    fun uploadOwnerPhotoRx(
        @Url server: String?,
        @Part photo: MultipartBody.Part
    ): Single<UploadOwnerPhotoDto>

    @Multipart
    @POST
    fun uploadChatPhotoRx(
        @Url server: String?,
        @Part photo: MultipartBody.Part
    ): Single<UploadChatPhotoDto>

    @Multipart
    @POST
    fun uploadPhotoToWallRx(
        @Url server: String?,
        @Part photo: MultipartBody.Part
    ): Single<UploadPhotoToWallDto>

    @Multipart
    @POST
    fun uploadPhotoToMessageRx(
        @Url server: String?,
        @Part photo: MultipartBody.Part
    ): Single<UploadPhotoToMessageDto>

    @Multipart
    @POST
    fun uploadPhotoToAlbumRx(
        @Url server: String?,
        @Part file1: MultipartBody.Part
    ): Single<UploadPhotoToAlbumDto>
}