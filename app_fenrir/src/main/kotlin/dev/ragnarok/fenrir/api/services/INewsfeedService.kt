package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFeedList
import dev.ragnarok.fenrir.api.model.response.*
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface INewsfeedService {
    /**
     * filters post, photo, video, topic, market, note
     */
    //https://vk.com/dev/newsfeed.getComments
    @FormUrlEncoded
    @POST("newsfeed.getComments")
    fun getComments(
        @Field("count") count: Int?,
        @Field("filters") filters: String?,
        @Field("reposts") reposts: String?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("last_comments_count") lastCommentsCount: Int?,
        @Field("start_from") startFrom: String?,
        @Field("fields") fields: String?,
        @Field("photo_sizes") photoSizes: Int?
    ): Single<BaseResponse<NewsfeedCommentsResponse>>

    //https://vk.com/dev/newsfeed.getMentions
    @FormUrlEncoded
    @POST("newsfeed.getMentions")
    fun getMentions(
        @Field("owner_id") owner_id: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?
    ): Single<BaseResponse<NewsfeedCommentsResponse>>

    //https://vk.com/dev/newsfeed.getLists
    @FormUrlEncoded
    @POST("newsfeed.getLists")
    fun getLists(
        @Field("list_ids") listIds: String?,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiFeedList>>>

    //https://vk.com/dev/newsfeed.saveList
    @FormUrlEncoded
    @POST("newsfeed.saveList")
    fun saveList(
        @Field("title") title: String?,
        @Field("source_ids") source_ids: String?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/newsfeed.getBanned
    @FormUrlEncoded
    @POST("newsfeed.getBanned")
    fun getBanned(
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedBanResponse>>

    //https://vk.com/dev/newsfeed.deleteBan
    @FormUrlEncoded
    @POST("newsfeed.deleteBan")
    fun deleteBan(
        @Field("user_ids") user_ids: String?,
        @Field("group_ids") group_ids: String?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/newsfeed.addBan
    @FormUrlEncoded
    @POST("newsfeed.addBan")
    fun addBan(
        @Field("user_ids") user_ids: String?,
        @Field("group_ids") group_ids: String?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/newsfeed.ignoreItem
    @FormUrlEncoded
    @POST("newsfeed.ignoreItem")
    fun ignoreItem(
        @Field("type") type: String?,
        @Field("owner_id") owner_id: Int?,
        @Field("item_id") item_id: Int?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/newsfeed.deleteList
    @FormUrlEncoded
    @POST("newsfeed.deleteList")
    fun deleteList(@Field("list_id") list_id: Int?): Single<BaseResponse<Int>>

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
    @FormUrlEncoded
    @POST("newsfeed.search")
    fun search(
        @Field("q") query: String?,
        @Field("extended") extended: Int?,
        @Field("count") count: Int?,
        @Field("latitude") latitude: Double?,
        @Field("longitude") longitude: Double?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("start_from") startFrom: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedSearchResponse>>

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
    @FormUrlEncoded
    @POST("newsfeed.get")
    operator fun get(
        @Field("filters") filters: String?,
        @Field("return_banned") returnBanned: Int?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("max_photos") maxPhotoCount: Int?,
        @Field("source_ids") sourceIds: String?,
        @Field("start_from") startFrom: String?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedResponse>>

    @FormUrlEncoded
    @POST("newsfeed.getByType")
    fun getByType(
        @Field("feed_type") feed_type: String,
        @Field("filters") filters: String?,
        @Field("return_banned") returnBanned: Int?,
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("max_photos") maxPhotoCount: Int?,
        @Field("source_ids") sourceIds: String?,
        @Field("start_from") startFrom: String?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedResponse>>

    @FormUrlEncoded
    @POST("newsfeed.getRecommended")
    fun getRecommended(
        @Field("start_time") startTime: Long?,
        @Field("end_time") endTime: Long?,
        @Field("max_photos") maxPhotoCount: Int?,
        @Field("start_from") startFrom: String?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedResponse>>

    @FormUrlEncoded
    @POST("execute.getFeedLikes")
    fun getFeedLikes(
        @Field("max_photos") maxPhotoCount: Int?,
        @Field("start_from") startFrom: String?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<NewsfeedResponse>>
}