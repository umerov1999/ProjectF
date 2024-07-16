package dev.ragnarok.filegallery.api.services

import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

class IUploadService : IServiceRest() {
    fun remotePlayAudioRx(
        server: String,
        file: MultipartBody.Part
    ): Flow<BaseResponse<Int>> {
        return rest.doMultipartFormFullUrl(server, file, baseInt)
    }
}