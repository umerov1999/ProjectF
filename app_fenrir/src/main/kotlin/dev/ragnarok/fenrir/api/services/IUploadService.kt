package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.upload.UploadAudioDto
import dev.ragnarok.fenrir.api.model.upload.UploadChatPhotoDto
import dev.ragnarok.fenrir.api.model.upload.UploadDocDto
import dev.ragnarok.fenrir.api.model.upload.UploadOwnerPhotoDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToAlbumDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToMessageDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToWallDto
import dev.ragnarok.fenrir.api.model.upload.UploadStoryDto
import dev.ragnarok.fenrir.api.model.upload.UploadVideoDto
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody

class IUploadService : IServiceRest() {
    fun uploadDocumentRx(server: String, file: MultipartBody.Part): Single<UploadDocDto> {
        return rest.doMultipartFormFullUrl(server, file, UploadDocDto.serializer())
    }

    fun uploadAudioRx(server: String, file: MultipartBody.Part): Single<UploadAudioDto> {
        return rest.doMultipartFormFullUrl(server, file, UploadAudioDto.serializer())
    }

    fun remotePlayAudioRx(
        server: String,
        file: MultipartBody.Part
    ): Single<BaseResponse<Int>> {
        return rest.doMultipartFormFullUrl(server, file, baseInt)
    }

    fun uploadStoryRx(
        server: String,
        file: MultipartBody.Part
    ): Single<BaseResponse<UploadStoryDto>> {
        return rest.doMultipartFormFullUrl(server, file, base(UploadStoryDto.serializer()))
    }

    fun uploadVideoRx(server: String, file: MultipartBody.Part): Single<UploadVideoDto> {
        return rest.doMultipartFormFullUrl(server, file, UploadVideoDto.serializer())
    }

    fun uploadOwnerPhotoRx(
        server: String,
        photo: MultipartBody.Part
    ): Single<UploadOwnerPhotoDto> {
        return rest.doMultipartFormFullUrl(server, photo, UploadOwnerPhotoDto.serializer())
    }

    fun uploadChatPhotoRx(
        server: String,
        photo: MultipartBody.Part
    ): Single<UploadChatPhotoDto> {
        return rest.doMultipartFormFullUrl(server, photo, UploadChatPhotoDto.serializer())
    }

    fun uploadPhotoToWallRx(
        server: String,
        photo: MultipartBody.Part
    ): Single<UploadPhotoToWallDto> {
        return rest.doMultipartFormFullUrl(server, photo, UploadPhotoToWallDto.serializer())
    }

    fun uploadPhotoToMessageRx(
        server: String,
        photo: MultipartBody.Part
    ): Single<UploadPhotoToMessageDto> {
        return rest.doMultipartFormFullUrl(server, photo, UploadPhotoToMessageDto.serializer())
    }

    fun uploadPhotoToAlbumRx(
        server: String,
        file1: MultipartBody.Part
    ): Single<UploadPhotoToAlbumDto> {
        return rest.doMultipartFormFullUrl(server, file1, UploadPhotoToAlbumDto.serializer())
    }
}