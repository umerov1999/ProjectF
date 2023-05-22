package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.Dictionary
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiSticker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import io.reactivex.rxjava3.core.Single

interface IStoreApi {
    val stickerKeywords: Single<Dictionary<VKApiStickersKeywords>>
    val stickersSets: Single<Items<VKApiStickerSet.Product>>
    val recentStickers: Single<Items<VKApiSticker>>
}