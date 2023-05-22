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
import io.reactivex.rxjava3.core.Single

internal class StoreApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStoreApi {
    override val stickerKeywords: Single<Dictionary<VKApiStickersKeywords>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getStickersKeywords()
                    .map(extractResponseWithErrorHandling())
            }
    override val stickersSets: Single<Items<VKApiStickerSet.Product>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getStickersSets()
                    .map(extractResponseWithErrorHandling())
            }
    override val recentStickers: Single<Items<VKApiSticker>>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getRecentStickers()
                    .map(extractResponseWithErrorHandling())
            }
}