package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IVideoApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import dev.ragnarok.fenrir.api.services.IVideoService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class VideoApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IVideoApi {
    override fun getComments(
        ownerId: Long?, videoId: Int, needLikes: Boolean?, startCommentId: Int?, offset: Int?,
        count: Int?, sort: String?, extended: Boolean?, fields: String?
    ): Flow<DefaultCommentsResponse> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.getComments(
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

    override fun addVideo(targetId: Long?, videoId: Int?, ownerId: Long?): Flow<Int> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.addVideo(targetId, videoId, ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteVideo(videoId: Int?, ownerId: Long?, targetId: Long?): Flow<Int> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.deleteVideo(videoId, ownerId, targetId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbums(
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        needSystem: Boolean?
    ): Flow<Items<VKApiVideoAlbum>> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.getAlbums(ownerId, offset, count, 1, integerFromBoolean(needSystem))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbumsByVideo(
        target_id: Long?,
        owner_id: Long?,
        video_id: Int?
    ): Flow<Items<VKApiVideoAlbum>> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.getAlbumsByVideo(target_id, owner_id, video_id, 1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?, sort: Int?, hd: Boolean?, adult: Boolean?,
        filters: String?, searchOwn: Boolean?, offset: Int?,
        longer: Int?, shorter: Int?, count: Int?, extended: Boolean?
    ): Flow<SearchVideoResponse> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.search(
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

    override fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override
    operator fun get(
        ownerId: Long?, ids: Collection<AccessIdPair>?, albumId: Int?,
        count: Int?, offset: Int?, extended: Boolean?
    ): Flow<Items<VKApiVideo>> {
        val videos =
            join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IVideoService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it[ownerId, videos, albumId, count, offset, integerFromBoolean(extended)]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun createComment(
        ownerId: Long, videoId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        replyToComment: Int?, stickerId: Int?, uniqueGeneratedId: Int?
    ): Flow<Int> {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.createComment(
                    ownerId, videoId, message, atts, integerFromBoolean(fromGroup),
                    replyToComment, stickerId, uniqueGeneratedId
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override
    fun editComment(
        ownerId: Long, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean> {
        return provideService(IVideoService(), TokenType.USER)
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

    override
    fun edit(ownerId: Long, video_id: Int, name: String?, desc: String?): Flow<Boolean> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat {
                it.edit(ownerId, video_id, name, desc)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun getVideoServer(
        isPrivate: Int?,
        group_id: Long?,
        name: String?
    ): Flow<VKApiVideosUploadServer> {
        return provideService(IVideoService(), TokenType.USER)
            .flatMapConcat { s ->
                var finalName = name
                if (finalName?.startsWith("VID_", false) == true) {
                    finalName = "Telegram $finalName"
                } else if (finalName?.startsWith("VID-", false) == true && finalName.contains(
                        "-WA",
                        false
                    )
                ) {
                    finalName = "WhatsApp $finalName"
                }
                s.getVideoServer(isPrivate, group_id, finalName)
                    .map(extractResponseWithErrorHandling())
            }
    }
}