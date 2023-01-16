package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.api.model.response.GroupWallInfoResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IGroupsService : IServiceRest() {
    fun editManager(
        groupId: Long,
        userId: Long,
        role: String?,
        isContact: Int?,
        contactPosition: String?,
        contactEmail: String?,
        contactPhone: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "groups.editManager", form(
                "group_id" to groupId,
                "user_id" to userId,
                "role" to role,
                "is_contact" to isContact,
                "contact_position" to contactPosition,
                "contact_email" to contactEmail,
                "contact_phone" to contactPhone
            ), baseInt
        )
    }

    fun edit(
        groupId: Long,
        title: String?,
        description: String?,
        screen_name: String?,
        access: Int?,
        website: String?,
        //public_category: Int?,
        //public_subcategory: Int?,
        public_date: String?,
        age_limits: Int?,
        obscene_filter: Int?,
        obscene_stopwords: Int?,
        obscene_words: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "groups.edit", form(
                "group_id" to groupId,
                "title" to title,
                "description" to description,
                "screen_name" to screen_name,
                "access" to access,
                "website" to website,
                "public_date" to public_date,
                "age_limits" to age_limits,
                "obscene_filter" to obscene_filter,
                "obscene_stopwords" to obscene_stopwords,
                "obscene_words" to obscene_words
            ), baseInt
        )
    }

    fun unban(
        groupId: Long,
        ownerId: Long
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "groups.unban", form(
                "group_id" to groupId,
                "owner_id" to ownerId
            ), baseInt
        )
    }

    fun getMarketAlbums(
        owner_id: Long,
        offset: Int,
        count: Int
    ): Single<BaseResponse<Items<VKApiMarketAlbum>>> {
        return rest.request(
            "market.getAlbums", form(
                "owner_id" to owner_id,
                "offset" to offset,
                "count" to count
            ), items(VKApiMarketAlbum.serializer())
        )
    }

    fun getMarket(
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>> {
        return rest.request(
            "market.get", form(
                "owner_id" to owner_id,
                "album_id" to album_id,
                "offset" to offset,
                "count" to count,
                "extended" to extended
            ), items(VKApiMarket.serializer())
        )
    }

    fun getMarketServices(
        owner_id: Long,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>> {
        return rest.request(
            "market.getServices", form(
                "owner_id" to owner_id,
                "offset" to offset,
                "count" to count,
                "extended" to extended
            ), items(VKApiMarket.serializer())
        )
    }

    fun getMarketById(
        item_ids: String?,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>> {
        return rest.request(
            "market.getById", form(
                "item_ids" to item_ids,
                "extended" to extended
            ), items(VKApiMarket.serializer())
        )
    }

    fun ban(
        groupId: Long,
        ownerId: Long,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "groups.ban", form(
                "group_id" to groupId,
                "owner_id" to ownerId,
                "end_date" to endDate,
                "reason" to reason,
                "comment" to comment,
                "comment_visible" to commentVisible
            ), baseInt
        )
    }

    fun getSettings(groupId: Long): Single<BaseResponse<GroupSettingsDto>> {
        return rest.request(
            "groups.getSettings",
            form("group_id" to groupId),
            base(GroupSettingsDto.serializer())
        )
    }

    //https://vk.com/dev/groups.getBanned
    fun getBanned(
        groupId: Long,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Long?
    ): Single<BaseResponse<Items<VKApiBanned>>> {
        return rest.request(
            "groups.getBanned", form(
                "group_id" to groupId,
                "offset" to offset,
                "count" to count,
                "fields" to fields,
                "user_id" to userId
            ), items(VKApiBanned.serializer())
        )
    }

    fun getGroupWallInfo(
        code: String?,
        groupId: String?,
        fields: String?
    ): Single<BaseResponse<GroupWallInfoResponse>> {
        return rest.request(
            "execute", form(
                "code" to code,
                "group_id" to groupId,
                "fields" to fields
            ), base(GroupWallInfoResponse.serializer())
        )
    }

    fun getMembers(
        groupId: String?,
        sort: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        filter: String?
    ): Single<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "groups.getMembers", form(
                "group_id" to groupId,
                "sort" to sort,
                "offset" to offset,
                "count" to count,
                "fields" to fields,
                "filter" to filter
            ), items(VKApiUser.serializer())
        )
    }

    //https://vk.com/dev/groups.search
    fun search(
        query: String?,
        type: String?,
        fields: String?,
        countryId: Int?,
        cityId: Int?,
        future: Int?,
        market: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiCommunity>>> {
        return rest.request(
            "groups.search", form(
                "q" to query,
                "type" to type,
                "fields" to fields,
                "country_id" to countryId,
                "city_id" to cityId,
                "future" to future,
                "market" to market,
                "sort" to sort,
                "offset" to offset,
                "count" to count
            ), items(VKApiCommunity.serializer())
        )
    }

    fun getLongPollServer(groupId: Long): Single<BaseResponse<GroupLongpollServer>> {
        return rest.request(
            "groups.getLongPollServer",
            form("group_id" to groupId),
            base(GroupLongpollServer.serializer())
        )
    }

    //https://vk.com/dev/groups.leave
    fun leave(groupId: Long): Single<BaseResponse<Int>> {
        return rest.request("groups.leave", form("group_id" to groupId), baseInt)
    }

    //https://vk.com/dev/groups.join
    fun join(
        groupId: Long,
        notSure: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "groups.join", form(
                "group_id" to groupId,
                "not_sure" to notSure
            ), baseInt
        )
    }

    //https://vk.com/dev/groups.get
    operator fun get(
        userId: Long?,
        extended: Int?,
        filter: String?,
        fields: String?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiCommunity>>> {
        return rest.request(
            "groups.get", form(
                "user_id" to userId,
                "extended" to extended,
                "filter" to filter,
                "fields" to fields,
                "offset" to offset,
                "count" to count
            ), items(VKApiCommunity.serializer())
        )
    }

    /**
     * Returns information about communities by their IDs.
     *
     * @param groupIds IDs or screen names of communities.
     * List of comma-separated words
     * @param groupId  ID or screen name of the community
     * @param fields   Group fields to return. List of comma-separated words
     * @return an array of objects describing communities
     */
    fun getById(
        groupIds: String?,
        groupId: String?,
        fields: String?
    ): Single<BaseResponse<List<VKApiCommunity>>> {
        return rest.request(
            "groups.getById", form(
                "group_ids" to groupIds,
                "group_id" to groupId,
                "fields" to fields
            ), baseList(VKApiCommunity.serializer())
        )
    }

    fun getChats(
        groupId: Long,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiGroupChats>>> {
        return rest.request(
            "groups.getChats", form(
                "group_id" to groupId,
                "offset" to offset,
                "count" to count
            ), items(VKApiGroupChats.serializer())
        )
    }
}