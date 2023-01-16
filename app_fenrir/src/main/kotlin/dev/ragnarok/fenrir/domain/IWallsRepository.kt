package dev.ragnarok.fenrir.domain

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.IdPair
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface IWallsRepository {
    @CheckResult
    fun editPost(
        accountId: Long, ownerId: Long, postId: Int, friendsOnly: Boolean?, message: String?,
        attachments: List<AbsModel>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?,
        longitude: Double?, placeId: Int?, markAsAds: Boolean?
    ): Completable

    fun search(
        accountId: Long,
        ownerId: Long,
        query: String?,
        ownersPostOnly: Boolean,
        count: Int,
        offset: Int
    ): Single<Pair<List<Post>, Int>>

    fun post(
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
    ): Single<Post>

    fun like(accountId: Long, ownerId: Long, postId: Int, add: Boolean): Single<Int>
    fun isLiked(accountId: Long, ownerId: Long, postId: Int): Single<Boolean>
    fun getWall(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int,
        needStore: Boolean
    ): Single<List<Post>>

    fun getWallNoCache(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int
    ): Single<List<Post>>

    fun checkAndAddLike(accountId: Long, ownerId: Long, postId: Int): Single<Int>
    fun getCachedWall(accountId: Long, ownerId: Long, wallFilter: Int): Single<List<Post>>
    fun delete(accountId: Long, ownerId: Long, postId: Int): Completable
    fun restore(accountId: Long, ownerId: Long, postId: Int): Completable
    fun reportPost(accountId: Long, owner_id: Long, post_id: Int, reason: Int): Single<Int>
    fun subscribe(accountId: Long, owner_id: Long): Single<Int>
    fun unsubscribe(accountId: Long, owner_id: Long): Single<Int>
    fun getById(accountId: Long, ownerId: Long, postId: Int): Single<Post>
    fun pinUnpin(accountId: Long, ownerId: Long, postId: Int, pin: Boolean): Completable

    /**
     * Ability to observe minor post changes (likes, deleted, pin state, etc.)
     */
    fun observeMinorChanges(): Observable<PostUpdate>

    /**
     *
     */
    fun observeChanges(): Observable<Post>

    /**
     * @return onNext в том случае, если пост перестал существовать
     */
    fun observePostInvalidation(): Observable<IdPair>

    /**
     * Получить пост-черновик
     *
     * @param accountId       идентификатор аккаунта
     * @param ownerId         идентификатор владельца стены
     * @param type            тип (черновик или временный пост)
     * @param withAttachments если true - загрузить вложения поста
     * @return Single c обьектом поста
     */
    fun getEditingPost(
        accountId: Long,
        ownerId: Long,
        @EditingPostType type: Int,
        withAttachments: Boolean
    ): Single<Post>

    fun post(accountId: Long, post: Post, fromGroup: Boolean, showSigner: Boolean): Single<Post>
    fun repost(
        accountId: Long,
        postId: Int,
        ownerId: Long,
        groupId: Long?,
        message: String?
    ): Single<Post>

    /**
     * Сохранить пост в базу с тем же локальным идентификатором
     *
     * @param accountId идентификатор аккаунта
     * @param post      пост
     * @return Single с локальным идентификатором
     */
    fun cachePostWithIdSaving(accountId: Long, post: Post): Single<Int>

    /**
     * Удалить пост из кеша (используется только для "черновиков"
     *
     * @param accountId идентификатор аккаунта
     * @param postDbid  локальный идентификатор поста в БД
     * @return Completable
     */
    @CheckResult
    fun deleteFromCache(accountId: Long, postDbid: Int): Completable
}