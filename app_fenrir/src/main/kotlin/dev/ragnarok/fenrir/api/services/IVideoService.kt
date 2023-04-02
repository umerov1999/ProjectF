package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IVideoService : IServiceRest() {
    fun getComments(
        ownerId: Long?,
        videoId: Int,
        needLikes: Int?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        sort: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>> {
        return rest.request(
            "video.getComments", form(
                "owner_id" to ownerId,
                "video_id" to videoId,
                "need_likes" to needLikes,
                "start_comment_id" to startCommentId,
                "offset" to offset,
                "count" to count,
                "sort" to sort,
                "extended" to extended,
                "fields" to fields
            ), base(DefaultCommentsResponse.serializer())
        )
    }

    fun addVideo(
        targetId: Long?,
        videoId: Int?,
        ownerId: Long?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.add", form(
                "target_id" to targetId,
                "video_id" to videoId,
                "owner_id" to ownerId
            ), baseInt
        )
    }

    fun deleteVideo(
        videoId: Int?,
        ownerId: Long?,
        targetId: Long?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.delete", form(
                "video_id" to videoId,
                "owner_id" to ownerId,
                "target_id" to targetId
            ), baseInt
        )
    }

    fun getAlbums(
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        extended: Int?,
        needSystem: Int?
    ): Single<BaseResponse<Items<VKApiVideoAlbum>>> {
        return rest.request(
            "video.getAlbums", form(
                "owner_id" to ownerId,
                "offset" to offset,
                "count" to count,
                "extended" to extended,
                "need_system" to needSystem
            ), items(VKApiVideoAlbum.serializer())
        )
    }

    fun getAlbumsByVideo(
        target_id: Long?,
        owner_id: Long?,
        video_id: Int?,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiVideoAlbum>>> {
        return rest.request(
            "video.getAlbumsByVideo", form(
                "target_id" to target_id,
                "owner_id" to owner_id,
                "video_id" to video_id,
                "extended" to extended
            ), items(VKApiVideoAlbum.serializer())
        )
    }

    fun search(
        query: String?,
        sort: Int?,
        hd: Int?,
        adult: Int?,
        filters: String?,
        searchOwn: Int?,
        offset: Int?,
        longer: Int?,
        shorter: Int?,
        count: Int?,
        extended: Int?
    ): Single<BaseResponse<SearchVideoResponse>> {
        return rest.request(
            "video.search", form(
                "q" to query,
                "sort" to sort,
                "hd" to hd,
                "adult" to adult,
                "filters" to filters,
                "search_own" to searchOwn,
                "offset" to offset,
                "longer" to longer,
                "shorter" to shorter,
                "count" to count,
                "extended" to extended
            ), base(SearchVideoResponse.serializer())
        )
    }

    fun restoreComment(
        ownerId: Long?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.restoreComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    fun deleteComment(
        ownerId: Long?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.deleteComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    operator fun get(
        ownerId: Long?,
        videos: String?,
        albumId: Int?,
        count: Int?,
        offset: Int?,
        extended: Int?
    ): Single<BaseResponse<Items<VKApiVideo>>> {
        return rest.request(
            "video.get", form(
                "owner_id" to ownerId,
                "videos" to videos,
                "album_id" to albumId,
                "count" to count,
                "offset" to offset,
                "extended" to extended
            ), items(VKApiVideo.serializer())
        )
    }

    fun createComment(
        ownerId: Long?,
        videoId: Int,
        message: String?,
        attachments: String?,
        fromGroup: Int?,
        replyToComment: Int?,
        stickerId: Int?,
        uniqueGeneratedId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.createComment", form(
                "owner_id" to ownerId,
                "video_id" to videoId,
                "message" to message,
                "attachments" to attachments,
                "from_group" to fromGroup,
                "reply_to_comment" to replyToComment,
                "sticker_id" to stickerId,
                "guid" to uniqueGeneratedId
            ), baseInt
        )
    }

    fun editComment(
        ownerId: Long?,
        commentId: Int,
        message: String?,
        attachments: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.editComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId,
                "message" to message,
                "attachments" to attachments
            ), baseInt
        )
    }

    fun edit(
        ownerId: Long?,
        video_id: Int,
        name: String?,
        desc: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "video.edit", form(
                "owner_id" to ownerId,
                "video_id" to video_id,
                "name" to name,
                "desc" to desc
            ), baseInt
        )
    }

    fun getVideoServer(
        is_private: Int?,
        group_id: Long?,
        name: String?
    ): Single<BaseResponse<VKApiVideosUploadServer>> {
        return rest.request(
            "video.save", form(
                "is_private" to is_private,
                "group_id" to group_id,
                "name" to name
            ), base(VKApiVideosUploadServer.serializer())
        )
    }
}