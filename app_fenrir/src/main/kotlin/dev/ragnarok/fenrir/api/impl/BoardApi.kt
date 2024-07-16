package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IBoardApi
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import dev.ragnarok.fenrir.api.services.IBoardService
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class BoardApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IBoardApi {
    override fun getComments(
        groupId: Long,
        topicId: Int,
        needLikes: Boolean?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        extended: Boolean?,
        sort: String?,
        fields: String?
    ): Flow<DefaultCommentsResponse> {
        return provideService(IBoardService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getComments(
                    groupId,
                    topicId,
                    integerFromBoolean(needLikes),
                    startCommentId,
                    offset,
                    count,
                    integerFromBoolean(extended),
                    sort,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun restoreComment(groupId: Long, topicId: Int, commentId: Int): Flow<Boolean> {
        return provideService(IBoardService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.restoreComment(groupId, topicId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun deleteComment(groupId: Long, topicId: Int, commentId: Int): Flow<Boolean> {
        return provideService(IBoardService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.deleteComment(groupId, topicId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun getTopics(
        groupId: Long, topicIds: Collection<Int>?, order: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        preview: Int?, previewLength: Int?, fields: String?
    ): Flow<TopicsResponse> {
        return provideService(IBoardService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat { s ->
                s.getTopics(
                    groupId,
                    join(topicIds, ","),
                    order,
                    offset,
                    count,
                    integerFromBoolean(extended),
                    preview,
                    previewLength,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
                    .map { response ->
                        // fix (не приходит owner_id)
                        response.items.requireNonNull {
                            for (topic in it) {
                                topic.owner_id = -groupId
                            }
                        }
                        response
                    }
            }
    }

    override fun editComment(
        groupId: Long, topicId: Int, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean> {
        return provideService(IBoardService(), TokenType.USER)
            .flatMapConcat { s ->
                s.editComment(
                    groupId,
                    topicId,
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

    override fun addComment(
        groupId: Long?,
        topicId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?,
        fromGroup: Boolean?,
        stickerId: Int?,
        generatedUniqueId: Int?
    ): Flow<Int> {
        return provideService(IBoardService(), TokenType.USER)
            .flatMapConcat { s ->
                s.addComment(
                    groupId,
                    topicId,
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) },
                    integerFromBoolean(fromGroup),
                    stickerId,
                    generatedUniqueId
                )
                    .map(extractResponseWithErrorHandling())
            }
    }
}