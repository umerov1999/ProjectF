package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Single

interface IRelationshipInteractor {
    fun getCachedGroupMembers(accountId: Long, groupId: Long): Single<List<Owner>>
    fun getGroupMembers(
        accountId: Long,
        groupId: Long,
        offset: Int,
        count: Int,
        filter: String?
    ): Single<List<Owner>>

    fun getCachedFriends(accountId: Long, objectId: Long): Single<List<User>>
    fun getCachedFollowers(accountId: Long, objectId: Long): Single<List<User>>
    fun getCachedRequests(accountId: Long): Single<List<User>>
    fun getActualFriendsList(
        accountId: Long,
        objectId: Long,
        count: Int?,
        offset: Int
    ): Single<List<User>>

    fun getOnlineFriends(
        accountId: Long,
        objectId: Long,
        count: Int,
        offset: Int
    ): Single<List<User>>

    fun getRecommendations(accountId: Long, count: Int?): Single<List<User>>
    fun deleteSubscriber(accountId: Long, subscriber_id: Long): Single<Int>
    fun getFollowers(accountId: Long, objectId: Long, count: Int, offset: Int): Single<List<User>>
    fun getMutualFriends(
        accountId: Long,
        objectId: Long,
        count: Int,
        offset: Int
    ): Single<List<User>>

    fun getRequests(accountId: Long, offset: Int?, count: Int?, store: Boolean): Single<List<User>>
    fun searchFriends(
        accountId: Long,
        userId: Long,
        count: Int,
        offset: Int,
        q: String?
    ): Single<Pair<List<User>, Int>>

    fun getFriendsCounters(accountId: Long, userId: Long): Single<FriendsCounters>
    fun addFriend(
        accountId: Long,
        userId: Long,
        optionalText: String?,
        keepFollow: Boolean
    ): Single<Int>

    fun deleteFriends(accountId: Long, userId: Long): Single<Int>
    interface DeletedCodes {
        companion object {
            const val FRIEND_DELETED = 1
            const val OUT_REQUEST_DELETED = 2
            const val IN_REQUEST_DELETED = 3
            const val SUGGESTION_DELETED = 4
        }
    }

    companion object {
        const val FRIEND_ADD_REQUEST_SENT = 1
        const val FRIEND_ADD_REQUEST_FROM_USER_APPROVED = 2
        const val FRIEND_ADD_RESENDING = 4
    }
}