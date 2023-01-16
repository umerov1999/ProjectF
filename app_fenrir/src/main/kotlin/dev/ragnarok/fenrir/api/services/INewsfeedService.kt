package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFeedList
import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class INewsfeedService : IServiceRest() {
    /**
     * filters post, photo, video, topic, market, note
     */
    //https://vk.com/dev/newsfeed.getComments
    fun getComments(
        count: Int?,
        filters: String?,
        reposts: String?,
        startTime: Long?,
        endTime: Long?,
        lastCommentsCount: Int?,
        startFrom: String?,
        fields: String?,
        photoSizes: Int?
    ): Single<BaseResponse<NewsfeedCommentsResponse>> {
        return rest.request(
            "newsfeed.getComments", form(
                "count" to count,
                "filters" to filters,
                "reposts" to reposts,
                "start_time" to startTime,
                "end_time" to endTime,
                "last_comments_count" to lastCommentsCount,
                "start_from" to startFrom,
                "fields" to fields,
                "photo_sizes" to photoSizes
            ), base(NewsfeedCommentsResponse.serializer())
        )
    }

    //https://vk.com/dev/newsfeed.getMentions
    fun getMentions(
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Single<BaseResponse<NewsfeedCommentsResponse>> {
        return rest.request(
            "newsfeed.getMentions", form(
                "owner_id" to owner_id,
                "count" to count,
                "offset" to offset,
                "start_time" to startTime,
                "end_time" to endTime
            ), base(NewsfeedCommentsResponse.serializer())
        )
    }

    //https://vk.com/dev/newsfeed.getLists
    fun getLists(
        listIds: String?,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiFeedList>>> {
        return rest.request(
            "newsfeed.getLists", form(
                "list_ids" to listIds,
                "extended" to extended
            ), items(VKApiFeedList.serializer())
        )
    }

    //https://vk.com/dev/newsfeed.saveList
    fun saveList(
        title: String?,
        source_ids: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "newsfeed.saveList", form(
                "title" to title,
                "source_ids" to source_ids
            ), baseInt
        )
    }

    //https://vk.com/dev/newsfeed.getBanned
    fun getBanned(
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<NewsfeedBanResponse>> {
        return rest.request(
            "newsfeed.getBanned", form(
                "extended" to extended,
                "fields" to fields
            ), base(NewsfeedBanResponse.serializer())
        )
    }

    //https://vk.com/dev/newsfeed.deleteBan
    fun deleteBan(
        user_ids: String?,
        group_ids: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "newsfeed.deleteBan", form(
                "user_ids" to user_ids,
                "group_ids" to group_ids
            ), baseInt
        )
    }

    //https://vk.com/dev/newsfeed.addBan
    fun addBan(
        user_ids: String?,
        group_ids: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "newsfeed.addBan", form(
                "user_ids" to user_ids,
                "group_ids" to group_ids
            ), baseInt
        )
    }

    //https://vk.com/dev/newsfeed.ignoreItem
    fun ignoreItem(
        type: String?,
        owner_id: Long?,
        item_id: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "newsfeed.ignoreItem", form(
                "type" to type,
                "owner_id" to owner_id,
                "item_id" to item_id
            ), baseInt
        )
    }

    //https://vk.com/dev/newsfeed.deleteList
    fun deleteList(list_id: Int?): Single<BaseResponse<Int>> {
        return rest.request("newsfeed.deleteList", form("list_id" to list_id), baseInt)
    }

    /**
     * Returns search results by statuses.
     *
     * @param query     Search query string (e.g., New Year).
     * @param extended  1 — to return additional information about the user or community that placed the post.
     * @param count     Number of posts to return.
     * @param latitude  Geographical latitude point (in degrees, -90 to 90) within which to search.
     * @param longitude Geographical longitude point (in degrees, -180 to 180) within which to search.
     * @param startTime Earliest timestamp (in Unix time) of a news item to return. By default, 24 hours ago.
     * @param endTime   Latest timestamp (in Unix time) of a news item to return. By default, the current time.
     * @param startFrom identifier required to get the next page of results.
     * Value for this parameter is returned in next_from field in a reply
     * @param fields    Additional fields of profiles and communities to return.
     * @return Returns the total number of posts and an array of wall objects
     */
    fun search(
        query: String?,
        extended: Int?,
        count: Int?,
        latitude: Double?,
        longitude: Double?,
        startTime: Long?,
        endTime: Long?,
        startFrom: String?,
        fields: String?
    ): Single<BaseResponse<NewsfeedSearchResponse>> {
        return rest.request(
            "newsfeed.search", form(
                "q" to query,
                "extended" to extended,
                "count" to count,
                "latitude" to latitude,
                "longitude" to longitude,
                "start_time" to startTime,
                "end_time" to endTime,
                "start_from" to startFrom,
                "fields" to fields
            ), base(NewsfeedSearchResponse.serializer())
        )
    }

    /**
     * Returns data required to show newsfeed for the current user.
     *
     * @param filters       Filters to apply:
     * post — new wall posts
     * photo — new photos
     * photo_tag — new photo tags
     * wall_photo — new wall photos
     * friend — new friends
     * note — new notes
     * List of comma-separated words
     * @param returnBanned  1 — to return news items from banned sources
     * @param startTime     Earliest timestamp (in Unix time) of a news item to return. By default, 24 hours ago.
     * @param endTime       Latest timestamp (in Unix time) of a news item to return. By default, the current time.
     * @param maxPhotoCount Maximum number of photos to return. By default, 5
     * @param sourceIds     Sources to obtain news from, separated by commas.
     * User IDs can be specified in formats <uid> or u<uid>
     * where <uid> is the user's friend ID.
     * Community IDs can be specified in formats -<gid> or g<gid>
     * where <gid> is the community ID.
     * If the parameter is not set, all of the user's friends and communities
     * are returned, except for banned sources, which can be obtained with
     * the newsfeed.getBanned method.
     * @param startFrom     identifier required to get the next page of results.
     * Value for this parameter is returned in next_from field in a reply
     * @param count         Number of news items to return (default 50; maximum 100). For auto feed,
     * you can use the new_offset parameter returned by this method.
     * @param fields        Additional fields of profiles and communities to return.
     * @return Returns an object containing the following fields:
     * items — News array for the current user.
     * profiles — Information about users in the newsfeed.
     * groups — Information about groups in the newsfeed.
     * new_offset — Contains an offset parameter that is passed to get the next array of news.
     * next_from — Contains a from parameter that is passed to get the next array of news.
    </gid></gid></gid></uid></uid></uid> */
    operator fun get(
        filters: String?,
        returnBanned: Int?,
        startTime: Long?,
        endTime: Long?,
        maxPhotoCount: Int?,
        sourceIds: String?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Single<BaseResponse<NewsfeedResponse>> {
        return rest.request(
            "newsfeed.get", form(
                "filters" to filters,
                "return_banned" to returnBanned,
                "start_time" to startTime,
                "end_time" to endTime,
                "max_photos" to maxPhotoCount,
                "source_ids" to sourceIds,
                "start_from" to startFrom,
                "count" to count,
                "fields" to fields
            ), base(NewsfeedResponse.serializer())
        )
    }

    fun getByType(
        feed_type: String,
        filters: String?,
        returnBanned: Int?,
        startTime: Long?,
        endTime: Long?,
        maxPhotoCount: Int?,
        sourceIds: String?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Single<BaseResponse<NewsfeedResponse>> {
        return rest.request(
            "newsfeed.getByType", form(
                "feed_type" to feed_type,
                "filters" to filters,
                "return_banned" to returnBanned,
                "start_time" to startTime,
                "end_time" to endTime,
                "max_photos" to maxPhotoCount,
                "source_ids" to sourceIds,
                "start_from" to startFrom,
                "count" to count,
                "fields" to fields
            ), base(NewsfeedResponse.serializer())
        )
    }

    fun getRecommended(
        startTime: Long?,
        endTime: Long?,
        maxPhotoCount: Int?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Single<BaseResponse<NewsfeedResponse>> {
        return rest.request(
            "newsfeed.getRecommended", form(
                "start_time" to startTime,
                "end_time" to endTime,
                "max_photos" to maxPhotoCount,
                "start_from" to startFrom,
                "count" to count,
                "fields" to fields
            ), base(NewsfeedResponse.serializer())
        )
    }

    fun getFeedLikes(
        maxPhotoCount: Int?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Single<BaseResponse<NewsfeedResponse>> {
        return rest.request(
            "execute.getFeedLikes", form(
                "max_photos" to maxPhotoCount,
                "start_from" to startFrom,
                "count" to count,
                "fields" to fields
            ), base(NewsfeedResponse.serializer())
        )
    }
}