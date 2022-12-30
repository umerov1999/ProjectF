package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IStoreApi
import dev.ragnarok.fenrir.api.model.VKApiStickerSetsData
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.api.services.IStoreService
import io.reactivex.rxjava3.core.Single

internal class StoreApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStoreApi {
    override val stickerKeywords: Single<VKApiStickersKeywords>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getStickersKeywords("var dic=API.store.getStickersKeywords({'v':'" + Constants.API_VERSION + "','aliases':1,'all_products':1}).dictionary;return {'keywords': dic@.words, 'words_stickers': dic@.user_stickers};")
                    .map(extractResponseWithErrorHandling())
            }
    override val stickers: Single<VKApiStickerSetsData>
        get() = provideService(IStoreService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getStickers("var pack = API.store.getProducts({'v':'" + Constants.API_VERSION + "','extended':1,'filters':'active','type':'stickers'}); var recent = API.messages.getRecentStickers(); return {'sticker_pack': pack, 'recent': recent};")
                    .map(extractResponseWithErrorHandling())
            }
}