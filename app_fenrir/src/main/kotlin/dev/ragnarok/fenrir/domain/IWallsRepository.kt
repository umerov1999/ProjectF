package dev.ragnarok.fenrir.domain

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.IdPair
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IWallsRepository {
    @CheckResult
    fun editPost(
        accountId: Long, ownerId: Long, postId: Int, friendsOnly: Boolean?, message: String?,
        attachments: List<AbsModel>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?,
        longitude: Double?, placeId: Int?, markAsAds: Boolean?
    ): Flow<Boolean>

    fun search(
        accountId: Long,
        ownerId: Long,
        query: String?,
        ownersPostOnly: Boolean,
        count: Int,
        offset: Int
    ): Flow<Pair<List<Post>, Int>>

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
    ): Flow<Post>

    fun like(accountId: Long, ownerId: Long, postId: Int, add: Boolean): Flow<Int>
    fun isLiked(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean>
    fun getWall(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int,
        needStore: Boolean
    ): Flow<List<Post>>

    fun getWallNoCache(
        accountId: Long,
        ownerId: Long,
        offset: Int,
        count: Int,
        wallFilter: Int
    ): Flow<List<Post>>

    fun checkAndAddLike(accountId: Long, ownerId: Long, postId: Int): Flow<Int>
    fun getCachedWall(accountId: Long, ownerId: Long, wallFilter: Int): Flow<List<Post>>
    fun delete(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean>
    fun restore(accountId: Long, ownerId: Long, postId: Int): Flow<Boolean>
    fun reportPost(accountId: Long, owner_id: Long, post_id: Int, reason: Int): Flow<Int>
    fun subscribe(accountId: Long, owner_id: Long): Flow<Int>
    fun unsubscribe(accountId: Long, owner_id: Long): Flow<Int>
    fun getById(accountId: Long, ownerId: Long, postId: Int): Flow<Post>
    fun pinUnpin(accountId: Long, ownerId: Long, postId: Int, pin: Boolean): Flow<Boolean>

    /**
     * Ability to observe minor post changes (likes, deleted, pin state, etc.)
     */
    fun observeMinorChanges(): SharedFlow<PostUpdate>

    /**
     *
     */
    fun observeChanges(): SharedFlow<Post>

    /**
     * @return onNext в том случае, если пост перестал существовать
     */
    fun observePostInvalidation(): SharedFlow<IdPair>

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
    ): Flow<Post>

    fun post(accountId: Long, post: Post, fromGroup: Boolean, showSigner: Boolean): Flow<Post>
    fun repost(
        accountId: Long,
        postId: Int,
        ownerId: Long,
        groupId: Long?,
        message: String?
    ): Flow<Post>

    /**
     * Сохранить пост в базу с тем же локальным идентификатором
     *
     * @param accountId идентификатор аккаунта
     * @param post      пост
     * @return Single с локальным идентификатором
     */
    fun cachePostWithIdSaving(accountId: Long, post: Post): Flow<Int>

    /**
     * Удалить пост из кеша (используется только для "черновиков"
     *
     * @param accountId идентификатор аккаунта
     * @param postDbid  локальный идентификатор поста в БД
     * @return Completable
     */
    @CheckResult
    fun deleteFromCache(accountId: Long, postDbid: Int): Flow<Boolean>
}