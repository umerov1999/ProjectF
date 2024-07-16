package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IUploadRestProvider
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.IUploadApi
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
import dev.ragnarok.fenrir.api.services.IUploadService
import dev.ragnarok.fenrir.api.util.ProgressRequestBody
import dev.ragnarok.fenrir.api.util.ProgressRequestBody.UploadCallbacks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.InputStream

class UploadApi internal constructor(private val provider: IUploadRestProvider) : IUploadApi {
    private fun service(): Flow<IUploadService> {
        return provider.provideUploadRest().map {
            val ret = IUploadService()
            ret.addon(it)
            ret
        }
    }

    override fun uploadDocumentRx(
        server: String,
        filename: String?,
        doc: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadDocDto> {
        val body = ProgressRequestBody(
            doc, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().flatMapConcat {
            it.uploadDocumentRx(server, part)
        }
    }

    override fun uploadAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadAudioDto> {
        val body = ProgressRequestBody(
            inputStream, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().flatMapConcat {
            it.uploadAudioRx(server, part)
        }
    }

    override fun remotePlayAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<BaseResponse<Int>> {
        val body = ProgressRequestBody(
            inputStream, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("audio", filename, body)
        return service().flatMapConcat {
            it.remotePlayAudioRx(server, part)
        }
    }

    override fun uploadStoryRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?,
        isVideo: Boolean
    ): Flow<CustomResponse<UploadStoryDto>> {
        val body = ProgressRequestBody(
            inputStream, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part =
            MultipartBody.Part.createFormData(
                if (!isVideo) "photo" else "video_file",
                filename,
                body
            )
        return service().flatMapConcat {
            it.uploadStoryRx(server, part)
        }
    }

    override fun uploadVideoRx(
        server: String,
        filename: String?,
        video: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadVideoDto> {
        val body = ProgressRequestBody(
            video, wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", filename, body)
        return service().flatMapConcat {
            it.uploadVideoRx(server, part)
        }
    }

    override fun uploadOwnerPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadOwnerPhotoDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().flatMapConcat {
            it.uploadOwnerPhotoRx(server, part)
        }
    }

    override fun uploadChatPhotoRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadChatPhotoDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().flatMapConcat {
            it.uploadChatPhotoRx(server, part)
        }
    }

    override fun uploadPhotoToWallRx(
        server: String,
        photo: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToWallDto> {
        val body =
            ProgressRequestBody(
                photo, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().flatMapConcat {
            it.uploadPhotoToWallRx(server, part)
        }
    }

    override fun uploadPhotoToMessageRx(
        server: String,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToMessageDto> {
        val body =
            ProgressRequestBody(
                inputStream, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("photo", "photo.jpg", body)
        return service().flatMapConcat {
            it.uploadPhotoToMessageRx(server, part)
        }
    }

    override fun uploadPhotoToAlbumRx(
        server: String,
        file1: InputStream,
        listener: PercentagePublisher?
    ): Flow<UploadPhotoToAlbumDto> {
        val body =
            ProgressRequestBody(
                file1, wrapPercentageListener(listener),
                "image/*".toMediaTypeOrNull()
            )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file1", "photo.jpg", body)
        return service().flatMapConcat {
            it.uploadPhotoToAlbumRx(server, part)
        }
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