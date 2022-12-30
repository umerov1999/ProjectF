package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.upload.*
import io.reactivex.rxjava3.core.Single
import java.io.InputStream

interface IUploadApi {
    fun uploadDocumentRx(
        server: String,
        filename: String?,
        doc: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadDocDto>

    fun uploadAudioRx(
        server: String,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadAudioDto>

    fun remotePlayAudioRx(
        server: String,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<BaseResponse<Int>>

    fun uploadStoryRx(
        server: String,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?,
        isVideo: Boolean
    ): Single<BaseResponse<UploadStoryDto>>

    fun uploadVideoRx(
        server: String,
        filename: String?,
        video: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadVideoDto>

    fun uploadOwnerPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadOwnerPhotoDto>

    fun uploadChatPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadChatPhotoDto>

    fun uploadPhotoToWallRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToWallDto>

    fun uploadPhotoToMessageRx(
        server: String,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToMessageDto>

    fun uploadPhotoToAlbumRx(
        server: String,
        file1: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToAlbumDto>
}