package dev.ragnarok.filegallery.upload

import dev.ragnarok.filegallery.api.PercentagePublisher
import io.reactivex.rxjava3.core.Single

interface IUploadable<T> {
    fun doUpload(
        upload: Upload,
        listener: PercentagePublisher?
    ): Single<UploadResult<T>>
}