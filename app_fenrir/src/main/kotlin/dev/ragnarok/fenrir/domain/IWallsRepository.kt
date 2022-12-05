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
        accountId: Int, ownerId: Int, postId: Int, friendsOnly: Boolean?, message: String?,
        attachments: List<AbsModel>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?,
        longitude: Double?, placeId: Int?, markAsAds: Boolean?
    ): Completable

    fun search(
        accountId: Int,
        ownerId: Int,
        query: String?,
        ownersPostOnly: Boolean,
        count: Int,
        offset: Int
    ): Single<Pair<List<Post>, Int>>

    fun post(
        accountId: Int, ownerId: Int, friendsOnly: Boolean?, fromGroup: Boolean?, message: String?,
        attachments: List<AbsModel>?, services: String?, signed: Boolean?,
        publishDate: Long?, latitude: Double?, longitude: Double?, placeId: Int?,
        postId: Int?, guid: Int?, markAsAds: Boolean?, adsPromotedStealth: Boolean?
    ): Single<Post>

    fun like(accountId: Int, ownerId: Int, postId: Int, add: Boolean): Single<Int>
    fun isLiked(accountId: Int, ownerId: Int, postId: Int): Single<Boolean>
    fun getWall(
        accountId: Int,
        ownerId: Int,
        offset: Int,
        count: Int,
        wallFilter: Int,
        needStore: Boolean
    ): Single<List<Post>>

    fun getWallNoCache(
        accountId: Int,
        ownerId: Int,
        offset: Int,
        count: Int,
        wallFilter: Int
    ): Single<List<Post>>

    fun checkAndAddLike(accountId: Int, ownerId: Int, postId: Int): Single<Int>
    fun getCachedWall(accountId: Int, ownerId: Int, wallFilter: Int): Single<List<Post>>
    fun delete(accountId: Int, ownerId: Int, postId: Int): Completable
    fun restore(accountId: Int, ownerId: Int, postId: Int): Completable
    fun reportPost(accountId: Int, owner_id: Int, post_id: Int, reason: Int): Single<Int>
    fun subscribe(accountId: Int, owner_id: Int): Single<Int>
    fun unsubscribe(accountId: Int, owner_id: Int): Single<Int>
    fun getById(accountId: Int, ownerId: Int, postId: Int): Single<Post>
    fun pinUnpin(accountId: Int, ownerId: Int, postId: Int, pin: Boolean): Completable

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
        accountId: Int,
        ownerId: Int,
        @EditingPostType type: Int,
        withAttachments: Boolean
    ): Single<Post>

    fun post(accountId: Int, post: Post, fromGroup: Boolean, showSigner: Boolean): Single<Post>
    fun repost(
        accountId: Int,
        postId: Int,
        ownerId: Int,
        groupId: Int?,
        message: String?
    ): Single<Post>

    /**
     * Сохранить пост в базу с тем же локальным идентификатором
     *
     * @param accountId идентификатор аккаунта
     * @param post      пост
     * @return Single с локальным идентификатором
     */
    fun cachePostWithIdSaving(accountId: Int, post: Post): Single<Int>

    /**
     * Удалить пост из кеша (используется только для "черновиков"
     *
     * @param accountId идентификатор аккаунта
     * @param postDbid  локальный идентификатор поста в БД
     * @return Completable
     */
    @CheckResult
    fun deleteFromCache(accountId: Int, postDbid: Int): Completable
}