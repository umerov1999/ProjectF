package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IUtilsService : IServiceRest() {
    fun resolveScreenName(screenName: String?): Flow<BaseResponse<ResolveDomailResponse>> {
        return rest.request(
            "utils.resolveScreenName",
            form("screen_name" to screenName),
            base(ResolveDomailResponse.serializer())
        )
    }

    fun getShortLink(url: String?, t_private: Int?): Flow<BaseResponse<VKApiShortLink>> {
        return rest.request(
            "utils.getShortLink",
            form("url" to url, "private" to t_private),
            base(VKApiShortLink.serializer())
        )
    }

    fun getLastShortenedLinks(
        count: Int?,
        offset: Int?
    ): Flow<BaseResponse<Items<VKApiShortLink>>> {
        return rest.request(
            "utils.getLastShortenedLinks", form("count" to count, "offset" to offset),
            items(VKApiShortLink.serializer())
        )
    }

    fun deleteFromLastShortened(key: String?): Flow<BaseResponse<Int>> {
        return rest.request(
            "utils.deleteFromLastShortened", form("key" to key),
            baseInt
        )
    }

    fun checkLink(url: String?): Flow<BaseResponse<VKApiCheckedLink>> {
        return rest.request(
            "utils.checkLink", form("url" to url),
            base(VKApiCheckedLink.serializer())
        )
    }

    fun joinChatByInviteLink(link: String?): Flow<BaseResponse<VKApiChatResponse>> {
        return rest.request(
            "messages.joinChatByInviteLink",
            form("link" to link),
            base(VKApiChatResponse.serializer())
        )
    }

    fun getInviteLink(
        peer_id: Long?,
        reset: Int?
    ): Flow<BaseResponse<VKApiLinkResponse>> {
        return rest.request(
            "messages.getInviteLink",
            form("peer_id" to peer_id, "reset" to reset),
            base(VKApiLinkResponse.serializer())
        )
    }

    fun customScript(code: String?): Flow<BaseResponse<Int>> {
        return rest.request("execute", form("code" to code), baseInt)
    }

    fun getServerTime(): Flow<BaseResponse<Long>> {
        return rest.request("utils.getServerTime", null, baseLong)
    }
}