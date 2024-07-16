package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IStoreApi
import dev.ragnarok.fenrir.api.model.Dictionary
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiSticker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.api.services.IStoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class StoreApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStoreApi {
    override val stickerKeywords: Flow<Dictionary<VKApiStickersKeywords>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMapConcat {
                it.getStickersKeywords()
                    .map(extractResponseWithErrorHandling())
            }
    override val stickersSets: Flow<Items<VKApiStickerSet.Product>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMapConcat {
                it.getStickersSets()
                    .map(extractResponseWithErrorHandling())
            }
    override val recentStickers: Flow<Items<VKApiSticker>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMapConcat {
                it.getRecentStickers()
                    .map(extractResponseWithErrorHandling())
            }
}