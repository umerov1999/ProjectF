package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOfflineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOnlineUpdate
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunity
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunityDetails
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUser
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUserDetails
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUsers
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformGifts
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformGroupChats
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformMarket
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformMarketAlbums
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUsers
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunitiesFromDbos
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunityDetailsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildUserDetailsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildUsersFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.model.Gift
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SparseArrayOwnersBundle
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.UserUpdate
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyListFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import java.util.LinkedList

class OwnersRepository(private val networker: INetworker, private val cache: IOwnersStorage) :
    IOwnersRepository {
    private val userUpdatesPublisher = createPublishSubject<List<UserUpdate>>()
    private fun getCachedDetails(accountId: Long, userId: Long): Flow<Optional<UserDetails>> {
        return cache.getUserDetails(accountId, userId)
            .flatMapConcat { optional ->
                if (optional.isEmpty) {
                    toFlow(empty())
                } else {
                    val entity = optional.requireNonEmpty()
                    val requiredIds: MutableSet<Long> = HashSet(1)
                    entity.careers.nonNullNoEmpty {
                        for (career in it) {
                            if (career.groupId != 0L) {
                                requiredIds.add(-career.groupId)
                            }
                        }
                    }
                    entity.relatives.nonNullNoEmpty {
                        for (e in it) {
                            if (e.id > 0) {
                                requiredIds.add(e.id)
                            }
                        }
                    }
                    if (entity.relationPartnerId != 0L) {
                        requiredIds.add(entity.relationPartnerId)
                    }
                    findBaseOwnersDataAsBundle(accountId, requiredIds, IOwnersRepository.MODE_ANY)
                        .map { bundle ->
                            wrap(
                                buildUserDetailsFromDbo(
                                    entity, bundle
                                )
                            )
                        }
                }
            }
    }

    private fun getCachedGroupsDetails(
        accountId: Long,
        groupId: Long
    ): Flow<Optional<CommunityDetails>> {
        return cache.getGroupsDetails(accountId, groupId)
            .map { optional ->
                if (optional.isEmpty) {
                    empty()
                } else {
                    val entity = optional.requireNonEmpty()
                    wrap(
                        buildCommunityDetailsFromDbo(
                            entity
                        )
                    )
                }
            }
    }

    private fun getCachedGroupsFullData(
        accountId: Long,
        groupId: Long
    ): Flow<Pair<Community?, CommunityDetails?>> {
        return cache.findCommunityDboById(accountId, groupId)
            .zip(
                getCachedGroupsDetails(accountId, groupId)
            ) { groupsEntityOptional, groupsDetailsOptional ->
                create(map(groupsEntityOptional.get()), groupsDetailsOptional.get())
            }
    }

    private fun getCachedFullData(
        accountId: Long,
        userId: Long
    ): Flow<Pair<User?, UserDetails?>> {
        return cache.findUserDboById(accountId, userId)
            .zip(
                getCachedDetails(accountId, userId)
            ) { userEntityOptional, userDetailsOptional ->
                create(map(userEntityOptional.get()), userDetailsOptional.get())
            }
    }

    override fun report(
        accountId: Long,
        userId: Long,
        type: String?,
        comment: String?
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .users()
            .report(userId, type, comment)
    }

    override fun checkAndAddFriend(accountId: Long, userId: Long): Flow<Int> {
        return networker.vkDefault(accountId)
            .users()
            .checkAndAddFriend(userId)
    }

    override fun getFullUserInfo(
        accountId: Long,
        userId: Long,
        mode: Int
    ): Flow<Pair<User?, UserDetails?>> {
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return getCachedFullData(accountId, userId)
            IOwnersRepository.MODE_NET -> return networker.vkDefault(accountId)
                .users()
                .getUserWallInfo(userId, Fields.FIELDS_FULL_USER, null)
                .flatMapConcat { user ->
                    val userEntity = mapUser(user)
                    val detailsEntity = mapUserDetails(user)
                    cache.storeUserDbos(accountId, listOf(userEntity))
                        .andThen(cache.storeUserDetails(accountId, userId, detailsEntity))
                        .andThen(getCachedFullData(accountId, userId))
                }
        }
        throw UnsupportedOperationException("Unsupported mode: $mode")
    }

    override fun getMarketAlbums(
        accountId: Long,
        owner_id: Long,
        offset: Int,
        count: Int
    ): Flow<List<MarketAlbum>> {
        return networker.vkDefault(accountId)
            .groups()
            .getMarketAlbums(owner_id, offset, count)
            .map { obj -> listEmptyIfNull(obj.items) }
            .map { albums ->
                val market_albums: MutableList<MarketAlbum> = ArrayList(albums.size)
                market_albums.addAll(transformMarketAlbums(albums))
                market_albums
            }
    }

    override fun getMarket(
        accountId: Long,
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        isService: Boolean
    ): Flow<List<Market>> {
        if (isService) {
            return networker.vkDefault(accountId)
                .groups()
                .getMarketServices(owner_id, offset, count, 1)
                .map { obj -> listEmptyIfNull(obj.items) }
                .map { products ->
                    val market: MutableList<Market> = ArrayList(products.size)
                    market.addAll(transformMarket(products))
                    market
                }
        }
        return networker.vkDefault(accountId)
            .groups()
            .getMarket(owner_id, album_id, offset, count, 1)
            .map { obj -> listEmptyIfNull(obj.items) }
            .map { products ->
                val market: MutableList<Market> = ArrayList(products.size)
                market.addAll(transformMarket(products))
                market
            }
    }

    override fun getMarketById(
        accountId: Long,
        ids: Collection<AccessIdPair>
    ): Flow<List<Market>> {
        return networker.vkDefault(accountId)
            .groups()
            .getMarketById(ids)
            .map { obj -> listEmptyIfNull(obj.items) }
            .map { products ->
                val market: MutableList<Market> = ArrayList(products.size)
                market.addAll(transformMarket(products))
                market
            }
    }

    override fun getFullCommunityInfo(
        accountId: Long,
        communityId: Long,
        mode: Int
    ): Flow<Pair<Community?, CommunityDetails?>> {
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return getCachedGroupsFullData(accountId, communityId)
            IOwnersRepository.MODE_NET -> return networker.vkDefault(accountId)
                .groups()
                .getWallInfo(communityId.toString(), Fields.FIELDS_FULL_GROUP)
                .flatMapConcat { dto ->
                    val community = mapCommunity(dto)
                    val details = mapCommunityDetails(dto)
                    cache.storeCommunityDbos(accountId, listOf(community))
                        .andThen(cache.storeGroupsDetails(accountId, communityId, details))
                        .andThen(getCachedGroupsFullData(accountId, communityId))
                }
        }
        return toFlowThrowable(Exception("Not yet implemented"))
    }

    override fun findFriendBirtday(accountId: Long): Flow<List<User>> {
        return cache.findFriendBirtday(accountId)
    }

    override fun cacheActualOwnersData(accountId: Long, ids: Collection<Long>): Flow<Boolean> {
        var completable = emptyTaskFlow()
        val dividedIds = DividedIds(ids)
        if (dividedIds.gids.nonNullNoEmpty()) {
            completable = completable.andThen(
                networker.vkDefault(accountId)
                    .groups()
                    .getById(dividedIds.gids, null, null, Fields.FIELDS_BASE_GROUP)
                    .flatMapConcat {
                        cache.storeCommunityDbos(
                            accountId,
                            mapCommunities(it.groups)
                        )
                    })
        }
        if (dividedIds.uids.nonNullNoEmpty()) {
            completable = completable.andThen(
                networker.vkDefault(accountId)
                    .users()[dividedIds.uids, null, Fields.FIELDS_BASE_USER, null]
                    .flatMapConcat {
                        cache.storeUserDbos(
                            accountId,
                            mapUsers(it)
                        )
                    })
        }
        return completable
    }

    override fun getCommunitiesWhereAdmin(
        accountId: Long,
        admin: Boolean,
        editor: Boolean,
        moderator: Boolean
    ): Flow<List<Owner>> {
        val roles: MutableList<String> = ArrayList()
        if (admin) {
            roles.add("admin")
        }
        if (editor) {
            roles.add("editor")
        }
        if (moderator) {
            roles.add("moderator")
        }
        return networker.vkDefault(accountId)
            .groups()[accountId, true, join(
            roles,
            ","
        ) { it }, Fields.FIELDS_BASE_GROUP, null, 1000]
            .map { obj -> listEmptyIfNull(obj.items) }
            .map { groups ->
                val owners: MutableList<Owner> = ArrayList(groups.size)
                owners.addAll(transformCommunities(groups))
                owners
            }
    }

    override fun insertOwners(accountId: Long, entities: OwnerEntities): Flow<Boolean> {
        return cache.storeOwnerEntities(accountId, entities)
    }

    override fun handleStatusChange(accountId: Long, userId: Long, status: String?): Flow<Boolean> {
        val patch = UserPatch(userId).setStatus(UserPatch.Status(status))
        return applyPatchesThenPublish(accountId, listOf(patch))
    }

    override fun handleOnlineChanges(
        accountId: Long,
        offlineUpdates: List<UserIsOfflineUpdate>?,
        onlineUpdates: List<UserIsOnlineUpdate>?
    ): Flow<Boolean> {
        val patches: MutableList<UserPatch> = ArrayList()
        if (offlineUpdates.nonNullNoEmpty()) {
            for (update in offlineUpdates) {
                val lastSeeenUnixtime =
                    if (update.isTimeout) now() - 5 * 60 else update.timestamp
                patches.add(
                    UserPatch(update.userId).setOnlineUpdate(
                        UserPatch.Online(
                            false,
                            lastSeeenUnixtime,
                            0
                        )
                    )
                )
            }
        }
        if (onlineUpdates.nonNullNoEmpty()) {
            for (update in onlineUpdates) {
                patches.add(
                    UserPatch(update.userId).setOnlineUpdate(
                        UserPatch.Online(
                            true,
                            now(),
                            update.platform
                        )
                    )
                )
            }
        }
        return applyPatchesThenPublish(accountId, patches)
    }

    private fun applyPatchesThenPublish(accountId: Long, patches: List<UserPatch>): Flow<Boolean> {
        val updates: MutableList<UserUpdate> = ArrayList(patches.size)
        for (patch in patches) {
            val update = UserUpdate(accountId, patch.userId)
            patch.online.requireNonNull {
                update.online = UserUpdate.Online(
                    it.isOnline,
                    it.lastSeen,
                    it.platform
                )
            }
            patch.status.requireNonNull {
                update.status = it.status?.let { it1 -> UserUpdate.Status(it1) }
            }
            updates.add(update)
        }
        return cache.applyPathes(accountId, patches)
            .map {
                userUpdatesPublisher.emit(updates)
                true
            }
    }

    override fun observeUpdates(): SharedFlow<List<UserUpdate>> {
        return userUpdatesPublisher
    }

    override fun searchPeoples(
        accountId: Long,
        criteria: PeopleSearchCriteria,
        count: Int,
        offset: Int
    ): Flow<List<User>> {
        val q = criteria.query
        val sortOption = criteria.findOptionByKey<SpinnerOption>(PeopleSearchCriteria.KEY_SORT)
        val sort =
            if (sortOption?.value == null) null else sortOption.value?.id
        val fields = Fields.FIELDS_BASE_USER
        val city = criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_CITY)
        val country = criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_COUNTRY)
        val hometown = criteria.extractTextValueFromOption(PeopleSearchCriteria.KEY_HOMETOWN)
        val universityCountry =
            criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_UNIVERSITY_COUNTRY)
        val university = criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_UNIVERSITY)
        val universityYear =
            criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_UNIVERSITY_YEAR)
        val universityFaculty =
            criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_UNIVERSITY_FACULTY)
        val universityChair =
            criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_UNIVERSITY_CHAIR)
        val sexOption = criteria.findOptionByKey<SpinnerOption>(PeopleSearchCriteria.KEY_SEX)
        val sex = if (sexOption?.value == null) null else sexOption.value?.id
        val statusOption =
            criteria.findOptionByKey<SpinnerOption>(PeopleSearchCriteria.KEY_RELATIONSHIP)
        val status =
            if (statusOption?.value == null) null else statusOption.value?.id
        val ageFrom = criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_AGE_FROM)
        val ageTo = criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_AGE_TO)
        val birthDay = criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_BIRTHDAY_DAY)
        val birthMonthOption =
            criteria.findOptionByKey<SpinnerOption>(PeopleSearchCriteria.KEY_BIRTHDAY_MONTH)
        val birthMonth =
            if (birthMonthOption?.value == null) null else birthMonthOption.value?.id
        val birthYear =
            criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_BIRTHDAY_YEAR)
        val online = criteria.extractBoleanValueFromOption(PeopleSearchCriteria.KEY_ONLINE_ONLY)
        val hasPhoto =
            criteria.extractBoleanValueFromOption(PeopleSearchCriteria.KEY_WITH_PHOTO_ONLY)
        val schoolCountry =
            criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_SCHOOL_COUNTRY)
        val schoolCity = criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_SCHOOL_CITY)
        val schoolClass =
            criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_SCHOOL_CLASS)
        val school = criteria.extractDatabaseEntryValueId(PeopleSearchCriteria.KEY_SCHOOL)
        val schoolYear = criteria.extractNumberValueFromOption(PeopleSearchCriteria.KEY_SCHOOL_YEAR)
        val religion = criteria.extractTextValueFromOption(PeopleSearchCriteria.KEY_RELIGION)
        val interests = criteria.extractTextValueFromOption(PeopleSearchCriteria.KEY_INTERESTS)
        val company = criteria.extractTextValueFromOption(PeopleSearchCriteria.KEY_COMPANY)
        val position = criteria.extractTextValueFromOption(PeopleSearchCriteria.KEY_POSITION)
        val groupId = criteria.groupId
        val fromListOption =
            criteria.findOptionByKey<SpinnerOption>(PeopleSearchCriteria.KEY_FROM_LIST)
        val fromList =
            if (fromListOption?.value == null) null else fromListOption.value?.id
        var targetFromList: String? = null
        if (fromList != null) {
            when (fromList) {
                PeopleSearchCriteria.FromList.FRIENDS -> targetFromList = "friends"
                PeopleSearchCriteria.FromList.SUBSCRIPTIONS -> targetFromList = "subscriptions"
            }
        }
        return networker
            .vkDefault(accountId)
            .users()
            .search(
                q, sort, offset, count, fields, city, country, hometown, universityCountry,
                university, universityYear, universityFaculty, universityChair, sex, status,
                ageFrom, ageTo, birthDay, birthMonth, birthYear, online, hasPhoto, schoolCountry,
                schoolCity, schoolClass, school, schoolYear, religion, interests, company,
                position, groupId, targetFromList
            )
            .map { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                transformUsers(dtos)
            }
    }

    override fun getGifts(
        accountId: Long,
        user_id: Long,
        count: Int,
        offset: Int
    ): Flow<List<Gift>> {
        return networker.vkDefault(accountId)
            .users()
            .getGifts(user_id, count, offset)
            .map { dtos ->
                transformGifts(dtos.items)
            }
    }

    override fun findBaseOwnersDataAsList(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Flow<List<Owner>> {
        if (ids.isEmpty()) {
            return emptyListFlow()
        }
        val dividedIds = DividedIds(ids)
        return getUsers(accountId, dividedIds.uids, mode)
            .zip(
                getCommunities(
                    accountId,
                    dividedIds.gids,
                    mode
                )
            ) { users, communities ->
                val owners: MutableList<Owner> = ArrayList(users.size + communities.size)
                owners.addAll(users)
                owners.addAll(communities)
                owners
            }
    }

    override fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int
    ): Flow<IOwnersBundle> {
        if (ids.isEmpty()) {
            return toFlow(SparseArrayOwnersBundle(0))
        }
        val dividedIds = DividedIds(ids)
        return getUsers(accountId, dividedIds.uids, mode)
            .zip(getCommunities(accountId, dividedIds.gids, mode)) { users, communities ->
                val bundle = SparseArrayOwnersBundle(users.size + communities.size)
                bundle.putAll(users)
                bundle.putAll(communities)
                bundle
            }
    }

    override fun findBaseOwnersDataAsBundle(
        accountId: Long,
        ids: Collection<Long>,
        mode: Int,
        alreadyExists: Collection<Owner>?
    ): Flow<IOwnersBundle> {
        if (ids.isEmpty()) {
            return toFlow(SparseArrayOwnersBundle(0))
        }
        val bundle: IOwnersBundle = SparseArrayOwnersBundle(ids.size)
        if (alreadyExists != null) {
            bundle.putAll(alreadyExists)
        }

        val missing = bundle.getMissing(ids)
        return if (missing.isEmpty()) {
            toFlow(bundle)
        } else {
            findBaseOwnersDataAsList(accountId, missing, mode)
                .map { owners ->
                    bundle.putAll(owners)
                    bundle
                }
        }
    }

    override fun getBaseOwnerInfo(accountId: Long, ownerId: Long, mode: Int): Flow<Owner> {
        var pOwnerId = ownerId
        if (pOwnerId == 0L) {
            pOwnerId = Settings.get().accounts().current
            // return Single.error(new IllegalArgumentException("Zero owner id!!!"));
        }
        return if (pOwnerId > 0) {
            getUsers(accountId, listOf(pOwnerId), mode)
                .map { users ->
                    if (users.isEmpty()) {
                        throw NotFoundException()
                    }
                    users[0]
                }
        } else {
            getCommunities(accountId, listOf(-pOwnerId), mode)
                .map { communities ->
                    if (communities.isEmpty()) {
                        throw NotFoundException()
                    }
                    communities[0]
                }
        }
    }

    private fun getCommunities(
        accountId: Long,
        gids: List<Long>,
        mode: Int
    ): Flow<List<Community>> {
        if (gids.isEmpty()) {
            return emptyListFlow()
        }
        return when (mode) {
            IOwnersRepository.MODE_CACHE -> cache.findCommunityDbosByIds(accountId, gids)
                .map { obj -> buildCommunitiesFromDbos(obj) }

            IOwnersRepository.MODE_ANY -> cache.findCommunityDbosByIds(accountId, gids)
                .flatMapConcat { dbos ->
                    if (dbos.size == gids.size) {
                        toFlow(buildCommunitiesFromDbos(dbos))
                    } else {
                        getActualCommunitiesAndStore(accountId, gids)
                    }
                }

            IOwnersRepository.MODE_NET -> getActualCommunitiesAndStore(accountId, gids)
            else -> throw IllegalArgumentException("Invalid mode: $mode")
        }
    }

    private fun getActualUsersAndStore(
        accountId: Long,
        uids: Collection<Long>
    ): Flow<List<User>> {
        return networker.vkDefault(accountId)
            .users()[uids, null, Fields.FIELDS_BASE_USER, null]
            .flatMapConcat { dtos ->
                cache.storeUserDbos(accountId, mapUsers(dtos))
                    .map {
                        transformUsers(dtos)
                    }
            }
    }

    private fun getActualCommunitiesAndStore(
        accountId: Long,
        gids: List<Long>
    ): Flow<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()
            .getById(gids, null, null, Fields.FIELDS_BASE_GROUP)
            .flatMapConcat { dtos ->
                val communityEntities = mapCommunities(dtos.groups)
                val communities = transformCommunities(dtos.groups)
                cache.storeCommunityDbos(accountId, communityEntities)
                    .map {
                        communities
                    }
            }
    }

    override fun getGroupChats(
        accountId: Long,
        groupId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<GroupChats>> {
        return networker.vkDefault(accountId)
            .groups()
            .getChats(groupId, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { obj -> transformGroupChats(obj) }
    }

    private fun getUsers(accountId: Long, uids: List<Long>, mode: Int): Flow<List<User>> {
        if (uids.isEmpty()) {
            return emptyListFlow()
        }
        return when (mode) {
            IOwnersRepository.MODE_CACHE -> cache.findUserDbosByIds(accountId, uids)
                .map { obj -> buildUsersFromDbo(obj) }

            IOwnersRepository.MODE_ANY -> cache.findUserDbosByIds(accountId, uids)
                .flatMapConcat { dbos ->
                    if (dbos.size == uids.size) {
                        toFlow(buildUsersFromDbo(dbos))
                    } else {
                        getActualUsersAndStore(accountId, uids)
                    }
                }

            IOwnersRepository.MODE_NET -> getActualUsersAndStore(accountId, uids)
            else -> throw IllegalArgumentException("Invalid mode: $mode")
        }
    }

    private class DividedIds(ids: Collection<Long>) {
        val uids: MutableList<Long> = LinkedList()
        val gids: MutableList<Long> = LinkedList()

        init {
            for (id in ids) {
                when {
                    id > 0 -> {
                        uids.add(id)
                    }

                    id < 0 -> {
                        gids.add(-id)
                    }

                    else -> {
                        uids.add(Settings.get().accounts().current)
                        //throw new IllegalArgumentException("Zero owner id!!!");
                    }
                }
            }
        }
    }
}
