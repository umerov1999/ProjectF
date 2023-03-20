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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IOwnersRepository {
    fun findFriendBirtday(accountId: Long): Single<List<User>>
    fun findBaseOwnersDataAsList(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Single<List<Owner>>

    fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Single<IOwnersBundle>

    fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int,
        alreadyExists: Collection<Owner>?
    ): Single<IOwnersBundle>

    fun getBaseOwnerInfo(accountId: Long, ownerId: Long, mode: Int): Single<Owner>
    fun getFullUserInfo(accountId: Long, userId: Long, mode: Int): Single<Pair<User?, UserDetails?>>
    fun getMarketAlbums(
        accountId: Long,
        owner_id: Long,
        offset: Int,
        count: Int
    ): Single<List<MarketAlbum>>

    fun getMarket(
        accountId: Long,
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        isService: Boolean
    ): Single<List<Market>>

    fun getGifts(accountId: Long, user_id: Long, count: Int, offset: Int): Single<List<Gift>>
    fun getMarketById(accountId: Long, ids: Collection<AccessIdPair>): Single<List<Market>>
    fun getFullCommunityInfo(
        accountId: Long,
        communityId: Long,
        mode: Int
    ): Single<Pair<Community?, CommunityDetails?>>

    fun cacheActualOwnersData(accountId: Long, ids: Collection<Long>): Completable
    fun getCommunitiesWhereAdmin(
        accountId: Long,
        admin: Boolean,
        editor: Boolean,
        moderator: Boolean
    ): Single<List<Owner>>

    fun searchPeoples(
        accountId: Long,
        criteria: PeopleSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<User>>

    fun insertOwners(accountId: Long, entities: OwnerEntities): Completable
    fun handleStatusChange(accountId: Long, userId: Long, status: String?): Completable
    fun handleOnlineChanges(
        accountId: Long,
        offlineUpdates: List<UserIsOfflineUpdate>?,
        onlineUpdates: List<UserIsOnlineUpdate>?
    ): Completable

    fun observeUpdates(): Flowable<List<UserUpdate>>
    fun report(accountId: Long, userId: Long, type: String?, comment: String?): Single<Int>
    fun checkAndAddFriend(accountId: Long, userId: Long): Single<Int>

    fun getGroupChats(
        accountId: Long,
        groupId: Long,
        offset: Int?,
        count: Int?
    ): Single<List<GroupChats>>

    companion object {
        const val MODE_ANY = 1
        const val MODE_NET = 2
        const val MODE_CACHE = 3
    }
}