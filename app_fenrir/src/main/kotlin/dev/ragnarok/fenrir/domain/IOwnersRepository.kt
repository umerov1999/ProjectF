package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOfflineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOnlineUpdate
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.model.Gift
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.UserUpdate
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IOwnersRepository {
    fun findFriendBirtday(accountId: Long): Flow<List<User>>
    fun findBaseOwnersDataAsList(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Flow<List<Owner>>

    fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Flow<IOwnersBundle>

    fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int,
        alreadyExists: Collection<Owner>?
    ): Flow<IOwnersBundle>

    fun getBaseOwnerInfo(accountId: Long, ownerId: Long, mode: Int): Flow<Owner>
    fun getFullUserInfo(accountId: Long, userId: Long, mode: Int): Flow<Pair<User?, UserDetails?>>
    fun getMarketAlbums(
        accountId: Long,
        owner_id: Long,
        offset: Int,
        count: Int
    ): Flow<List<MarketAlbum>>

    fun getMarket(
        accountId: Long,
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        isService: Boolean
    ): Flow<List<Market>>

    fun getGifts(accountId: Long, user_id: Long, count: Int, offset: Int): Flow<List<Gift>>
    fun getMarketById(accountId: Long, ids: Collection<AccessIdPair>): Flow<List<Market>>
    fun getFullCommunityInfo(
        accountId: Long,
        communityId: Long,
        mode: Int
    ): Flow<Pair<Community?, CommunityDetails?>>

    fun cacheActualOwnersData(accountId: Long, ids: Collection<Long>): Flow<Boolean>
    fun getCommunitiesWhereAdmin(
        accountId: Long,
        admin: Boolean,
        editor: Boolean,
        moderator: Boolean
    ): Flow<List<Owner>>

    fun searchPeoples(
        accountId: Long,
        criteria: PeopleSearchCriteria,
        count: Int,
        offset: Int
    ): Flow<List<User>>

    fun insertOwners(accountId: Long, entities: OwnerEntities): Flow<Boolean>
    fun handleStatusChange(accountId: Long, userId: Long, status: String?): Flow<Boolean>
    fun handleOnlineChanges(
        accountId: Long,
        offlineUpdates: List<UserIsOfflineUpdate>?,
        onlineUpdates: List<UserIsOnlineUpdate>?
    ): Flow<Boolean>

    fun observeUpdates(): SharedFlow<List<UserUpdate>>
    fun report(accountId: Long, userId: Long, type: String?, comment: String?): Flow<Int>
    fun checkAndAddFriend(accountId: Long, userId: Long): Flow<Int>

    fun getGroupChats(
        accountId: Long,
        groupId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<GroupChats>>

    companion object {
        const val MODE_ANY = 1
        const val MODE_NET = 2
        const val MODE_CACHE = 3
    }
}