package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOfflineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOnlineUpdate
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IOwnersRepository {
    fun findFriendBirtday(accountId: Int): Single<List<User>>
    fun findBaseOwnersDataAsList(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int
    ): Single<List<Owner>>

    fun findBaseOwnersDataAsBundle(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int
    ): Single<IOwnersBundle>

    fun findBaseOwnersDataAsBundle(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int,
        alreadyExists: Collection<Owner>?
    ): Single<IOwnersBundle>

    fun getBaseOwnerInfo(accountId: Int, ownerId: Int, mode: Int): Single<Owner>
    fun getFullUserInfo(accountId: Int, userId: Int, mode: Int): Single<Pair<User?, UserDetails?>>
    fun getMarketAlbums(
        accountId: Int,
        owner_id: Int,
        offset: Int,
        count: Int
    ): Single<List<MarketAlbum>>

    fun getMarket(
        accountId: Int,
        owner_id: Int,
        album_id: Int?,
        offset: Int,
        count: Int,
        isService: Boolean
    ): Single<List<Market>>

    fun getGifts(accountId: Int, user_id: Int, count: Int, offset: Int): Single<List<Gift>>
    fun getMarketById(accountId: Int, ids: Collection<AccessIdPair>): Single<List<Market>>
    fun getFullCommunityInfo(
        accountId: Int,
        communityId: Int,
        mode: Int
    ): Single<Pair<Community?, CommunityDetails?>>

    fun cacheActualOwnersData(accountId: Int, ids: Collection<Int>): Completable
    fun getCommunitiesWhereAdmin(
        accountId: Int,
        admin: Boolean,
        editor: Boolean,
        moderator: Boolean
    ): Single<List<Owner>>

    fun getStoryById(accountId: Int, stories: List<AccessIdPair>): Single<List<Story>>

    fun searchPeoples(
        accountId: Int,
        criteria: PeopleSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<User>>

    fun insertOwners(accountId: Int, entities: OwnerEntities): Completable
    fun handleStatusChange(accountId: Int, userId: Int, status: String?): Completable
    fun handleOnlineChanges(
        accountId: Int,
        offlineUpdates: List<UserIsOfflineUpdate>?,
        onlineUpdates: List<UserIsOnlineUpdate>?
    ): Completable

    fun observeUpdates(): Flowable<List<UserUpdate>>
    fun report(accountId: Int, userId: Int, type: String?, comment: String?): Single<Int>
    fun checkAndAddFriend(accountId: Int, userId: Int): Single<Int>
    fun getStory(accountId: Int, owner_id: Int?): Single<List<Story>>
    fun getNarratives(
        accountId: Int,
        owner_id: Int,
        offset: Int?,
        count: Int?
    ): Single<List<Narratives>>

    fun searchStory(accountId: Int, q: String?, mentioned_id: Int?): Single<List<Story>>
    fun getGroupChats(
        accountId: Int,
        groupId: Int,
        offset: Int?,
        count: Int?
    ): Single<List<GroupChats>>

    companion object {
        const val MODE_ANY = 1
        const val MODE_NET = 2
        const val MODE_CACHE = 3
    }
}