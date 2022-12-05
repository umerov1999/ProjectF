package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.column.GroupColumns
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
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.CommentsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import io.reactivex.rxjava3.functions.BooleanSupplier
import kotlin.math.abs

class CommentsInteractor(
    private val networker: INetworker,
    private val cache: IStorages,
    private val ownersRepository: IOwnersRepository
) : ICommentsInteractor {
    override fun getAllCachedData(accountId: Int, commented: Commented): Single<List<Comment>> {
        val criteria = CommentsCriteria(accountId, commented)
        return cache.comments()
            .getDbosByCriteria(criteria)
            .compose(dbos2models(accountId))
    }

    private fun dbos2models(accountId: Int): SingleTransformer<List<CommentEntity>, List<Comment>> {
        return SingleTransformer { single: Single<List<CommentEntity>> ->
            single.flatMap { dbos ->
                val ownids = VKOwnIds()
                for (c in dbos) {
                    fillCommentOwnerIds(ownids, c)
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(accountId, ownids.all, IOwnersRepository.MODE_ANY)
                    .map<List<Comment>> {
                        val comments: MutableList<Comment> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            buildCommentFromDbo(dbo, it)?.let { it1 -> comments.add(it1) }
                        }
                        comments
                    }
            }
        }
    }

    private fun cacheData(
        accountId: Int,
        commented: Commented,
        data: List<CommentEntity>,
        owners: OwnerEntities,
        invalidateCache: Boolean
    ): Completable {
        val sourceId = commented.sourceId
        val ownerId = commented.sourceOwnerId
        val type = commented.sourceType
        return Single.just(data)
            .flatMapCompletable {
                cache.comments()
                    .insert(accountId, sourceId, ownerId, type, it, owners, invalidateCache)
                    .ignoreElement()
            }
    }

    private fun transform(
        accountId: Int,
        commented: Commented,
        comments: List<VKApiComment>,
        users: Collection<VKApiUser>?,
        groups: Collection<VKApiCommunity>?
    ): Single<ArrayList<Comment>> {
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
                data.sortWith { o1: Comment, o2: Comment ->
                    o2.getObjectId().compareTo(o1.getObjectId())
                }
                data
            }
    }

    override fun getCommentsNoCache(
        accountId: Int,
        ownerId: Int,
        postId: Int,
        offset: Int
    ): Single<List<Comment>> {
        return networker.vkDefault(accountId)
            .comments()["post", ownerId, postId, offset, 100, "desc", null, null, null, Constants.MAIN_OWNER_FIELDS]
            .flatMap { response ->
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
        accountId: Int,
        commented: Commented,
        offset: Int,
        count: Int,
        startCommentId: Int?,
        threadComment: Int?,
        invalidateCache: Boolean,
        sort: String?
    ): Single<CommentsBundle> {
        val type = commented.typeForStoredProcedure
        return networker.vkDefault(accountId)
            .comments()[type, commented.sourceOwnerId, commented.sourceId, offset, count, sort, startCommentId, threadComment, commented.accessKey, Constants.MAIN_OWNER_FIELDS]
            .flatMap { response ->
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
                    return@flatMap modelsSingle.map { data ->
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
                }
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

    override fun restoreDraftComment(accountId: Int, commented: Commented): Maybe<DraftComment>? {
        return cache.comments()
            .findEditingComment(accountId, commented)
    }

    override fun safeDraftComment(
        accountId: Int,
        commented: Commented,
        body: String?,
        replyToCommentId: Int,
        replyToUserId: Int
    ): Single<Int> {
        return cache.comments()
            .saveDraftComment(accountId, commented, body, replyToUserId, replyToCommentId)
    }

    override fun isLiked(accountId: Int, commented: Commented, commentId: Int): Single<Boolean> {
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
        accountId: Int,
        commented: Commented,
        commentId: Int
    ): Single<Int> {
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
        accountId: Int,
        commented: Commented,
        commentId: Int,
        add: Boolean
    ): Completable {
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
                .flatMapCompletable { count: Int ->
                    update.withLikes(true, count)
                    cache.comments().commitMinorUpdate(update)
                }
        } else {
            api.delete(type, commented.sourceOwnerId, commentId, commented.accessKey)
                .flatMapCompletable { count: Int ->
                    update.withLikes(false, count)
                    cache.comments().commitMinorUpdate(update)
                }
        }
    }

    override fun deleteRestore(
        accountId: Int,
        commented: Commented,
        commentId: Int,
        delete: Boolean
    ): Completable {
        val apis = networker.vkDefault(accountId)
        val ownerId = commented.sourceOwnerId
        val update = CommentUpdate.create(accountId, commented, commentId)
            .withDeletion(delete)
        val single: Single<Boolean> = when (commented.sourceType) {
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
        return single.flatMapCompletable {
            cache
                .comments()
                .commitMinorUpdate(update)
        }
    }

    override fun send(
        accountId: Int,
        commented: Commented,
        commentThread: Int?,
        intent: CommentIntent
    ): Single<Comment> {
        val cachedAttachments: Single<List<IAttachmentToken>> =
            intent.getDraftMessageId().requireNonNull({
                getCachedAttachmentsToken(accountId, it)
            }, {
                Single.just(emptyList())
            })
        return cachedAttachments
            .flatMap { cachedTokens ->
                val tokens: MutableList<IAttachmentToken> = ArrayList()
                tokens.addAll(cachedTokens)
                if (intent.getModels().nonNullNoEmpty()) {
                    tokens.addAll(createTokens(intent.getModels()))
                }
                sendComment(accountId, commented, intent, tokens)
                    .flatMap { id ->
                        getCommentByIdAndStore(
                            accountId,
                            commented,
                            id,
                            commentThread,
                            true
                        )
                    }
                    .flatMap { comment ->
                        intent.getDraftMessageId().requireNonNull({
                            cache.comments()
                                .deleteByDbid(accountId, it)
                                .andThen(Single.just(comment))
                        }, {
                            Single.just(comment)
                        })
                    }
            }
    }

    private fun getCachedAttachmentsToken(
        accountId: Int,
        commentDbid: Int
    ): Single<List<IAttachmentToken>> {
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
        accountId: Int,
        commented: Commented,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Single<List<Comment>> {
        val tempData = TempData()
        val booleanSupplier = BooleanSupplier {
            for (c in tempData.comments) {
                if (continueToCommentId == c.id) {
                    return@BooleanSupplier true
                }
            }
            false
        }
        val completable =
            startLooking(accountId, commented, tempData, startFromCommentId, continueToCommentId)
                .repeatUntil(booleanSupplier)
        return completable.toSingleDefault(tempData)
            .flatMap { data ->
                transform(
                    accountId,
                    commented,
                    data.comments,
                    data.profiles,
                    data.groups
                )
            }
    }

    override fun getAvailableAuthors(accountId: Int): Single<List<Owner>> {
        return ownersRepository.getBaseOwnerInfo(accountId, accountId, IOwnersRepository.MODE_ANY)
            .flatMap { owner ->
                networker.vkDefault(accountId)
                    .groups()[accountId, true, "admin,editor", GroupColumns.API_FIELDS, null, 1000]
                    .map { obj -> obj.items.orEmpty() }
                    .map<List<Owner>> {
                        val owners: MutableList<Owner> = ArrayList(it.size + 1)
                        owners.add(owner)
                        owners.addAll(transformCommunities(it))
                        owners
                    }
            }
    }

    override fun edit(
        accountId: Int,
        commented: Commented,
        commentId: Int,
        body: String?,
        commentThread: Int?,
        attachments: List<AbsModel>?
    ): Single<Comment> {
        val tokens: MutableList<IAttachmentToken> = ArrayList()
        try {
            if (attachments != null) {
                tokens.addAll(createTokens(attachments))
            }
        } catch (e: Exception) {
            return Single.error(e)
        }
        val editSingle: Single<Boolean> = when (commented.sourceType) {
            CommentedType.POST -> networker
                .vkDefault(accountId)
                .wall()
                .editComment(commented.sourceOwnerId, commentId, body, tokens)
            CommentedType.PHOTO -> networker
                .vkDefault(accountId)
                .photos()
                .editComment(commented.sourceOwnerId, commentId, body, tokens)
            CommentedType.TOPIC -> {
                val groupId = abs(commented.sourceOwnerId)
                val topicId = commented.sourceId
                networker
                    .vkDefault(accountId)
                    .board()
                    .editComment(groupId, topicId, commentId, body, tokens)
            }
            CommentedType.VIDEO -> networker
                .vkDefault(accountId)
                .video()
                .editComment(commented.sourceOwnerId, commentId, body, tokens)
            else -> return Single.error(IllegalArgumentException("Unknown commented source type"))
        }
        return editSingle.flatMap {
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
        accountId: Int,
        commented: Commented,
        tempData: TempData,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Completable {
        val tryNumber = intArrayOf(0)
        return Single
            .fromCallable {
                tryNumber[0]++
                if (tryNumber[0] == 1) {
                    return@fromCallable startFromCommentId
                }
                if (tempData.comments.isEmpty()) {
                    throw NotFoundException()
                }
                val older = tempData.comments[tempData.comments.size - 1]
                if (older.id < continueToCommentId) {
                    throw NotFoundException()
                }
                older.id
            }.flatMapCompletable { id: Int ->
                getDefaultCommentsService(
                    accountId,
                    commented,
                    id,
                    1,
                    100,
                    "desc",
                    true,
                    Constants.MAIN_OWNER_FIELDS
                )
                    .map { response ->
                        tempData.append(response, continueToCommentId)
                        response
                    }.ignoreElement()
            }
    }

    private fun getDefaultCommentsService(
        accountId: Int, commented: Commented, startCommentId: Int,
        offset: Int, count: Int, sort: String, extended: Boolean, fields: String
    ): Single<DefaultCommentsResponse> {
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
        accountId: Int,
        commented: Commented,
        intent: CommentIntent,
        attachments: List<IAttachmentToken>?
    ): Single<Int> {
        val apies = networker.vkDefault(accountId)
        return when (commented.sourceType) {
            CommentedType.POST -> {
                val fromGroup = if (intent.getAuthorId() < 0) abs(intent.getAuthorId()) else null
                apies.wall()
                    .createComment(
                        commented.sourceOwnerId, commented.sourceId,
                        fromGroup, intent.getMessage(), intent.getReplyToComment(),
                        attachments, intent.getStickerId(), intent.getDraftMessageId()
                    )
            }
            CommentedType.PHOTO -> apies.photos()
                .createComment(
                    commented.sourceOwnerId,
                    commented.sourceId,
                    intent.getAuthorId() < 0,
                    intent.getMessage(),
                    intent.getReplyToComment(),
                    attachments,
                    intent.getStickerId(),
                    commented.accessKey,
                    intent.getDraftMessageId()
                )
            CommentedType.VIDEO -> apies.video()
                .createComment(
                    commented.sourceOwnerId, commented.sourceId,
                    intent.getMessage(), attachments, intent.getAuthorId() < 0,
                    intent.getReplyToComment(), intent.getStickerId(), intent.getDraftMessageId()
                )
            CommentedType.TOPIC -> {
                val topicGroupId = abs(commented.sourceOwnerId)
                apies.board()
                    .addComment(
                        topicGroupId,
                        commented.sourceId,
                        intent.getMessage(),
                        attachments,
                        intent.getAuthorId() < 0,
                        intent.getStickerId(),
                        intent.getDraftMessageId()
                    )
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun getCommentByIdAndStore(
        accountId: Int,
        commented: Commented,
        commentId: Int,
        commentThread: Int?,
        storeToCache: Boolean
    ): Single<Comment> {
        val type = commented.typeForStoredProcedure
        val sourceId = commented.sourceId
        val ownerId = commented.sourceOwnerId
        val sourceType = commented.sourceType
        return networker.vkDefault(accountId)
            .comments()[type, commented.sourceOwnerId, commented.sourceId, 0, 1, null, commentId, commentThread, commented.accessKey, Constants.MAIN_OWNER_FIELDS]
            .flatMap { response ->
                if (response.main == null || safeCountOf(response.main?.comments) != 1) {
                    throw NotFoundException()
                }
                val comments = response.main?.comments ?: throw NotFoundException()
                val users = response.main?.profiles
                val communities = response.main?.groups
                val storeCompletable: Completable = if (storeToCache) {
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
                    Completable.complete()
                }
                storeCompletable.andThen(transform(
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
        accountId: Int,
        owner_id: Int,
        post_id: Int,
        reason: Int
    ): Single<Int> {
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