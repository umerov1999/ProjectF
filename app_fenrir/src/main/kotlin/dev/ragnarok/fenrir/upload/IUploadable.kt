package dev.ragnarok.fenrir.upload

import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.model.server.UploadServer
import io.reactivex.rxjava3.core.Single

interface IUploadable<T> {
    fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<T>>
}