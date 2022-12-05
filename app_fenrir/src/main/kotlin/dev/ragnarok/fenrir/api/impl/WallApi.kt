package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IWallApi
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.api.model.IdPair
import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.services.IWallService
import io.reactivex.rxjava3.core.Single

internal class WallApi(accountId: Int, provider: IServiceProvider) : AbsApi(accountId, provider),
    IWallApi {
    override fun search(
        ownerId: Int,
        query: String?,
        ownersOnly: Boolean?,
        count: Int,
        offset: Int,
        extended: Boolean?,
        fields: String?
    ): Single<WallSearchResponse> {
        return provideService(IWallService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .search(
                        ownerId, null, query, integerFromBoolean(ownersOnly),
                        count, offset, integerFromBoolean(extended), fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun edit(
        ownerId: Int?,
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
    ): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .edit(
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
                    .map { response -> response.postId != 0 }
            }
    }

    override fun pin(ownerId: Int?, postId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.pin(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun unpin(ownerId: Int?, postId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.unpin(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun repost(
        postOwnerId: Int,
        postId: Int,
        message: String?,
        groupId: Int?,
        markAsAds: Boolean?
    ): Single<RepostReponse> {
        val `object` = "wall" + postOwnerId + "_" + postId
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.repost(`object`, message, groupId, integerFromBoolean(markAsAds))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun post(
        ownerId: Int?,
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
    ): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .post(
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
                    .map { response -> response.postId }
            }
    }

    override fun delete(ownerId: Int?, postId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.delete(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun restoreComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun deleteComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun restore(ownerId: Int?, postId: Int): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.restore(ownerId, postId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun editComment(
        ownerId: Int?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service.editComment(
                    ownerId,
                    commentId,
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) })
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun createComment(
        ownerId: Int?, postId: Int, fromGroup: Int?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, generatedUniqueId: Int?
    ): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .createComment(
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
                    .map { response -> response.commentId }
            }
    }

    override fun get(
        ownerId: Int?,
        domain: String?,
        offset: Int?,
        count: Int?,
        filter: String?,
        extended: Boolean?,
        fields: String?
    ): Single<WallResponse> {
        return provideService(IWallService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service[ownerId, domain, offset, count, filter, if (extended != null) if (extended) 1 else 0 else null, fields]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(
        ids: Collection<IdPair>?,
        extended: Boolean?,
        copyHistoryDepth: Int?,
        fields: String?
    ): Single<PostsResponse> {
        val line = join(ids, ",") { orig -> orig.ownerId.toString() + "_" + orig.id }
        return provideService(IWallService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getById(
                        line,
                        if (extended != null) if (extended) 1 else 0 else null,
                        copyHistoryDepth,
                        fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun reportPost(owner_id: Int?, post_id: Int?, reason: Int?): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .reportPost(owner_id, post_id, reason)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun subscribe(owner_id: Int?): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .subscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun unsubscribe(owner_id: Int?): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .unsubscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun reportComment(owner_id: Int?, post_id: Int?, reason: Int?): Single<Int> {
        return provideService(IWallService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .reportComment(owner_id, post_id, reason)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getComments(
        ownerId: Int, postId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?,
        sort: String?, extended: Boolean?, fields: String?
    ): Single<DefaultCommentsResponse> {
        return provideService(IWallService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getComments(
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