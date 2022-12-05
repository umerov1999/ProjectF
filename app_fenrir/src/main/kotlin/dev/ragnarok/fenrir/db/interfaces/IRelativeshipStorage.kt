package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IRelativeshipStorage : IStorage {
    fun storeFriendsList(
        accountId: Int,
        userId: Int,
        data: Collection<FriendListEntity>
    ): Completable

    fun storeFriends(
        accountId: Int,
        users: List<UserEntity>,
        objectId: Int,
        clearBeforeStore: Boolean
    ): Completable

    fun storeFollowers(
        accountId: Int,
        users: List<UserEntity>,
        objectId: Int,
        clearBeforeStore: Boolean
    ): Completable

    fun storeRequests(
        accountId: Int,
        users: List<UserEntity>,
        objectId: Int,
        clearBeforeStore: Boolean
    ): Completable

    fun storeGroupMembers(
        accountId: Int,
        users: List<UserEntity>,
        objectId: Int,
        clearBeforeStore: Boolean
    ): Completable

    fun getFriends(accountId: Int, objectId: Int): Single<List<UserEntity>>
    fun getGroupMembers(accountId: Int, groupId: Int): Single<List<UserEntity>>
    fun getFollowers(accountId: Int, objectId: Int): Single<List<UserEntity>>
    fun getRequests(accountId: Int): Single<List<UserEntity>>
    fun getCommunities(accountId: Int, ownerId: Int): Single<List<CommunityEntity>>
    fun storeComminities(
        accountId: Int,
        communities: List<CommunityEntity>,
        userId: Int,
        invalidateBefore: Boolean
    ): Completable
}