package dev.ragnarok.filegallery.upload

import dev.ragnarok.filegallery.api.PercentagePublisher
import kotlinx.coroutines.flow.Flow

interface IUploadable<T> {
    fun doUpload(
        upload: Upload,
        listener: PercentagePublisher?
    ): Flow<UploadResult<T>>
}