package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IBoardApi
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import dev.ragnarok.fenrir.api.services.IBoardService
import dev.ragnarok.fenrir.requireNonNull
import io.reactivex.rxjava3.core.Single

internal class BoardApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IBoardApi {
    override fun getComments(
        groupId: Int,
        topicId: Int,
        needLikes: Boolean?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        extended: Boolean?,
        sort: String?,
        fields: String?
    ): Single<DefaultCommentsResponse> {
        return provideService(IBoardService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getComments(
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

    override fun restoreComment(groupId: Int, topicId: Int, commentId: Int): Single<Boolean> {
        return provideService(IBoardService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.restoreComment(groupId, topicId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun deleteComment(groupId: Int, topicId: Int, commentId: Int): Single<Boolean> {
        return provideService(IBoardService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.deleteComment(groupId, topicId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun getTopics(
        groupId: Int, topicIds: Collection<Int>?, order: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        preview: Int?, previewLength: Int?, fields: String?
    ): Single<TopicsResponse> {
        return provideService(IBoardService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getTopics(
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
        groupId: Int, topicId: Int, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean> {
        return provideService(IBoardService::class.java, TokenType.USER)
            .flatMap { service ->
                service.editComment(
                    groupId,
                    topicId,
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

    override fun addComment(
        groupId: Int?,
        topicId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?,
        fromGroup: Boolean?,
        stickerId: Int?,
        generatedUniqueId: Int?
    ): Single<Int> {
        return provideService(IBoardService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .addComment(
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