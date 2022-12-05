package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.IAttachmentToken
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
import dev.ragnarok.fenrir.model.IOwnersBundle
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.math.abs

class WallsRepository(
    private val networker: INetworker,
    private val storages: IStorages,
    private val ownersRepository: IOwnersRepository
) : IWallsRepository {
    private val minorUpdatesPublisher = PublishSubject.create<PostUpdate>()
    private val majorUpdatesPublisher = PublishSubject.create<Post>()
    private val postInvalidatePublisher = PublishSubject.create<IdPair>()
    override fun editPost(
        accountId: Int, ownerId: Int, postId: Int, friendsOnly: Boolean?,
        message: String?, attachments: List<AbsModel>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?, longitude: Double?,
        placeId: Int?, markAsAds: Boolean?
    ): Completable {
        var tokens: List<IAttachmentToken>? = null
        try {
            if (attachments.nonNullNoEmpty()) {
                tokens = createTokens(attachments)
            }
        } catch (e: Exception) {
            return Completable.error(e)
        }
        return networker.vkDefault(accountId)
            .wall()
            .edit(
                ownerId, postId, friendsOnly, message, tokens, services,
                signed, publishDate, latitude, longitude, placeId, markAsAds
            )
            .flatMapCompletable {
                getAndStorePost(
                    accountId,
                    ownerId,
                    postId
                ).ignoreElement()
            }
    }

    override fun post(
        accountId: Int, ownerId: Int, friendsOnly: Boolean?, fromGroup: Boolean?, message: String?,
        attachments: List<AbsModel>?, services: String?, signed: Boolean?,
        publishDate: Long?, latitude: Double?, longitude: Double?, placeId: Int?,
        postId: Int?, guid: Int?, markAsAds: Boolean?, adsPromotedStealth: Boolean?
    ): Single<Post> {
        var tokens: List<IAttachmentToken>? = null
        try {
            if (attachments.nonNullNoEmpty()) {
                tokens = createTokens(attachments)
            }
        } catch (e: Exception) {
            return Single.error(e)
        }
        return networker.vkDefault(accountId)
            .wall()
            .post(
                ownerId, friendsOnly, fromGroup, message, tokens, services, signed, publishDate,
                latitude, longitude, placeId, postId, guid, markAsAds, adsPromotedStealth
            )
            .flatMap { vkid ->
                val completable: Completable = if (postId != null && postId != vkid) {
                    // если id поста изменился - удаляем его из бд
                    invalidatePost(accountId, postId, ownerId)
                } else {
                    Completable.complete()
                }
                completable.andThen(getAndStorePost(accountId, ownerId, vkid))
            }
    }

    private fun invalidatePost(accountId: Int, postId: Int, ownerId: Int): Completable {
        val pair = IdPair(postId, ownerId)
        return storages.wall()
            .invalidatePost(accountId, postId, ownerId)
            .doOnComplete { postInvalidatePublisher.onNext(pair) }
    }

    override fun like(accountId: Int, ownerId: Int, postId: Int, add: Boolean): Single<Int> {
        val single: Single<Int> = if (add) {
            networker.vkDefault(accountId)
                .likes()
                .add("post", ownerId, postId, null)
        } else {
            networker.vkDefault(accountId)
                .likes()
                .delete("post", ownerId, postId, null)
        }
        return single.flatMap { count ->
            // TODO: 05.09.2017 Сохранение лайков в таблице новостей надо ?
            val update = PostUpdate(accountId, postId, ownerId).withLikes(count, add)
            applyPatch(update).andThen(Single.just(count))
        }
    }

    override fun checkAndAddLike(accountId: Int, ownerId: Int, postId: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("post", ownerId, postId, null)
    }

    override fun isLiked(accountId: Int, ownerId: Int, postId: Int): Single<Boolean> {
        return networker.vkDefault(accountId)
            .likes()
            .isLiked("post", ownerId, postId)
    }

    override fun getWallNoCache(
        accountId: Int,
        ownerId: Int,
        offset: Int,
        count: Int,
        wallFilter: Int
    ): Single<List<Post>> {
        return networker.vkDefault(accountId)
            .wall()[ownerId, null, offset, count, convertToApiFilter(wallFilter), true, Constants.MAIN_OWNER_FIELDS]
            .flatMap { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val dtos = listEmptyIfNull(response.posts)
                val ids = VKOwnIds()
                for (dto in dtos) {
                    ids.append(dto)
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ids.all,
                        IOwnersRepository.MODE_ANY,
                        owners
                    )
                    .flatMap { bundle ->
                        val posts = transformPosts(dtos, bundle)
                        Single.just(posts)
                    }
            }
    }

    override fun getWall(
        accountId: Int,
        ownerId: Int,
        offset: Int,
        count: Int,
        wallFilter: Int,
        needStore: Boolean
    ): Single<List<Post>> {
        return networker.vkDefault(accountId)
            .wall()[ownerId, null, offset, count, convertToApiFilter(wallFilter), true, Constants.MAIN_OWNER_FIELDS]
            .flatMap { response ->
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
                    .flatMap { bundle ->
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
                                        override val ownerId: Int
                                            get() = ownerId
                                    } else null)
                                .map { posts }
                        } else {
                            Single.just(posts)
                        }
                    }
            }
    }

    private fun entities2models(accountId: Int): SingleTransformer<List<PostDboEntity>, List<Post>> {
        return SingleTransformer { single: Single<List<PostDboEntity>> ->
            single
                .flatMap { dbos ->
                    val ids = VKOwnIds()
                    fillOwnerIds(ids, dbos)
                    ownersRepository
                        .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
                        .map<List<Post>> { owners: IOwnersBundle ->
                            val posts: MutableList<Post> = ArrayList(dbos.size)
                            for (dbo in dbos) {
                                posts.add(buildPostFromDbo(dbo, owners))
                            }
                            posts
                        }
                }
        }
    }

    private fun entity2model(accountId: Int): SingleTransformer<PostDboEntity, Post> {
        return SingleTransformer { single: Single<PostDboEntity> ->
            single
                .flatMap { dbo ->
                    val ids = VKOwnIds()
                    fillPostOwnerIds(ids, dbo)
                    ownersRepository
                        .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
                        .map { owners ->
                            buildPostFromDbo(
                                dbo, owners
                            )
                        }
                }
        }
    }

    override fun getCachedWall(accountId: Int, ownerId: Int, wallFilter: Int): Single<List<Post>> {
        val criteria = WallCriteria(accountId, ownerId).setMode(wallFilter)
        return storages.wall()
            .findDbosByCriteria(criteria)
            .compose(entities2models(accountId))
    }

    private fun applyPatch(update: PostUpdate): Completable {
        val patch = update2patch(update)
        return storages.wall()
            .update(update.accountId, update.ownerId, update.postId, patch)
            .andThen(Completable.fromAction { minorUpdatesPublisher.onNext(update) })
    }

    override fun delete(accountId: Int, ownerId: Int, postId: Int): Completable {
        val update = PostUpdate(accountId, postId, ownerId).withDeletion(true)
        return networker.vkDefault(accountId)
            .wall()
            .delete(ownerId, postId)
            .flatMapCompletable { applyPatch(update) }
    }

    override fun restore(accountId: Int, ownerId: Int, postId: Int): Completable {
        val update = PostUpdate(accountId, postId, ownerId).withDeletion(false)
        return networker.vkDefault(accountId)
            .wall()
            .restore(ownerId, postId)
            .flatMapCompletable { applyPatch(update) }
    }

    override fun reportPost(accountId: Int, owner_id: Int, post_id: Int, reason: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .reportPost(owner_id, post_id, reason)
    }

    override fun subscribe(accountId: Int, owner_id: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .subscribe(owner_id)
    }

    override fun unsubscribe(accountId: Int, owner_id: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .wall()
            .unsubscribe(owner_id)
    }

    override fun getById(accountId: Int, ownerId: Int, postId: Int): Single<Post> {
        val id = dev.ragnarok.fenrir.api.model.IdPair(postId, ownerId)
        return networker.vkDefault(accountId)
            .wall()
            .getById(setOf(id), true, 5, Constants.MAIN_OWNER_FIELDS)
            .flatMap { response ->
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

    override fun pinUnpin(accountId: Int, ownerId: Int, postId: Int, pin: Boolean): Completable {
        val single: Single<Boolean> = if (pin) {
            networker.vkDefault(accountId)
                .wall()
                .pin(ownerId, postId)
        } else {
            networker.vkDefault(accountId)
                .wall()
                .unpin(ownerId, postId)
        }
        val update = PostUpdate(accountId, postId, ownerId).withPin(pin)
        return single.flatMapCompletable { applyPatch(update) }
    }

    override fun observeMinorChanges(): Observable<PostUpdate> {
        return minorUpdatesPublisher
    }

    override fun observeChanges(): Observable<Post> {
        return majorUpdatesPublisher
    }

    override fun observePostInvalidation(): Observable<IdPair> {
        return postInvalidatePublisher
    }

    override fun getEditingPost(
        accountId: Int,
        ownerId: Int,
        type: Int,
        withAttachments: Boolean
    ): Single<Post> {
        return storages.wall()
            .getEditingPost(accountId, ownerId, type, withAttachments)
            .compose(entity2model(accountId))
    }

    override fun post(
        accountId: Int,
        post: Post,
        fromGroup: Boolean,
        showSigner: Boolean
    ): Single<Post> {
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
        accountId: Int,
        postId: Int,
        ownerId: Int,
        groupId: Int?,
        message: String?
    ): Single<Post> {
        val resultOwnerId = if (groupId != null) -abs(groupId) else accountId
        return networker.vkDefault(accountId)
            .wall()
            .repost(ownerId, postId, message, groupId, null)
            .flatMap { reponse ->
                reponse.postId?.let {
                    getAndStorePost(
                        accountId,
                        resultOwnerId,
                        it
                    )
                } ?: throw NullPointerException("Wall Repository response.postId is Null")
            }
    }

    override fun cachePostWithIdSaving(accountId: Int, post: Post): Single<Int> {
        val entity = buildPostDbo(post)
        return storages.wall()
            .replacePost(accountId, entity)
    }

    override fun deleteFromCache(accountId: Int, postDbid: Int): Completable {
        return storages.wall().deletePost(accountId, postDbid)
    }

    private fun getAndStorePost(accountId: Int, ownerId: Int, postId: Int): Single<Post> {
        val cache = storages.wall()
        return networker.vkDefault(accountId)
            .wall()
            .getById(singlePair(postId, ownerId), true, 5, Constants.MAIN_OWNER_FIELDS)
            .flatMap { response ->
                if (safeCountOf(response.posts) != 1) {
                    throw NotFoundException()
                }
                val dbo = mapPost(response.posts?.get(0) ?: throw NotFoundException())
                val ownerEntities = mapOwners(response.profiles, response.groups)
                cache.storeWallEntities(accountId, listOf(dbo), ownerEntities, null)
                    .map { ints -> ints[0] }
                    .flatMap { dbid ->
                        cache
                            .findPostById(accountId, dbid)
                            .map { obj -> obj.requireNonEmpty() }
                            .compose(entity2model(accountId))
                    }
            }
            .map { post ->
                majorUpdatesPublisher.onNext(post)
                post
            }
    }

    override fun search(
        accountId: Int,
        ownerId: Int,
        query: String?,
        ownersPostOnly: Boolean,
        count: Int,
        offset: Int
    ): Single<Pair<List<Post>, Int>> {
        return networker.vkDefault(accountId)
            .wall()
            .search(
                ownerId,
                query,
                ownersPostOnly,
                count,
                offset,
                true,
                Constants.MAIN_OWNER_FIELDS
            )
            .flatMap { response ->
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
            postOwnerId: Int
        ): Collection<dev.ragnarok.fenrir.api.model.IdPair> {
            return listOf(dev.ragnarok.fenrir.api.model.IdPair(postId, postOwnerId))
        }
    }
}