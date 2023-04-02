package dev.ragnarok.filegallery.api.impl

import dev.ragnarok.filegallery.api.IUploadRestProvider
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.interfaces.IUploadApi
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.api.services.IUploadService
import dev.ragnarok.filegallery.api.util.ProgressRequestBody
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.InputStream

class UploadApi internal constructor(private val provider: IUploadRestProvider) : IUploadApi {
    private fun service(): Single<IUploadService> {
        return provider.provideUploadRest().map {
            val ret = IUploadService()
            ret.addon(it)
            ret
        }
    }

    override fun remotePlayAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Single<BaseResponse<Int>> {
        val body = ProgressRequestBody(
            inputStream, LocalServerApi.wrapPercentageListener(listener),
            "*/*".toMediaTypeOrNull()
        )
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("audio", filename, body)
        return service()
            .flatMap { it.remotePlayAudioRx(server, part) }
    }
}