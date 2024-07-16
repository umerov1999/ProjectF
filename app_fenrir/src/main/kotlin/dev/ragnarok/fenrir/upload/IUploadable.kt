package dev.ragnarok.fenrir.upload

import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.model.server.UploadServer
import kotlinx.coroutines.flow.Flow

interface IUploadable<T> {
    fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<T>>
}