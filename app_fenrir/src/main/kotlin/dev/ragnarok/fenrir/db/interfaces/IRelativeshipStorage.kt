package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IRelativeshipStorage : IStorage {
    fun storeFriendsList(
        accountId: Long,
        userId: Long,
        data: Collection<FriendListEntity>
    ): Completable

    fun storeFriends(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable

    fun storeFollowers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable

    fun storeRequests(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable

    fun storeGroupMembers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable

    fun getFriends(accountId: Long, objectId: Long): Single<List<UserEntity>>
    fun getGroupMembers(accountId: Long, groupId: Long): Single<List<UserEntity>>
    fun getFollowers(accountId: Long, objectId: Long): Single<List<UserEntity>>
    fun getRequests(accountId: Long): Single<List<UserEntity>>
    fun getCommunities(accountId: Long, ownerId: Long): Single<List<CommunityEntity>>
    fun storeComminities(
        accountId: Long,
        communities: List<CommunityEntity>,
        userId: Long,
        invalidateBefore: Boolean
    ): Completable
}