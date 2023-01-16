package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFriendList
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DeleteFriendResponse
import dev.ragnarok.fenrir.api.model.response.MutualFriendsResponse
import dev.ragnarok.fenrir.api.model.response.OnlineFriendsResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IFriendsService : IServiceRest() {
    fun getOnline(code: String?): Single<BaseResponse<OnlineFriendsResponse>> {
        return rest.request(
            "execute",
            form("code" to code),
            base(OnlineFriendsResponse.serializer())
        )
    }

    /*@FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<FriendsWithCountersResponse>> getWithMyCounters(@Field("code") String code);*/
    operator fun get(
        userId: Long?,
        order: String?,
        listId: Int?,
        count: Int?,
        offset: Int?,
        fields: String?,
        nameCase: String?
    ): Single<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "friends.get", form(
                "user_id" to userId,
                "order" to order,
                "list_id" to listId,
                "count" to count,
                "offset" to offset,
                "fields" to fields,
                "name_case" to nameCase
            ), items(VKApiUser.serializer())
        )
    }

    fun getRecommendations(
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Single<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "friends.getRecommendations", form(
                "count" to count,
                "fields" to fields,
                "name_case" to nameCase
            ), items(VKApiUser.serializer())
        )
    }

    //https://vk.com/dev/friends.getLists
    fun getLists(
        userId: Long?,
        returnSystem: Int?
    ): Single<BaseResponse<Items<VKApiFriendList>>> {
        return rest.request(
            "friends.getLists", form(
                "user_id" to userId,
                "return_system" to returnSystem
            ), items(VKApiFriendList.serializer())
        )
    }

    //https://vk.com/dev/friends.delete
    fun delete(userId: Long): Single<BaseResponse<DeleteFriendResponse>> {
        return rest.request(
            "friends.delete",
            form("user_id" to userId),
            base(DeleteFriendResponse.serializer())
        )
    }

    //https://vk.com/dev/friends.add
    fun add(
        userId: Long,
        text: String?,
        follow: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "friends.add", form(
                "user_id" to userId,
                "text" to text,
                "follow" to follow
            ), baseInt
        )
    }

    fun deleteSubscriber(
        subscriber_id: Long
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "friends.deleteSubscriber", form(
                "subscriber_id" to subscriber_id,
            ), baseInt
        )
    }

    //https://vk.com/dev/friends.search
    fun search(
        userId: Long,
        query: String?,
        fields: String?,
        nameCase: String?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "friends.search", form(
                "user_id" to userId,
                "q" to query,
                "fields" to fields,
                "name_case" to nameCase,
                "offset" to offset,
                "count" to count
            ), items(VKApiUser.serializer())
        )
    }

    fun getMutual(code: String?): Single<BaseResponse<MutualFriendsResponse>> {
        return rest.request(
            "execute",
            form("code" to code),
            base(MutualFriendsResponse.serializer())
        )
    }

    //https://vk.com/dev/friends.getByPhones
    fun getByPhones(
        phones: String?,
        fields: String?
    ): Single<BaseResponse<List<VKApiUser>>> {
        return rest.request(
            "friends.getByPhones", form(
                "phones" to phones,
                "fields" to fields
            ), baseList(VKApiUser.serializer())
        )
    }
}