package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface IRelativeshipStorage : IStorage {
    fun storeFriendsList(
        accountId: Long,
        userId: Long,
        data: Collection<FriendListEntity>
    ): Flow<Boolean>

    fun storeFriends(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    fun storeFollowers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    fun storeRequests(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    fun storeGroupMembers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    fun getFriends(accountId: Long, objectId: Long): Flow<List<UserEntity>>
    fun getGroupMembers(accountId: Long, groupId: Long): Flow<List<UserEntity>>
    fun getFollowers(accountId: Long, objectId: Long): Flow<List<UserEntity>>
    fun getRequests(accountId: Long): Flow<List<UserEntity>>
    fun getCommunities(accountId: Long, ownerId: Long): Flow<List<CommunityEntity>>
    fun storeCommunities(
        accountId: Long,
        communities: List<CommunityEntity>,
        userId: Long,
        invalidateBefore: Boolean
    ): Flow<Boolean>
}