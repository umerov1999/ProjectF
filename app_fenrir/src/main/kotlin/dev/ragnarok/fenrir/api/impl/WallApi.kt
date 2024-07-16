package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IWallApi
import dev.ragnarok.fenrir.api.model.IdPair
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.PostsResponse
import dev.ragnarok.fenrir.api.model.response.RepostReponse
import dev.ragnarok.fenrir.api.model.response.WallResponse
import dev.ragnarok.fenrir.api.model.response.WallSearchResponse
import dev.ragnarok.fenrir.api.services.IWallService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class WallApi(accountId: Long, provider: IServiceProvider) : AbsApi(accountId, provider),
    IWallApi {
    override fun search(
        ownerId: Long,
        query: String?,
        ownersOnly: Boolean?,
        count: Int,
        offset: Int,
        extended: Boolean?,
        fields: String?
    ): Flow<WallSearchResponse> {
        return provideService(IWallService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.search(
                    ownerId, null, query, integerFromBoolean(ownersOnly),
                    count, offset, integerFromBoolean(extended), fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun edit(
        ownerId: Long?,
        postId: Int?,
        friendsOnly: Boolean?,
        message: String?,
        attachments: Collection<IAttachmentToken>?,
        services: String?,
        signed: Boolean?,
        publishDate: Long?,
        latitude: Double?,
        longitude: Double?,
        placeId: Int?,
        markAsAds: Boolean?
    ): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat { s ->
                s.edit(
                    ownerId,
                    postId,
                    integerFromBoolean(friendsOnly),
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) },
                    services,
                    integerFromBoolean(signed),
                    publishDate,
                    latitude,
                    longitude,
                    placeId,
                    integerFromBoolean(markAsAds)
                )
                    .map(extractResponseWithErrorHandling())
                    .map { it.postId != 0 }
            }
    }

    override fun pin(ownerId: Long?, postId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.pin(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun unpin(ownerId: Long?, postId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.unpin(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun repost(
        postOwnerId: Long,
        postId: Int,
        message: String?,
        groupId: Long?,
        markAsAds: Boolean?
    ): Flow<RepostReponse> {
        val obj = "wall" + postOwnerId + "_" + postId
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.repost(obj, message, groupId, integerFromBoolean(markAsAds))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun post(
        ownerId: Long?,
        friendsOnly: Boolean?,
        fromGroup: Boolean?,
        message: String?,
        attachments: Collection<IAttachmentToken>?,
        services: String?,
        signed: Boolean?,
        publishDate: Long?,
        latitude: Double?,
        longitude: Double?,
        placeId: Int?,
        postId: Int?,
        guid: Int?,
        markAsAds: Boolean?,
        adsPromotedStealth: Boolean?
    ): Flow<Int> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat { s ->
                s.post(
                    ownerId,
                    integerFromBoolean(friendsOnly),
                    integerFromBoolean(fromGroup),
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) },
                    services,
                    integerFromBoolean(signed),
                    publishDate,
                    latitude,
                    longitude,
                    placeId,
                    postId,
                    guid,
                    integerFromBoolean(markAsAds),
                    integerFromBoolean(adsPromotedStealth)
                )
                    .map(extractResponseWithErrorHandling())
                    .map { it.postId }
            }
    }

    override fun delete(ownerId: Long?, postId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.delete(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun restore(ownerId: Long?, postId: Int): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.restore(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun editComment(
        ownerId: Long?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat { s ->
                s.editComment(
                    ownerId,
                    commentId,
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) })
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun createComment(
        ownerId: Long?, postId: Int, fromGroup: Long?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, generatedUniqueId: Int?
    ): Flow<Int> {
        return provideService(IWallService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat { s ->
                s.createComment(
                    ownerId,
                    postId,
                    fromGroup,
                    message,
                    replyToComment,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) },
                    stickerId,
                    generatedUniqueId
                )
                    .map(extractResponseWithErrorHandling())
                    .map { it.commentId }
            }
    }

    override fun get(
        ownerId: Long?,
        domain: String?,
        offset: Int?,
        count: Int?,
        filter: String?,
        extended: Boolean?,
        fields: String?
    ): Flow<WallResponse> {
        return provideService(IWallService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it[ownerId, domain, offset, count, filter, if (extended != null) if (extended) 1 else 0 else null, fields]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(
        ids: Collection<IdPair>?,
        extended: Boolean?,
        copyHistoryDepth: Int?,
        fields: String?
    ): Flow<PostsResponse> {
        val line = join(ids, ",") { orig -> orig.ownerId.toString() + "_" + orig.id }
        return provideService(IWallService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getById(
                    line,
                    if (extended != null) if (extended) 1 else 0 else null,
                    copyHistoryDepth,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun reportPost(owner_id: Long?, post_id: Int?, reason: Int?): Flow<Int> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.reportPost(owner_id, post_id, reason)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun subscribe(owner_id: Long?): Flow<Int> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.subscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun unsubscribe(owner_id: Long?): Flow<Int> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.unsubscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun reportComment(owner_id: Long?, post_id: Int?, reason: Int?): Flow<Int> {
        return provideService(IWallService(), TokenType.USER)
            .flatMapConcat {
                it.reportComment(owner_id, post_id, reason)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getComments(
        ownerId: Long, postId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?,
        sort: String?, extended: Boolean?, fields: String?
    ): Flow<DefaultCommentsResponse> {
        return provideService(IWallService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getComments(
                    ownerId,
                    postId,
                    integerFromBoolean(needLikes),
                    startCommentId,
                    offset,
                    count,
                    sort,
                    integerFromBoolean(extended),
                    10,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }
}