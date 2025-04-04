package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.domain.ICommentsInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapComment
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildComment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Entity2Dto.createToken
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommentFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillCommentOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Model2Dto.createTokens
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.CommentIntent
import dev.ragnarok.fenrir.model.CommentUpdate
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.CommentsBundle
import dev.ragnarok.fenrir.model.DraftComment
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.criteria.CommentsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyListFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.repeatUntil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlin.math.abs

class CommentsInteractor(
    private val networker: INetworker,
    private val cache: IStorages,
    private val ownersRepository: IOwnersRepository
) : ICommentsInteractor {
    override fun getAllCachedData(accountId: Long, commented: Commented): Flow<List<Comment>> {
        val criteria = CommentsCriteria(accountId, commented)
        return cache.comments()
            .getDbosByCriteria(criteria)
            .flatMapConcat { dbos ->
                val ownids = VKOwnIds()
                for (c in dbos) {
                    fillCommentOwnerIds(ownids, c)
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(accountId, ownids.all, IOwnersRepository.MODE_ANY)
                    .map {
                        val comments: MutableList<Comment> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            buildCommentFromDbo(dbo, it)?.let { it1 -> comments.add(it1) }
                        }
                        comments
                    }
            }
    }

    private fun cacheData(
        accountId: Long,
        commented: Commented,
        data: List<CommentEntity>,
        owners: OwnerEntities,
        invalidateCache: Boolean
    ): Flow<Boolean> {
        val sourceId = commented.sourceId
        val ownerId = commented.sourceOwnerId
        val type = commented.sourceType
        return cache.comments()
            .insert(accountId, sourceId, ownerId, type, data, owners, invalidateCache)
            .ignoreElement()
    }

    private fun transform(
        accountId: Long,
        commented: Commented,
        comments: List<VKApiComment>,
        users: Collection<VKApiUser>?,
        groups: Collection<VKApiCommunity>?
    ): Flow<ArrayList<Comment>> {
        val ownids = VKOwnIds()
        for (dto in comments) {
            ownids.append(dto)
        }
        return ownersRepository
            .findBaseOwnersDataAsBundle(
                accountId,
                ownids.all,
                IOwnersRepository.MODE_ANY,
                transformOwners(users, groups)
            )
            .map {
                val data: ArrayList<Comment> = ArrayList(comments.size)
                for (dto in comments) {
                    val cm = buildComment(commented, dto, it)
                    data.add(cm)
                }
                data.sortWith { o1, o2 ->
                    o2.getObjectId().compareTo(o1.getObjectId())
                }
                data
            }
    }

    override fun getCommentsNoCache(
        accountId: Long,
        ownerId: Long,
        postId: Int,
        offset: Int
    ): Flow<List<Comment>> {
        return networker.vkDefault(accountId)
            .comments()["post", ownerId, postId, offset, 100, "desc", null, null, null, Fields.FIELDS_BASE_OWNER]
            .flatMapConcat { response ->
                val commentDtos =
                    response.main?.comments.orEmpty()
                val users =
                    response.main?.profiles.orEmpty()
                val groups =
                    response.main?.groups.orEmpty()
                transform(
                    accountId,
                    Commented(postId, ownerId, CommentedType.POST, null),
                    commentDtos,
                    users,
                    groups
                )
            }
    }

    override fun getCommentsPortion(
        accountId: Long,
        commented: Commented,
        offset: Int,
        count: Int,
        startCommentId: Int?,
        threadComment: Int?,
        invalidateCache: Boolean,
        sort: String?
    ): Flow<CommentsBundle> {
        val type = commented.typeForStoredProcedure
        return networker.vkDefault(accountId)
            .comments()[type, commented.sourceOwnerId, commented.sourceId, offset, count, sort, startCommentId, threadComment, commented.accessKey, Fields.FIELDS_BASE_OWNER]
            .flatMapConcat { response ->
                val commentDtos =
                    response.main?.comments.orEmpty()
                val users =
                    response.main?.profiles.orEmpty()
                val groups =
                    response.main?.groups.orEmpty()
                val modelsSingle = transform(accountId, commented, commentDtos, users, groups)
                val dbos: MutableList<CommentEntity> = ArrayList(commentDtos.size)
                for (dto in commentDtos) dbos.add(
                    mapComment(
                        commented.sourceId,
                        commented.sourceOwnerId,
                        commented.sourceType,
                        commented.accessKey,
                        dto
                    )
                )
                if (threadComment != null) {
                    modelsSingle.map { data ->
                        val bundle = CommentsBundle(data)
                            .setAdminLevel(response.admin_level)
                            .setFirstCommentId(response.firstId)
                            .setLastCommentId(response.lastId)
                        response.main?.poll.requireNonNull {
                            val poll = transform(it)
                            poll.setBoard(true) // так как это может быть только из топика
                            bundle.setTopicPoll(poll)
                        }
                        bundle
                    }
                } else {
                    cacheData(accountId, commented, dbos, mapOwners(users, groups), invalidateCache)
                        .andThen(modelsSingle.map { data ->
                            val bundle = CommentsBundle(data)
                                .setAdminLevel(response.admin_level)
                                .setFirstCommentId(response.firstId)
                                .setLastCommentId(response.lastId)
                            response.main?.poll.requireNonNull {
                                val poll = transform(it)
                                poll.setBoard(true) // так как это может быть только из топика
                                bundle.setTopicPoll(poll)
                            }
                            bundle
                        })
                }
            }
    }

    override fun restoreDraftComment(accountId: Long, commented: Commented): Flow<DraftComment?> {
        return cache.comments()
            .findEditingComment(accountId, commented)
    }

    override fun safeDraftComment(
        accountId: Long,
        commented: Commented,
        text: String?,
        replyToCommentId: Int,
        replyToUserId: Long
    ): Flow<Int> {
        return cache.comments()
            .saveDraftComment(accountId, commented, text, replyToUserId, replyToCommentId)
    }

    override fun isLiked(accountId: Long, commented: Commented, commentId: Int): Flow<Boolean> {
        val type: String = when (commented.sourceType) {
            CommentedType.PHOTO -> "photo_comment"
            CommentedType.POST -> "comment"
            CommentedType.VIDEO -> "video_comment"
            CommentedType.TOPIC -> "topic_comment"
            else -> throw IllegalArgumentException()
        }
        return networker.vkDefault(accountId)
            .likes()
            .isLiked(type, commented.sourceOwnerId, commentId)
    }

    override fun checkAndAddLike(
        accountId: Long,
        commented: Commented,
        commentId: Int
    ): Flow<Int> {
        val type: String = when (commented.sourceType) {
            CommentedType.PHOTO -> "photo_comment"
            CommentedType.POST -> "comment"
            CommentedType.VIDEO -> "video_comment"
            CommentedType.TOPIC -> "topic_comment"
            else -> throw IllegalArgumentException()
        }
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike(type, commented.sourceOwnerId, commentId, commented.accessKey)
    }

    override fun like(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        add: Boolean
    ): Flow<Boolean> {
        val type: String = when (commented.sourceType) {
            CommentedType.PHOTO -> "photo_comment"
            CommentedType.POST -> "comment"
            CommentedType.VIDEO -> "video_comment"
            CommentedType.TOPIC -> "topic_comment"
            else -> throw IllegalArgumentException()
        }
        val api = networker.vkDefault(accountId).likes()
        val update = CommentUpdate.create(accountId, commented, commentId)
        return if (add) {
            api.add(type, commented.sourceOwnerId, commentId, commented.accessKey)
                .flatMapConcat { count ->
                    update.withLikes(true, count)
                    cache.comments().commitMinorUpdate(update)
                }
        } else {
            api.delete(type, commented.sourceOwnerId, commentId, commented.accessKey)
                .flatMapConcat { count ->
                    update.withLikes(false, count)
                    cache.comments().commitMinorUpdate(update)
                }
        }
    }

    override fun deleteRestore(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        delete: Boolean
    ): Flow<Boolean> {
        val apis = networker.vkDefault(accountId)
        val ownerId = commented.sourceOwnerId
        val update = CommentUpdate.create(accountId, commented, commentId)
            .withDeletion(delete)
        val single: Flow<Boolean> = when (commented.sourceType) {
            CommentedType.PHOTO -> {
                val photosApi = apis.photos()
                if (delete) {
                    photosApi.deleteComment(ownerId, commentId)
                } else {
                    photosApi.restoreComment(ownerId, commentId)
                }
            }

            CommentedType.POST -> {
                val wallApi = apis.wall()
                if (delete) {
                    wallApi.deleteComment(ownerId, commentId)
                } else {
                    wallApi.restoreComment(ownerId, commentId)
                }
            }

            CommentedType.VIDEO -> {
                val videoApi = apis.video()
                if (delete) {
                    videoApi.deleteComment(ownerId, commentId)
                } else {
                    videoApi.restoreComment(ownerId, commentId)
                }
            }

            CommentedType.TOPIC -> {
                val groupId = abs(ownerId)
                val topicId = commented.sourceId
                val boardApi = apis.board()
                if (delete) {
                    boardApi.deleteComment(groupId, topicId, commentId)
                } else {
                    boardApi.restoreComment(groupId, topicId, commentId)
                }
            }

            else -> throw UnsupportedOperationException()
        }
        return single.flatMapConcat {
            cache
                .comments()
                .commitMinorUpdate(update)
        }
    }

    override fun send(
        accountId: Long,
        commented: Commented,
        commentThread: Int?,
        intent: CommentIntent
    ): Flow<Comment> {
        val cachedAttachments: Flow<List<IAttachmentToken>> =
            intent.draftMessageId.requireNonNull({
                getCachedAttachmentsToken(accountId, it)
            }, {
                emptyListFlow()
            })
        return cachedAttachments
            .flatMapConcat { cachedTokens ->
                val tokens: MutableList<IAttachmentToken> = ArrayList()
                tokens.addAll(cachedTokens)
                intent.models.nonNullNoEmpty {
                    tokens.addAll(createTokens(it))
                }
                sendComment(accountId, commented, intent, tokens)
                    .flatMapConcat { id ->
                        getCommentByIdAndStore(
                            accountId,
                            commented,
                            id,
                            commentThread,
                            true
                        )
                    }
                    .map { comment ->
                        intent.draftMessageId.requireNonNull({
                            cache.comments()
                                .deleteByDbid(accountId, it).single()
                            comment
                        }, {
                            comment
                        })
                    }
            }
    }

    private fun getCachedAttachmentsToken(
        accountId: Long,
        commentDbid: Int
    ): Flow<List<IAttachmentToken>> {
        return cache.attachments()
            .getAttachmentsDbosWithIds(accountId, AttachToType.COMMENT, commentDbid)
            .map {
                val tokens: MutableList<IAttachmentToken> = ArrayList(it.size)
                for (pair in it) {
                    tokens.add(createToken(pair.second))
                }
                tokens
            }
    }

    override fun getAllCommentsRange(
        accountId: Long,
        commented: Commented,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Flow<List<Comment>> {
        val tempData = TempData()
        val completable =
            startLooking(accountId, commented, tempData, startFromCommentId, continueToCommentId)
                .repeatUntil({
                    for (c in tempData.comments) {
                        if (continueToCommentId == c.id) {
                            return@repeatUntil true
                        }
                    }
                    false
                }, 100)
        return completable.flatMapConcat {
            transform(
                accountId,
                commented,
                tempData.comments,
                tempData.profiles,
                tempData.groups
            )
        }
    }

    override fun getAvailableAuthors(accountId: Long): Flow<List<Owner>> {
        return ownersRepository.getBaseOwnerInfo(accountId, accountId, IOwnersRepository.MODE_ANY)
            .flatMapConcat { owner ->
                networker.vkDefault(accountId)
                    .groups()[accountId, true, "admin,editor", Fields.FIELDS_BASE_OWNER, null, 1000]
                    .map { obj -> obj.items.orEmpty() }
                    .map {
                        val owners: MutableList<Owner> = ArrayList(it.size + 1)
                        owners.add(owner)
                        owners.addAll(transformCommunities(it))
                        owners
                    }
            }
    }

    override fun edit(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        text: String?,
        commentThread: Int?,
        attachments: List<AbsModel>?
    ): Flow<Comment> {
        val tokens: MutableList<IAttachmentToken> = ArrayList()
        if (attachments != null) {
            tokens.addAll(createTokens(attachments))
        }
        val editSingle = when (commented.sourceType) {
            CommentedType.POST -> networker
                .vkDefault(accountId)
                .wall()
                .editComment(commented.sourceOwnerId, commentId, text, tokens)

            CommentedType.PHOTO -> networker
                .vkDefault(accountId)
                .photos()
                .editComment(commented.sourceOwnerId, commentId, text, tokens)

            CommentedType.TOPIC -> {
                val groupId = abs(commented.sourceOwnerId)
                val topicId = commented.sourceId
                networker
                    .vkDefault(accountId)
                    .board()
                    .editComment(groupId, topicId, commentId, text, tokens)
            }

            CommentedType.VIDEO -> networker
                .vkDefault(accountId)
                .video()
                .editComment(commented.sourceOwnerId, commentId, text, tokens)

            else -> throw IllegalArgumentException("Unknown commented source type")
        }
        return editSingle.flatMapConcat {
            getCommentByIdAndStore(
                accountId,
                commented,
                commentId,
                commentThread,
                true
            )
        }
    }

    private fun startLooking(
        accountId: Long,
        commented: Commented,
        tempData: TempData,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Flow<Boolean> {
        val tryNumber = intArrayOf(0)
        return flow {
            tryNumber[0]++
            if (tryNumber[0] == 1) {
                emit(startFromCommentId)
                return@flow
            }
            if (tempData.comments.isEmpty()) {
                throw NotFoundException()
            }
            val older = tempData.comments[tempData.comments.size - 1]
            if (older.id < continueToCommentId) {
                throw NotFoundException()
            }
            emit(older.id)
        }.flatMapConcat { id ->
            getDefaultCommentsService(
                accountId,
                commented,
                id,
                1,
                100,
                "desc",
                true,
                Fields.FIELDS_BASE_OWNER
            )
                .map { response ->
                    tempData.append(response, continueToCommentId)
                    response
                }.ignoreElement()
        }
    }

    private fun getDefaultCommentsService(
        accountId: Long, commented: Commented, startCommentId: Int,
        offset: Int, count: Int, sort: String, extended: Boolean, fields: String
    ): Flow<DefaultCommentsResponse> {
        val ownerId = commented.sourceOwnerId
        val sourceId = commented.sourceId
        when (commented.sourceType) {
            CommentedType.POST -> return networker.vkDefault(accountId)
                .wall()
                .getComments(
                    ownerId,
                    sourceId,
                    true,
                    startCommentId,
                    offset,
                    count,
                    sort,
                    extended,
                    fields
                )

            CommentedType.PHOTO -> return networker.vkDefault(accountId)
                .photos()
                .getComments(
                    ownerId,
                    sourceId,
                    true,
                    startCommentId,
                    offset,
                    count,
                    sort,
                    commented.accessKey,
                    extended,
                    fields
                )

            CommentedType.VIDEO -> return networker.vkDefault(accountId)
                .video()
                .getComments(
                    ownerId,
                    sourceId,
                    true,
                    startCommentId,
                    offset,
                    count,
                    sort,
                    extended,
                    fields
                )

            CommentedType.TOPIC -> return networker.vkDefault(accountId)
                .board()
                .getComments(
                    abs(ownerId),
                    sourceId,
                    true,
                    startCommentId,
                    offset,
                    count,
                    extended,
                    sort,
                    fields
                )
        }
        throw UnsupportedOperationException()
    }

    private fun sendComment(
        accountId: Long,
        commented: Commented,
        intent: CommentIntent,
        attachments: List<IAttachmentToken>?
    ): Flow<Int> {
        val apies = networker.vkDefault(accountId)
        return when (commented.sourceType) {
            CommentedType.POST -> {
                val fromGroup = if (intent.authorId < 0) abs(intent.authorId) else null
                apies.wall()
                    .createComment(
                        commented.sourceOwnerId, commented.sourceId,
                        fromGroup, intent.message, intent.replyToComment,
                        attachments, intent.stickerId, intent.draftMessageId
                    )
            }

            CommentedType.PHOTO -> apies.photos()
                .createComment(
                    commented.sourceOwnerId,
                    commented.sourceId,
                    intent.authorId < 0,
                    intent.message,
                    intent.replyToComment,
                    attachments,
                    intent.stickerId,
                    commented.accessKey,
                    intent.draftMessageId
                )

            CommentedType.VIDEO -> apies.video()
                .createComment(
                    commented.sourceOwnerId, commented.sourceId,
                    intent.message, attachments, intent.authorId < 0,
                    intent.replyToComment, intent.stickerId, intent.draftMessageId
                )

            CommentedType.TOPIC -> {
                val topicGroupId = abs(commented.sourceOwnerId)
                apies.board()
                    .addComment(
                        topicGroupId,
                        commented.sourceId,
                        intent.message,
                        attachments,
                        intent.authorId < 0,
                        intent.stickerId,
                        intent.draftMessageId
                    )
            }

            else -> throw UnsupportedOperationException()
        }
    }

    private fun getCommentByIdAndStore(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        commentThread: Int?,
        storeToCache: Boolean
    ): Flow<Comment> {
        val type = commented.typeForStoredProcedure
        val sourceId = commented.sourceId
        val ownerId = commented.sourceOwnerId
        val sourceType = commented.sourceType
        return networker.vkDefault(accountId)
            .comments()[type, commented.sourceOwnerId, commented.sourceId, 0, 1, null, commentId, commentThread, commented.accessKey, Fields.FIELDS_BASE_OWNER]
            .flatMapConcat { response ->
                if (response.main == null || safeCountOf(response.main?.comments) != 1) {
                    throw NotFoundException()
                }
                val comments = response.main?.comments ?: throw NotFoundException()
                val users = response.main?.profiles
                val communities = response.main?.groups
                val storeCompletable = if (storeToCache) {
                    val dbos: MutableList<CommentEntity> = ArrayList(comments.size)
                    for (dto in comments) {
                        dbos.add(
                            mapComment(
                                commented.sourceId,
                                commented.sourceOwnerId,
                                commented.sourceType,
                                commented.accessKey,
                                dto
                            )
                        )
                    }
                    cache.comments()
                        .insert(
                            accountId,
                            sourceId,
                            ownerId,
                            sourceType,
                            dbos,
                            mapOwners(users, communities),
                            false
                        )
                        .ignoreElement()
                } else {
                    emptyTaskFlow()
                }
                storeCompletable.andThen(
                    transform(
                        accountId,
                        commented,
                        comments,
                        users,
                        communities
                    )
                        .map { data -> data[0] })
            }
    }

    override fun reportComment(
        accountId: Long,
        owner_id: Long,
        post_id: Int,
        reason: Int
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .reportComment(owner_id, post_id, reason)
    }

    private class TempData {
        val profiles: MutableSet<VKApiUser> = HashSet()
        val groups: MutableSet<VKApiCommunity> = HashSet()
        val comments: MutableList<VKApiComment> = ArrayList()
        fun append(response: DefaultCommentsResponse, continueToCommentId: Int) {
            response.groups.requireNonNull {
                groups.addAll(it)
            }
            response.profiles.requireNonNull {
                profiles.addAll(it)
            }
            var hasTargetComment = false
            var additionalCount = 0
            response.items.nonNullNoEmpty {
                for (comment in it) {
                    if (comment.id == continueToCommentId) {
                        hasTargetComment = true
                    } else if (hasTargetComment) {
                        additionalCount++
                    }
                    comments.add(comment)
                    if (additionalCount > 5) {
                        break
                    }
                }
            }
        }
    }
}