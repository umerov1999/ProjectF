package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IVideoApi
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import dev.ragnarok.fenrir.api.services.IVideoService
import io.reactivex.rxjava3.core.Single

internal class VideoApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IVideoApi {
    override fun getComments(
        ownerId: Int?, videoId: Int, needLikes: Boolean?, startCommentId: Int?, offset: Int?,
        count: Int?, sort: String?, extended: Boolean?, fields: String?
    ): Single<DefaultCommentsResponse> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getComments(
                        ownerId,
                        videoId,
                        integerFromBoolean(needLikes),
                        startCommentId,
                        offset,
                        count,
                        sort,
                        integerFromBoolean(extended),
                        fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun addVideo(targetId: Int?, videoId: Int?, ownerId: Int?): Single<Int> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.addVideo(targetId, videoId, ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteVideo(videoId: Int?, ownerId: Int?, targetId: Int?): Single<Int> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteVideo(videoId, ownerId, targetId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbums(
        ownerId: Int?,
        offset: Int?,
        count: Int?,
        needSystem: Boolean?
    ): Single<Items<VKApiVideoAlbum>> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getAlbums(ownerId, offset, count, 1, integerFromBoolean(needSystem))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbumsByVideo(
        target_id: Int?,
        owner_id: Int?,
        video_id: Int?
    ): Single<Items<VKApiVideoAlbum>> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getAlbumsByVideo(target_id, owner_id, video_id, 1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?, sort: Int?, hd: Boolean?, adult: Boolean?,
        filters: String?, searchOwn: Boolean?, offset: Int?,
        longer: Int?, shorter: Int?, count: Int?, extended: Boolean?
    ): Single<SearchVideoResponse> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .search(
                        query,
                        sort,
                        integerFromBoolean(hd),
                        integerFromBoolean(adult),
                        filters,
                        integerFromBoolean(searchOwn),
                        offset,
                        longer,
                        shorter,
                        count,
                        integerFromBoolean(extended)
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun restoreComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun deleteComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override
    operator fun get(
        ownerId: Int?, ids: Collection<AccessIdPair>?, albumId: Int?,
        count: Int?, offset: Int?, extended: Boolean?
    ): Single<Items<VKApiVideo>> {
        val videos =
            join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IVideoService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service[ownerId, videos, albumId, count, offset, integerFromBoolean(extended)]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun createComment(
        ownerId: Int, videoId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        replyToComment: Int?, stickerId: Int?, uniqueGeneratedId: Int?
    ): Single<Int> {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .createComment(
                        ownerId, videoId, message, atts, integerFromBoolean(fromGroup),
                        replyToComment, stickerId, uniqueGeneratedId
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override
    fun editComment(
        ownerId: Int, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean> {
        return provideService(IVideoService::class.java, TokenType.USER)
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

    override
    fun edit(ownerId: Int, video_id: Int, name: String?, desc: String?): Single<Boolean> {
        return provideService(IVideoService::class.java, TokenType.USER)
            .flatMap { service ->
                service.edit(ownerId, video_id, name, desc)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }
}