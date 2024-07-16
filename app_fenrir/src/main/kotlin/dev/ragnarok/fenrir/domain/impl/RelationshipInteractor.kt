package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.IRelationshipInteractor.DeletedCodes
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUsers
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUsers
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildOwnerUsersFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildUsersFromDbo
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.exception.UnepectedResultException
import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class RelationshipInteractor(
    private val repositories: IStorages,
    private val networker: INetworker
) : IRelationshipInteractor {
    override fun getCachedGroupMembers(accountId: Long, groupId: Long): Flow<List<Owner>> {
        return repositories.relativeship()
            .getGroupMembers(accountId, groupId)
            .map { obj -> buildOwnerUsersFromDbo(obj) }
    }

    override fun getGroupMembers(
        accountId: Long,
        groupId: Long,
        offset: Int,
        count: Int,
        filter: String?
    ): Flow<List<Owner>> {
        return networker.vkDefault(accountId)
            .groups()
            .getMembers(
                groupId.toString(),
                null,
                offset,
                count,
                Fields.FIELDS_BASE_OWNER,
                filter
            )
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                if (filter.isNullOrEmpty()) {
                    val dbos = mapUsers(dtos)
                    repositories.relativeship()
                        .storeGroupMembers(accountId, dbos, groupId, offset == 0)
                        .map {
                            Dto2Model.transformOwners(dtos, null)
                        }
                } else {
                    toFlow(Dto2Model.transformOwners(dtos, null))
                }
            }
    }

    override fun getCachedFriends(accountId: Long, objectId: Long): Flow<List<User>> {
        return repositories.relativeship()
            .getFriends(accountId, objectId)
            .map { obj -> buildUsersFromDbo(obj) }
    }

    override fun getCachedFollowers(accountId: Long, objectId: Long): Flow<List<User>> {
        return repositories.relativeship()
            .getFollowers(accountId, objectId)
            .map { obj -> buildUsersFromDbo(obj) }
    }

    override fun getCachedRequests(accountId: Long): Flow<List<User>> {
        return repositories.relativeship()
            .getRequests(accountId)
            .map { obj -> buildUsersFromDbo(obj) }
    }

    override fun getActualFriendsList(
        accountId: Long,
        objectId: Long,
        count: Int?,
        offset: Int
    ): Flow<List<User>> {
        val order = if (accountId == objectId) "hints" else null
        return networker.vkDefault(accountId)
            .friends()[objectId, order, null, count, offset, Fields.FIELDS_BASE_USER, null]
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val dbos = mapUsers(dtos)
                val users = transformUsers(dtos)
                repositories.relativeship()
                    .storeFriends(accountId, dbos, objectId, offset == 0)
                    .map {
                        users
                    }
            }
    }

    override fun getOnlineFriends(
        accountId: Long,
        objectId: Long,
        count: Int,
        offset: Int
    ): Flow<List<User>> {
        val order =
            if (accountId == objectId) "hints" else null // hints (сортировка по популярности) доступна только для своих друзей
        return networker.vkDefault(accountId)
            .friends()
            .getOnline(objectId, order, count, offset, Fields.FIELDS_BASE_USER)
            .map { response -> listEmptyIfNull(response.profiles) }
            .map { obj -> transformUsers(obj) }
    }

    override fun getRecommendations(accountId: Long, count: Int?): Flow<List<User>> {
        return networker.vkDefault(accountId)
            .friends()
            .getRecommendations(count, Fields.FIELDS_BASE_USER, null)
            .map { response -> listEmptyIfNull(response.items) }
            .map { obj -> transformUsers(obj) }
    }

    override fun deleteSubscriber(accountId: Long, subscriber_id: Long): Flow<Int> {
        return networker.vkDefault(accountId)
            .friends()
            .deleteSubscriber(subscriber_id)
    }

    override fun getFollowers(
        accountId: Long,
        objectId: Long,
        count: Int,
        offset: Int
    ): Flow<List<User>> {
        return networker.vkDefault(accountId)
            .users()
            .getFollowers(objectId, offset, count, Fields.FIELDS_BASE_USER, null)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val dbos = mapUsers(dtos)
                val users = transformUsers(dtos)
                repositories.relativeship()
                    .storeFollowers(accountId, dbos, objectId, offset == 0)
                    .map {
                        users
                    }
            }
    }

    override fun getRequests(
        accountId: Long,
        offset: Int?,
        count: Int?,
        store: Boolean
    ): Flow<List<User>> {
        return networker.vkDefault(accountId)
            .users()
            .getRequests(offset, count, 1, 1, Fields.FIELDS_BASE_USER)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val dbos = mapUsers(dtos)
                val users = transformUsers(dtos)
                if (store) {
                    repositories.relativeship()
                        .storeRequests(accountId, dbos, accountId, offset == 0)
                        .map {
                            users
                        }
                } else {
                    toFlow(users)
                }
            }
    }

    override fun getMutualFriends(
        accountId: Long,
        objectId: Long,
        count: Int,
        offset: Int
    ): Flow<List<User>> {
        return networker.vkDefault(accountId)
            .friends()
            .getMutual(accountId, objectId, count, offset, Fields.FIELDS_BASE_USER)
            .map { obj -> transformUsers(obj) }
    }

    override fun searchFriends(
        accountId: Long,
        userId: Long,
        count: Int,
        offset: Int,
        q: String?
    ): Flow<Pair<List<User>, Int>> {
        return networker.vkDefault(accountId)
            .friends()
            .search(userId, q, Fields.FIELDS_BASE_USER, null, offset, count)
            .map { items ->
                val users = transformUsers(listEmptyIfNull(items.items))
                create(users, items.count)
            }
    }

    override fun getFriendsCounters(accountId: Long, userId: Long): Flow<FriendsCounters> {
        return networker.vkDefault(accountId)
            .users()[listOf(userId), null, "counters", null]
            .map { users ->
                if (users.isEmpty()) {
                    throw NotFoundException()
                }
                val user = users[0]
                val counters: FriendsCounters = if (user.counters != null) {
                    FriendsCounters(
                        user.counters?.friends.orZero(),
                        user.counters?.online_friends.orZero(),
                        user.counters?.followers.orZero(),
                        user.counters?.mutual_friends.orZero()
                    )
                } else {
                    FriendsCounters(0, 0, 0, 0)
                }
                counters
            }
    }

    override fun addFriend(
        accountId: Long,
        userId: Long,
        optionalText: String?,
        keepFollow: Boolean
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .friends()
            .add(userId, optionalText, keepFollow)
    }

    override fun deleteFriends(accountId: Long, userId: Long): Flow<Int> {
        return networker.vkDefault(accountId)
            .friends()
            .delete(userId)
            .map { response ->
                when {
                    response.friend_deleted -> {
                        DeletedCodes.FRIEND_DELETED
                    }

                    response.in_request_deleted -> {
                        DeletedCodes.IN_REQUEST_DELETED
                    }

                    response.out_request_deleted -> {
                        DeletedCodes.OUT_REQUEST_DELETED
                    }

                    response.suggestion_deleted -> {
                        DeletedCodes.SUGGESTION_DELETED
                    }

                    else -> throw UnepectedResultException()
                }
            }
    }
}
