package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.LikeResponse
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import dev.ragnarok.fenrir.api.model.response.isLikeResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class ILikesService : IServiceRest() {
    //https://vk.com/dev/likes.getList
    fun getList(
        type: String?,
        ownerId: Long?,
        itemId: Int?,
        pageUrl: String?,
        filter: String?,
        friendsOnly: Int?,
        extended: Int?,
        offset: Int?,
        count: Int?,
        skipOwn: Int?,
        fields: String?
    ): Single<BaseResponse<LikesListResponse>> {
        return rest.request(
            "likes.getList", form(
                "type" to type,
                "owner_id" to ownerId,
                "item_id" to itemId,
                "page_url" to pageUrl,
                "filter" to filter,
                "friends_only" to friendsOnly,
                "extended" to extended,
                "offset" to offset,
                "count" to count,
                "skip_own" to skipOwn,
                "fields" to fields
            ), base(LikesListResponse.serializer())
        )
    }

    //https://vk.com/dev/likes.delete
    fun delete(
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Single<BaseResponse<LikeResponse>> {
        return rest.request(
            "likes.delete", form(
                "type" to type,
                "owner_id" to ownerId,
                "item_id" to itemId,
                "access_key" to accessKey
            ), base(LikeResponse.serializer())
        )
    }

    //https://vk.com/dev/likes.add
    fun add(
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Single<BaseResponse<LikeResponse>> {
        return rest.request(
            "likes.add", form(
                "type" to type,
                "owner_id" to ownerId,
                "item_id" to itemId,
                "access_key" to accessKey
            ), base(LikeResponse.serializer())
        )
    }

    //https://vk.com/dev/likes.isLiked
    fun isLiked(
        type: String?,
        ownerId: Long?,
        itemId: Int
    ): Single<BaseResponse<isLikeResponse>> {
        return rest.request(
            "likes.isLiked", form(
                "type" to type,
                "owner_id" to ownerId,
                "item_id" to itemId
            ), base(isLikeResponse.serializer())
        )
    }

    fun checkAndAddLike(
        code: String?,
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "execute", form(
                "code" to code,
                "type" to type,
                "owner_id" to ownerId,
                "item_id" to itemId,
                "access_key" to accessKey
            ), baseInt
        )
    }
}