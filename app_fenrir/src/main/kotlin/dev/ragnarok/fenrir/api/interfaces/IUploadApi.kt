package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.CustomResponse
import dev.ragnarok.fenrir.api.model.upload.UploadAudioDto
import dev.ragnarok.fenrir.api.model.upload.UploadChatPhotoDto
import dev.ragnarok.fenrir.api.model.upload.UploadDocDto
import dev.ragnarok.fenrir.api.model.upload.UploadOwnerPhotoDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToAlbumDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToMessageDto
import dev.ragnarok.fenrir.api.model.upload.UploadPhotoToWallDto
import dev.ragnarok.fenrir.api.model.upload.UploadStoryDto
import dev.ragnarok.fenrir.api.model.upload.UploadVideoDto
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface IUploadApi {
    fun uploadDocumentRx(
        server: String,
        filename: String?,
        doc: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadDocDto>

    fun uploadAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadAudioDto>

    fun remotePlayAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<BaseResponse<Int>>

    fun uploadStoryRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?,
        isVideo: Boolean
    ): Flow<CustomResponse<UploadStoryDto>>

    fun uploadVideoRx(
        server: String,
        filename: String?,
        video: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadVideoDto>

    fun uploadOwnerPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadOwnerPhotoDto>

    fun uploadChatPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadChatPhotoDto>

    fun uploadPhotoToWallRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToWallDto>

    fun uploadPhotoToMessageRx(
        server: String,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToMessageDto>

    fun uploadPhotoToAlbumRx(
        server: String,
        file1: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToAlbumDto>
}