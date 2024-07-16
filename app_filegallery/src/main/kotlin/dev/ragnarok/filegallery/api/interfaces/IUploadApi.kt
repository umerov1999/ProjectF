package dev.ragnarok.filegallery.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface IUploadApi {
    @CheckResult
    fun remotePlayAudioRx(
        server: String,
        filename: String?,
        inputStream: InputStream,
        listener: PercentagePublisher?
    ): Flow<BaseResponse<Int>>
}