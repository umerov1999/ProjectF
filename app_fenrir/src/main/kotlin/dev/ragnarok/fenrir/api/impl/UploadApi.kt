package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IUploadRetrofitProvider
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.IUploadApi
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.upload.*
import dev.ragnarok.fenrir.api.services.IUploadService
import dev.ragnarok.fenrir.api.util.ProgressRequestBody
import dev.ragnarok.fenrir.api.util.ProgressRequestBody.UploadCallbacks
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.InputStream

class UploadApi internal constructor(private val provider: IUploadRetrofitProvider) : IUploadApi {
    private fun service(): IUploadService {
        return provider.provideUploadRetrofit().blockingGet().create(IUploadService::class.java)
    }

    override fun uploadDocumentRx(
        server: String?,
        filename: String?,
        doc: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadDocDto> {
        val body = ProgressRequestBody(
            doc, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().uploadDocumentRx(server, part)
    }

    override fun uploadAudioRx(
        server: String?,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadAudioDto> {
        val body = ProgressRequestBody(
            `is`, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().uploadAudioRx(server, part)
    }

    override fun remotePlayAudioRx(
        server: String?,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<BaseResponse<Int>> {
        val body = ProgressRequestBody(
            `is`, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("audio", filename, body)
        return service().remotePlayAudioRx(server, part)
    }

    override fun uploadStoryRx(
        server: String?,
        filename: String?,
        `is`: InputStream,
        listener: PercentagePublisher?,
        isVideo: Boolean
    ): Single<BaseResponse<UploadStoryDto>> {
        val body = ProgressRequestBody(
            `is`, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part =
            MultipartBody.Part.createFormData(
                if (!isVideo) "photo" else "video_file",
                filename,
                body
            )
        return service().uploadStoryRx(server, part)
    }

    override fun uploadVideoRx(
        server: String?,
        filename: String?,
        video: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadVideoDto> {
        val body = ProgressRequestBody(
            video, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().uploadVideoRx(server, part)
    }

    override fun uploadOwnerPhotoRx(
        server: String?,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadOwnerPhotoDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().uploadOwnerPhotoRx(server, part)
    }

    override fun uploadChatPhotoRx(
        server: String?,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadChatPhotoDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().uploadChatPhotoRx(server, part)
    }

    override fun uploadPhotoToWallRx(
        server: String?,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToWallDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().uploadPhotoToWallRx(server, part)
    }

    override fun uploadPhotoToMessageRx(
        server: String?,
        `is`: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToMessageDto> {
        val body =
            ProgressRequestBody(
                `is`, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().uploadPhotoToMessageRx(server, part)
    }

    override fun uploadPhotoToAlbumRx(
        server: String?,
        file1: InputStream,
        listener: PercentagePublisher?
    ): Single<UploadPhotoToAlbumDto> {
        val body =
            ProgressRequestBody(
                file1, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file1", "photo.jpg", body)
        return service().uploadPhotoToAlbumRx(server, part)
    }

    companion object {
        internal fun wrapPercentageListener(listener: PercentagePublisher?): UploadCallbacks {
            return object : UploadCallbacks {
                override fun onProgressUpdate(percentage: Int) {
                    listener?.onProgressChanged(percentage)
                }
            }
        }
    }
}