package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IWallService : IServiceRest() {
    //https://vk.com/dev/wall.search
    fun search(
        ownerId: Int?,
        domain: String?,
        query: String?,
        ownersOnly: Int?,
        count: Int?,
        offset: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<WallSearchResponse>> {
        return rest.request(
            "wall.search", form(
                "owner_id" to ownerId,
                "domain" to domain,
                "query" to query,
                "owners_only" to ownersOnly,
                "count" to count,
                "offset" to offset,
                "extended" to extended,
                "fields" to fields
            ), base(WallSearchResponse.serializer())
        )
    }

    //https://vk.com/dev/wall.edit
    fun edit(
        ownerId: Int?,
        postId: Int?,
        friendsOnly: Int?,
        message: String?,
        attachments: String?,
        services: String?,
        signed: Int?,
        publishDate: Long?,
        latitude: Double?,
        longitude: Double?,
        placeId: Int?,
        markAsAds: Int?
    ): Single<BaseResponse<WallEditResponse>> {
        return rest.request(
            "wall.edit", form(
                "owner_id" to ownerId,
                "post_id" to postId,
                "friends_only" to friendsOnly,
                "message" to message,
                "attachments" to attachments,
                "services" to services,
                "signed" to signed,
                "publish_date" to publishDate,
                "lat" to latitude,
                "long" to longitude,
                "place_id" to placeId,
                "mark_as_ads" to markAsAds
            ), base(WallEditResponse.serializer())
        )
    }

    //https://vk.com/dev/wall.pin
    fun pin(
        ownerId: Int?,
        postId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.pin", form(
                "owner_id" to ownerId,
                "post_id" to postId
            ), baseInt
        )
    }

    //https://vk.com/dev/wall.unpin
    fun unpin(
        ownerId: Int?,
        postId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.unpin", form(
                "owner_id" to ownerId,
                "post_id" to postId
            ), baseInt
        )
    }

    //https://vk.com/dev/wall.repost
    fun repost(
        pobject: String?,
        message: String?,
        groupId: Int?,
        markAsAds: Int?
    ): Single<BaseResponse<RepostReponse>> {
        return rest.request(
            "wall.repost", form(
                "object" to pobject,
                "message" to message,
                "group_id" to groupId,
                "mark_as_ads" to markAsAds
            ), base(RepostReponse.serializer())
        )
    }

    //https://vk.com/dev/wall.post
    fun post(
        ownerId: Int?,
        friendsOnly: Int?,
        fromGroup: Int?,
        message: String?,
        attachments: String?,
        services: String?,
        signed: Int?,
        publishDate: Long?,
        latitude: Double?,
        longitude: Double?,
        placeId: Int?,
        postId: Int?,
        guid: Int?,
        markAsAds: Int?,
        adsPromotedStealth: Int?
    ): Single<BaseResponse<PostCreateResponse>> {
        return rest.request(
            "wall.post", form(
                "owner_id" to ownerId,
                "friends_only" to friendsOnly,
                "from_group" to fromGroup,
                "message" to message,
                "attachments" to attachments,
                "services" to services,
                "signed" to signed,
                "publish_date" to publishDate,
                "lat" to latitude,
                "long" to longitude,
                "place_id" to placeId,
                "post_id" to postId,
                "guid" to guid,
                "mark_as_ads" to markAsAds,
                "ads_promoted_stealth" to adsPromotedStealth
            ), base(PostCreateResponse.serializer())
        )
    }

    /**
     * Deletes a post from a user wall or community wall.
     *
     * @param ownerId User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param postId  ID of the post to be deleted
     * @return 1
     */
    fun delete(
        ownerId: Int?,
        postId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.delete", form(
                "owner_id" to ownerId,
                "post_id" to postId
            ), baseInt
        )
    }

    /**
     * Restores a comment deleted from a user wall or community wall.
     *
     * @param ownerId   User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId Comment ID.
     * @return 1
     */
    fun restoreComment(
        ownerId: Int?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.restoreComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    /**
     * Deletes a comment on a post on a user wall or community wall.
     *
     * @param ownerId   User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId Comment ID.
     * @return 1
     */
    fun deleteComment(
        ownerId: Int?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.deleteComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    /**
     * Restores a post deleted from a user wall or community wall.
     *
     * @param ownerId User ID or community ID from whose wall the post was deleted.
     * Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param postId  ID of the post to be restored.
     * @return 1
     */
    fun restore(
        ownerId: Int?,
        postId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.restore", form(
                "owner_id" to ownerId,
                "post_id" to postId
            ), baseInt
        )
    }

    /**
     * Edits a comment on a user wall or community wall.
     *
     * @param ownerId     User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId   Comment ID.
     * @param message     New comment text.
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
        ownerId: Int?,
        commentId: Int,
        message: String?,
        attachments: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.editComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId,
                "message" to message,
                "attachments" to attachments
            ), baseInt
        )
    }

    fun createComment(
        ownerId: Int?,
        postId: Int,
        fromGroup: Int?,
        message: String?,
        replyToComment: Int?,
        attachments: String?,
        stickerId: Int?,
        generatedUniqueId: Int?
    ): Single<BaseResponse<CommentCreateResponse>> {
        return rest.request(
            "wall.createComment", form(
                "owner_id" to ownerId,
                "post_id" to postId,
                "from_group" to fromGroup,
                "message" to message,
                "reply_to_comment" to replyToComment,
                "attachments" to attachments,
                "sticker_id" to stickerId,
                "guid" to generatedUniqueId
            ), base(CommentCreateResponse.serializer())
        )
    }

    //https://vk.com/dev/wall.getComments
    fun getComments(
        ownerId: Int?,
        postId: Int,
        needLikes: Int?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        sort: String?,
        extended: Int?,
        thread_items_count: Int?,
        fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>> {
        return rest.request(
            "wall.getComments", form(
                "owner_id" to ownerId,
                "post_id" to postId,
                "need_likes" to needLikes,
                "start_comment_id" to startCommentId,
                "offset" to offset,
                "count" to count,
                "sort" to sort,
                "extended" to extended,
                "thread_items_count" to thread_items_count,
                "fields" to fields
            ), base(DefaultCommentsResponse.serializer())
        )
    }

    operator fun get(
        ownerId: Int?,
        domain: String?,
        offset: Int?,
        count: Int?,
        filter: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<WallResponse>> {
        return rest.request(
            "wall.get", form(
                "owner_id" to ownerId,
                "domain" to domain,
                "offset" to offset,
                "count" to count,
                "filter" to filter,
                "extended" to extended,
                "fields" to fields
            ), base(WallResponse.serializer())
        )
    }

    fun getById(
        ids: String?,
        extended: Int?,
        copyHistoryDepth: Int?,
        fields: String?
    ): Single<BaseResponse<PostsResponse>> {
        return rest.request(
            "wall.getById", form(
                "posts" to ids,
                "extended" to extended,
                "copy_history_depth" to copyHistoryDepth,
                "fields" to fields
            ), base(PostsResponse.serializer())
        )
    }

    fun reportComment(
        owner_id: Int?,
        comment_id: Int?,
        reason: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.reportComment", form(
                "owner_id" to owner_id,
                "comment_id" to comment_id,
                "reason" to reason
            ), baseInt
        )
    }

    fun reportPost(
        owner_id: Int?,
        post_id: Int?,
        reason: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "wall.reportPost", form(
                "owner_id" to owner_id,
                "post_id" to post_id,
                "reason" to reason
            ), baseInt
        )
    }

    fun subscribe(owner_id: Int?): Single<BaseResponse<Int>> {
        return rest.request("wall.subscribe", form("owner_id" to owner_id), baseInt)
    }

    fun unsubscribe(owner_id: Int?): Single<BaseResponse<Int>> {
        return rest.request("wall.unsubscribe", form("owner_id" to owner_id), baseInt)
    }
}