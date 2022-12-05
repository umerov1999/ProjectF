package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.VKApiStickerSetsData
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import io.reactivex.rxjava3.core.Single

interface IStoreApi {
    val stickerKeywords: Single<VKApiStickersKeywords>
    val stickers: Single<VKApiStickerSetsData>
}