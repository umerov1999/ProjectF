package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IBoardService : IServiceRest() {
    //https://vk.com/dev/board.getComments
    fun getComments(
        groupId: Long,
        topicId: Int,
        needLikes: Int?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        extended: Int?,
        sort: String?,
        fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>> {
        return rest.request(
            "board.getComments",
            form(
                "group_id" to groupId,
                "topic_id" to topicId,
                "need_likes" to needLikes,
                "start_comment_id" to startCommentId,
                "offset" to offset,
                "count" to count,
                "extended" to extended,
                "sort" to sort,
                "fields" to fields
            ), base(DefaultCommentsResponse.serializer())
        )
    }

    //https://vk.com/dev/board.restoreComment
    fun restoreComment(
        groupId: Long,
        topicId: Int,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "board.restoreComment",
            form("group_id" to groupId, "topic_id" to topicId, "comment_id" to commentId),
            baseInt
        )
    }

    //https://vk.com/dev/board.deleteComment
    fun deleteComment(
        groupId: Long,
        topicId: Int,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "board.deleteComment",
            form("group_id" to groupId, "topic_id" to topicId, "comment_id" to commentId),
            baseInt
        )
    }

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
    fun getTopics(
        groupId: Long,
        topicIds: String?,
        order: Int?,
        offset: Int?,
        count: Int?,
        extended: Int?,
        preview: Int?,
        previewLength: Int?,
        fields: String?
    ): Single<BaseResponse<TopicsResponse>> {
        return rest.request(
            "board.getTopics",
            form(
                "group_id" to groupId,
                "topic_ids" to topicIds,
                "order" to order,
                "offset" to offset,
                "count" to count,
                "extended" to extended,
                "preview" to preview,
                "preview_length" to previewLength,
                "fields" to fields
            ), base(TopicsResponse.serializer())
        )
    }

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
    fun editComment(
        groupId: Long,
        topicId: Int,
        commentId: Int,
        message: String?,
        attachments: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "board.editComment",
            form(
                "group_id" to groupId,
                "topic_id" to topicId,
                "comment_id" to commentId,
                "message" to message,
                "attachments" to attachments
            ), baseInt
        )
    }

    fun addComment(
        groupId: Long?,
        topicId: Int,
        message: String?,
        attachments: String?,
        fromGroup: Int?,
        stickerId: Int?,
        generatedUniqueId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "board.addComment",
            form(
                "group_id" to groupId,
                "topic_id" to topicId,
                "message" to message,
                "attachments" to attachments,
                "from_group" to fromGroup,
                "sticker_id" to stickerId,
                "guid" to generatedUniqueId
            ), baseInt
        )
    }
}