package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IBoardService {
    //https://vk.com/dev/board.getComments
    @FormUrlEncoded
    @POST("board.getComments")
    fun getComments(
        @Field("group_id") groupId: Int,
        @Field("topic_id") topicId: Int,
        @Field("need_likes") needLikes: Int?,
        @Field("start_comment_id") startCommentId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("sort") sort: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>>

    //https://vk.com/dev/board.restoreComment
    @FormUrlEncoded
    @POST("board.restoreComment")
    fun restoreComment(
        @Field("group_id") groupId: Int,
        @Field("topic_id") topicId: Int,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/board.deleteComment
    @FormUrlEncoded
    @POST("board.deleteComment")
    fun deleteComment(
        @Field("group_id") groupId: Int,
        @Field("topic_id") topicId: Int,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Returns a list of topics on a community's discussion board.
     *
     * @param groupId       ID of the community that owns the discussion board.
     * @param topicIds      IDs of topics to be returned (100 maximum). By default, all topics are returned.
     * If this parameter is set, the order, offset, and count parameters are ignored.
     * List of comma-separated numbers
     * @param order         Sort order:
     * 1 — by date updated in reverse chronological order.
     * 2 — by date created in reverse chronological order.
     * -1 — by date updated in chronological order.
     * -2 — by date created in chronological order.
     * If no sort order is specified, topics are returned in the order specified by the group administrator.
     * Pinned topics are returned first, regardless of the sorting.
     * @param offset        Offset needed to return a specific subset of topics.
     * @param count         Number of topics to return.
     * @param extended      1 — to return information about users who created topics or who posted there last
     * 0 — to return no additional fields (default)
     * @param preview       1 — to return the first comment in each topic;
     * 2 — to return the last comment in each topic;
     * 0 — to return no comments.
     * By default: 0.
     * @param previewLength Number of characters after which to truncate the previewed comment.
     * To preview the full comment, specify 0. Default 90
     * @return array of objects describing topics.
     */
    @FormUrlEncoded
    @POST("board.getTopics")
    fun getTopics(
        @Field("group_id") groupId: Int,
        @Field("topic_ids") topicIds: String?,
        @Field("order") order: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("preview") preview: Int?,
        @Field("preview_length") previewLength: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<TopicsResponse>> // not doccumented

    /**
     * Edits a comment on a topic on a community's discussion board.
     *
     * @param groupId     ID of the community that owns the discussion board.
     * @param topicId     Topic ID.
     * @param commentId   ID of the comment on the topic.
     * @param message     (Required if attachments is not set). New comment text.
     * @param attachments (Required if message is not set.) List of objects attached to the post, in the following format:
     * {type}{owner_id}_{media_id},{type}{owner_id}_{media_id}
     * {type} — Type of media attachment:
     * photo — photo
     * video — video
     * audio — audio
     * doc — document
     * {owner_id} — Media attachment owner ID.
     * {media_id} — Media attachment ID.
     * Example:
     * photo100172_166443618,photo66748_265827614
     * List of comma-separated words
     * @return 1
     */
    @FormUrlEncoded
    @POST("board.editComment")
    fun editComment(
        @Field("group_id") groupId: Int,
        @Field("topic_id") topicId: Int,
        @Field("comment_id") commentId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("board.addComment")
    fun addComment(
        @Field("group_id") groupId: Int?,
        @Field("topic_id") topicId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?,
        @Field("from_group") fromGroup: Int?,
        @Field("sticker_id") stickerId: Int?,
        @Field("guid") generatedUniqueId: Int?
    ): Single<BaseResponse<Int>>
}