package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Dictionary
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiSticker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IStoreService : IServiceRest() {
    fun getRecentStickers(): Single<BaseResponse<Items<VKApiSticker>>> {
        return rest.request(
            "messages.getRecentStickers",
            null,
            items(VKApiSticker.serializer())
        )
    }

    fun getStickersSets(): Single<BaseResponse<Items<VKApiStickerSet.Product>>> {
        return rest.request(
            "store.getProducts",
            form(
                "extended" to 1,
                "filters" to "active",
                "type" to "stickers"
            ),
            items(VKApiStickerSet.Product.serializer())
        )
    }

    fun getStickersKeywords(): Single<BaseResponse<Dictionary<VKApiStickersKeywords>>> {
        return rest.request(
            "store.getStickersKeywords",
            form(
                "aliases" to 1,
                "all_products" to 0,
                "need_stickers" to 1
            ),
            dictionary(VKApiStickersKeywords.serializer())
        )
    }
}