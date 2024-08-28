package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.interfaces.IWallStorage.IClearWallTask
import dev.ragnarok.fenrir.db.model.PostPatch
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapPost
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformPosts
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildPostFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillPostOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Model2Dto.createTokens
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildPostDbo
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.IdPair
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class WallsRepository(
    private val networker: INetworker,
    private val storages: IStorages,
    private val ownersRepository: IOwnersRepository
) : IWallsRepository {
    private val minorUpdatesPublisher = createPublishSubject<PostUpdate>()
    private val majorUpdatesPublisher = createPublishSubject<Post>()
    private val postInvalidatePublisher = createPublishSubject<IdPair>()
    override fun editPost(
        accountId: Long, ownerId: Long, postId: Int, friendsOnly: Boolean?,
        message: String?, attachments: List<AbsModel>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?, longitude: Double?,
        placeId: Int?, markAsAds: Boolean?
    ): Flow<Boolean> {
        var tokens: List<IAttachmentToken>? = null
        try {
            if (attachments.nonNullNoEmpty()) {
                tokens = createTokens(attachments)
            }
        } catch (e: Exception) {
            return toFlowThrowable(e)
        }
        return networker.vkDefault(accountId)
            .wall()
            .edit(
                ownerId, postId, friendsOnly, message, tokens, services,
                signed, publishDate, latitude, longitude, placeId, markAsAds
            )
            .flatMapConcat {
                getAndStorePost(
                    accountId,
                    ownerId,
                    postId
                ).ignoreElement()
            }
    }

    override fun post(
        accountId: Long,
        ownerId: Long,
        friendsOnly: Boolean?,
        fromGroup: Boolean?,
        message: String?,
        attachments: List<AbsModel>?,
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
    ): Flow<Post> {
        var tokens: List<IAttachmentToken>? = null
        try {
            if (attachments.nonNullNoEmpty()) {
                tokens = createTokens(attachments)
            }
        } catch (e: Exception) {
            return toFlowThrowable(e)
        }
        return networker.vkDefault(accountId)
            .wall()
            .post(
                ownerId, friendsOnly, fromGroup, message, tokens, services, signed, publishDate,
                latitude, longitude, placeId, postId, guid, markAsAds, adsPromotedStealth
            )
            .flatMapConcat { vkid ->
                val completable = if (postId != null && postId != vkid) {
                    // если id поста изменился - удаляем его из бд
                    invalidatePost(accountId, postId, ownerId)
                } else {
                    emptyTaskFlow()
                }
                completable.andThen(getAndStorePost(accountId, ownerId, vkid))
            }
    }

    private fun invalidatePost(accountId: Long, postId: Int, ownerId: Long): Flow<Boolean> {
        val pair = IdPair(postId, ownerId)
        return storages.wall()
            .invalidatePost(accountId, postId, ownerId)
            .map {
                postInvalidatePublisher.emit(pair)
                true
            }
    }

    override fun like(accountId: Long, ownerId: Long, postId: Int, add: Boolean): Flow<Int> {
        val single: Flow<Int> = if (add) {
            networker.vkDefault(accountId)
                .likes()
                .add("post", ownerId, postId, null)
        } else {
            networker.vkDefault(accountId)
                .likes()
                .delete("post", ownerId, postId, null)
        }
        return single.flatMapConcat { count ->
            val update = PostUpdate(accountId, postId, ownerId).withLikes(count, add)
            applyPatch(update)
                .map {
                    count
                }
        }
    }

    override fun checkAndAddLike(accountId: Long, ownerId: Long, postId: Int): Flow<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("post", ownerId, postId, null)
    }

    override fun isLiked(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .likes()
            .isLiked("post", ownerId, postId)
    }

    override fun getWallNoCache(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int
    ): Flow<List<Post>> {
        return networker.vkDefault(accountId)
            .wall()[ownerId, null, offset, count, convertToApiFilter(wallFilter), true, Fields.FIELDS_BASE_OWNER]
            .flatMapConcat { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val dtos = listEmptyIfNull(response.posts)
                val ids = VKOwnIds()

                for (dto in dtos) {
                    if (dto.owner_id == 0L) {
                        continue
                    }
                    ids.append(dto)
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ids.all,
                        IOwnersRepository.MODE_ANY,
                        owners
                    )
                    .map { bundle ->
                        transformPosts(dtos, bundle)
                    }
            }
    }

    override fun getWall(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int,
        needStore: Boolean
    ): Flow<List<Post>> {
        return networker.vkDefault(accountId)
            .wall()[ownerId, null, offset, count, convertToApiFilter(wallFilter), true, Fields.FIELDS_BASE_OWNER]
            .flatMapConcat { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val dtos = listEmptyIfNull(response.posts)
                val ids = VKOwnIds()
                for (dto in dtos) {
                    ids.append(dto)
                }
                val ownerEntities = mapOwners(response.profiles, response.groups)
                ownersRepository
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ids.all,
                        IOwnersRepository.MODE_ANY,
                        owners
                    )
                    .flatMapConcat { bundle ->
                        val posts = transformPosts(dtos, bundle)
                        val dbos: MutableList<PostDboEntity> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            dbos.add(mapPost(dto))
                        }
                        if (needStore) {
                            storages.wall()
                                .storeWallEntities(
                                    accountId,
                                    dbos,
                                    ownerEntities,
                                    if (offset == 0) object : IClearWallTask {
                                        override val ownerId: Long
                                            get() = ownerId
                                    } else null)
                                .map { posts }
                        } else {
                            toFlow(posts)
                        }
                    }
            }
    }

    private fun entities2models(accountId: Long): suspend (List<PostDboEntity>) -> Flow<List<Post>> =
        {
            val ids = VKOwnIds()
            fillOwnerIds(ids, it)
            ownersRepository
                .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
                .map { owners ->
                    val posts: MutableList<Post> = ArrayList(it.size)
                    for (dbo in it) {
                        posts.add(buildPostFromDbo(dbo, owners))
                    }
                    posts
                }
        }

    private fun entity2model(accountId: Long): suspend (PostDboEntity) -> Flow<Post> = {
        val ids = VKOwnIds()
        fillPostOwnerIds(ids, it)
        ownersRepository
            .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
            .map { owners ->
                buildPostFromDbo(
                    it, owners
                )
            }
    }

    override fun getCachedWall(
        accountId: Long,
        ownerId: Long,
        wallFilter: Int
    ): Flow<List<Post>> {
        val criteria = WallCriteria(accountId, ownerId).setMode(wallFilter)
        return storages.wall()
            .findDbosByCriteria(criteria)
            .flatMapConcat(entities2models(accountId))
    }

    private fun applyPatch(update: PostUpdate): Flow<Boolean> {
        val patch = update2patch(update)
        return storages.wall()
            .update(update.accountId, update.ownerId, update.postId, patch)
            .map {
                minorUpdatesPublisher.emit(update)
                true
            }
    }

    override fun delete(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean> {
        val update = PostUpdate(accountId, postId, ownerId).withDeletion(true)
        return networker.vkDefault(accountId)
            .wall()
            .delete(ownerId, postId)
            .flatMapConcat { applyPatch(update) }
    }

    override fun restore(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean> {
        val update = PostUpdate(accountId, postId, ownerId).withDeletion(false)
        return networker.vkDefault(accountId)
            .wall()
            .restore(ownerId, postId)
            .flatMapConcat { applyPatch(update) }
    }

    override fun reportPost(
        accountId: Long,
        owner_id: Long,
        post_id: Int,
        reason: Int
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .reportPost(owner_id, post_id, reason)
    }

    override fun subscribe(accountId: Long, owner_id: Long): Flow<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .subscribe(owner_id)
    }

    override fun unsubscribe(accountId: Long, owner_id: Long): Flow<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .unsubscribe(owner_id)
    }

    override fun getById(accountId: Long, ownerId: Long, postId: Int): Flow<Post> {
        val id = dev.ragnarok.fenrir.api.model.IdPair(postId, ownerId)
        return networker.vkDefault(accountId)
            .wall()
            .getById(setOf(id), true, 5, Fields.FIELDS_BASE_OWNER)
            .flatMapConcat { response ->
                if (response.posts.isNullOrEmpty()) {
                    throw NotFoundException()
                }
                val owners = transformOwners(response.profiles, response.groups)
                val dtos = response.posts
                val dto = dtos?.get(0) ?: throw NotFoundException()
                val ids = VKOwnIds().append(dto)
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { bundle -> transform(dto, bundle) }
            }
    }

    override fun pinUnpin(
        accountId: Long,
        ownerId: Long,
        postId: Int,
        pin: Boolean
    ): Flow<Boolean> {
        val single = if (pin) {
            networker.vkDefault(accountId)
                .wall()
                .pin(ownerId, postId)
        } else {
            networker.vkDefault(accountId)
                .wall()
                .unpin(ownerId, postId)
        }
        val update = PostUpdate(accountId, postId, ownerId).withPin(pin)
        return single.flatMapConcat { applyPatch(update) }
    }

    override fun observeMinorChanges(): SharedFlow<PostUpdate> {
        return minorUpdatesPublisher
    }

    override fun observeChanges(): SharedFlow<Post> {
        return majorUpdatesPublisher
    }

    override fun observePostInvalidation(): SharedFlow<IdPair> {
        return postInvalidatePublisher
    }

    override fun getEditingPost(
        accountId: Long,
        ownerId: Long,
        type: Int,
        withAttachments: Boolean
    ): Flow<Post> {
        return storages.wall()
            .getEditingPost(accountId, ownerId, type, withAttachments)
            .flatMapConcat(entity2model(accountId))
    }

    override fun post(
        accountId: Long,
        post: Post,
        fromGroup: Boolean,
        showSigner: Boolean
    ): Flow<Post> {
        val publishDate = if (post.isPostponed) post.date else null
        val attachments: List<AbsModel>? =
            if (post.hasAttachments()) post.attachments?.toList() else null
        val postponedPostId = if (post.isPostponed) if (post.vkid > 0) post.vkid else null else null
        return post(
            accountId, post.ownerId, post.isFriendsOnly, fromGroup, post.text,
            attachments, null, showSigner, publishDate, null, null, null,
            postponedPostId, post.dbid, null, null
        )
    }

    override fun repost(
        accountId: Long,
        postId: Int,
        ownerId: Long,
        groupId: Long?,
        message: String?
    ): Flow<Post> {
        val resultOwnerId = if (groupId != null) -abs(groupId) else accountId
        return networker.vkDefault(accountId)
            .wall()
            .repost(ownerId, postId, message, groupId, null)
            .flatMapConcat { reponse ->
                reponse.postId?.let {
                    getAndStorePost(
                        accountId,
                        resultOwnerId,
                        it
                    )
                } ?: throw NullPointerException("Wall Repository response.postId is Null")
            }
    }

    override fun cachePostWithIdSaving(accountId: Long, post: Post): Flow<Int> {
        val entity = buildPostDbo(post)
        return storages.wall()
            .replacePost(accountId, entity)
    }

    override fun deleteFromCache(accountId: Long, postDbid: Int): Flow<Boolean> {
        return storages.wall().deletePost(accountId, postDbid)
    }

    private fun getAndStorePost(accountId: Long, ownerId: Long, postId: Int): Flow<Post> {
        val cache = storages.wall()
        return networker.vkDefault(accountId)
            .wall()
            .getById(singlePair(postId, ownerId), true, 5, Fields.FIELDS_BASE_OWNER)
            .flatMapConcat { response ->
                if (safeCountOf(response.posts) != 1) {
                    throw NotFoundException()
                }
                val dbo = mapPost(response.posts?.get(0) ?: throw NotFoundException())
                val ownerEntities = mapOwners(response.profiles, response.groups)
                cache.storeWallEntities(accountId, listOf(dbo), ownerEntities, null)
                    .map { ints -> ints[0] }
                    .flatMapConcat { dbid ->
                        cache
                            .findPostById(accountId, dbid)
                            .map { obj -> obj.requireNonEmpty() }
                            .flatMapConcat(entity2model(accountId))
                    }
            }
            .map { post ->
                majorUpdatesPublisher.emit(post)
                post
            }
    }

    override fun search(
        accountId: Long,
        ownerId: Long,
        query: String?,
        ownersPostOnly: Boolean,
        count: Int,
        offset: Int
    ): Flow<Pair<List<Post>, Int>> {
        return networker.vkDefault(accountId)
            .wall()
            .search(
                ownerId,
                query,
                ownersPostOnly,
                count,
                offset,
                true,
                Fields.FIELDS_BASE_OWNER
            )
            .flatMapConcat { response ->
                val dtos = listEmptyIfNull(response.items)
                val owners = transformOwners(response.profiles, response.groups)
                val ids = VKOwnIds()
                for (dto in dtos) {
                    ids.append(dto)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { ownersBundle ->
                        create(
                            transformPosts(
                                dtos,
                                ownersBundle
                            ), response.count
                        )
                    }
            }
    }

    companion object {
        internal fun update2patch(data: PostUpdate): PostPatch {
            val patch = PostPatch()
            data.deleteUpdate.requireNonNull {
                patch.withDeletion(it.isDeleted)
            }
            data.likeUpdate.requireNonNull {
                patch.withLikes(it.count, it.isLiked)
            }
            data.pinUpdate.requireNonNull {
                patch.withPin(it.isPinned)
            }
            return patch
        }

        internal fun convertToApiFilter(filter: Int): String {
            return when (filter) {
                WallCriteria.MODE_ALL -> "all"
                WallCriteria.MODE_OWNER -> "owner"
                WallCriteria.MODE_SCHEDULED -> "postponed"
                WallCriteria.MODE_SUGGEST -> "suggests"
                WallCriteria.MODE_DONUT -> "donut"
                else -> throw IllegalArgumentException("Invalid wall filter")
            }
        }

        internal fun singlePair(
            postId: Int,
            postOwnerId: Long
        ): Collection<dev.ragnarok.fenrir.api.model.IdPair> {
            return listOf(dev.ragnarok.fenrir.api.model.IdPair(postId, postOwnerId))
        }
    }
}
