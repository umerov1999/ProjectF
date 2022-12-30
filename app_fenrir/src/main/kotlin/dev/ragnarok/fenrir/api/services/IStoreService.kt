package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiStickerSetsData
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IStoreService : IServiceRest() {
    fun getStickers(code: String?): Single<BaseResponse<VKApiStickerSetsData>> {
        return rest.request(
            "execute",
            form("code" to code),
            base(VKApiStickerSetsData.serializer())
        )
    }

    fun getStickersKeywords(code: String?): Single<BaseResponse<VKApiStickersKeywords>> {
        return rest.request(
            "execute",
            form("code" to code),
            base(VKApiStickersKeywords.serializer())
        )
    }
}