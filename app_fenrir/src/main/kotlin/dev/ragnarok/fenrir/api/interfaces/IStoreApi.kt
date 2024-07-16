package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.Dictionary
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiSticker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import kotlinx.coroutines.flow.Flow

interface IStoreApi {
    val stickerKeywords: Flow<Dictionary<VKApiStickersKeywords>>
    val stickersSets: Flow<Items<VKApiStickerSet.Product>>
    val recentStickers: Flow<Items<VKApiSticker>>
}