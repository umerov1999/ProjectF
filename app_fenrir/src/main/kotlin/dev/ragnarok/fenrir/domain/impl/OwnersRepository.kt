package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOfflineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOnlineUpdate
import dev.ragnarok.fenrir.api.model.response.StoryBlockResponce
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunity
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunityDetails
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUser
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUserDetails
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUsers
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunities
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformGifts
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformGroupChats
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformMarket
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformMarketAlbums
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformStory
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUsers
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunitiesFromDbos
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunityDetailsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildUserDetailsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildUsersFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.processors.PublishProcessor
import java.util.*

class OwnersRepository(private val networker: INetworker, private val cache: IOwnersStorage) :
    IOwnersRepository {
    private val userUpdatesPublisher = PublishProcessor.create<List<UserUpdate>>()
    private fun getCachedDetails(accountId: Int, userId: Int): Single<Optional<UserDetails>> {
        return cache.getUserDetails(accountId, userId)
            .flatMap { optional ->
                if (optional.isEmpty) {
                    return@flatMap Single.just(empty<UserDetails>())
                }
                val entity = optional.requireNonEmpty()
                val requiredIds: MutableSet<Int> = HashSet(1)
                entity.careers.nonNullNoEmpty {
                    for (career in it) {
                        if (career.groupId != 0) {
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
                if (entity.relationPartnerId != 0) {
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

    private fun getCachedGroupsDetails(
        accountId: Int,
        groupId: Int
    ): Single<Optional<CommunityDetails>> {
        return cache.getGroupsDetails(accountId, groupId)
            .flatMap { optional ->
                if (optional.isEmpty) {
                    return@flatMap Single.just(empty<CommunityDetails>())
                }
                val entity = optional.requireNonEmpty()
                Single.just(
                    wrap(
                        buildCommunityDetailsFromDbo(
                            entity
                        )
                    )
                )
            }
    }

    private fun getCachedGroupsFullData(
        accountId: Int,
        groupId: Int
    ): Single<Pair<Community?, CommunityDetails?>> {
        return cache.findCommunityDboById(accountId, groupId)
            .zipWith(
                getCachedGroupsDetails(accountId, groupId)
            ) { groupsEntityOptional: Optional<CommunityEntity>, groupsDetailsOptional: Optional<CommunityDetails> ->
                create(map(groupsEntityOptional.get()), groupsDetailsOptional.get())
            }
    }

    private fun getCachedFullData(accountId: Int, userId: Int): Single<Pair<User?, UserDetails?>> {
        return cache.findUserDboById(accountId, userId)
            .zipWith(
                getCachedDetails(accountId, userId)
            ) { userEntityOptional: Optional<UserEntity>, userDetailsOptional: Optional<UserDetails> ->
                create(map(userEntityOptional.get()), userDetailsOptional.get())
            }
    }

    override fun report(accountId: Int, userId: Int, type: String?, comment: String?): Single<Int> {
        return networker.vkDefault(accountId)
            .users()
            .report(userId, type, comment)
    }

    override fun checkAndAddFriend(accountId: Int, userId: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .users()
            .checkAndAddFriend(userId)
    }

    private fun parseParentStory(story: List<VKApiStory>, dtos: MutableList<VKApiStory>) {
        for (i in story) {
            i.parent_story.requireNonNull {
                dtos.add(it)
            }
        }
    }

    private fun parseStoryBlock(resp: StoryBlockResponce, dtos: MutableList<VKApiStory>) {
        resp.stories.nonNullNoEmpty {
            parseParentStory(it, dtos)
            dtos.addAll(it)
        }
        resp.grouped.nonNullNoEmpty {
            for (i in it) {
                parseStoryBlock(i, dtos)
            }
        }
    }

    override fun getNarratives(
        accountId: Int,
        owner_id: Int,
        offset: Int?,
        count: Int?
    ): Single<List<Narratives>> {
        return networker.vkDefault(accountId)
            .users()
            .getNarratives(owner_id, offset, count)
            .flatMap { story ->
                val dtos = listEmptyIfNull(story.items)
                Single.just(mapAll(dtos) { transform(it) })
            }
    }

    override fun getStoryById(accountId: Int, stories: List<AccessIdPair>): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .users()
            .getStoryById(stories, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos = listEmptyIfNull(story.items)
                val owners = transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val storiesDto: MutableList<Story> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            storiesDto.add(transformStory(dto, owners1))
                        }
                        storiesDto
                    }
            }
    }

    override fun getStory(accountId: Int, owner_id: Int?): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .users()
            .getStory(owner_id, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos_multy = listEmptyIfNull(story.items)
                val dtos: MutableList<VKApiStory> = ArrayList()
                for (itst in dtos_multy) {
                    parseStoryBlock(itst, dtos)
                }
                val owners = transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val blockAds = Settings.get().other().isAd_block_story_news
                        val stories: MutableList<Story> = ArrayList()
                        for (dto in dtos) {
                            if (dto.is_ads && blockAds) {
                                continue
                            }
                            stories.add(transformStory(dto, owners1))
                        }
                        stories
                    }
            }
    }

    override fun searchStory(accountId: Int, q: String?, mentioned_id: Int?): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .users()
            .searchStory(q, mentioned_id, 1000, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos_multy = listEmptyIfNull(story.items)
                val dtos: MutableList<VKApiStory> = ArrayList()
                for (itst in dtos_multy) {
                    parseStoryBlock(itst, dtos)
                }
                val owners = transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val stories: MutableList<Story> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            stories.add(transformStory(dto, owners1))
                        }
                        stories
                    }
            }
    }

    override fun getFullUserInfo(
        accountId: Int,
        userId: Int,
        mode: Int
    ): Single<Pair<User?, UserDetails?>> {
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return getCachedFullData(accountId, userId)
            IOwnersRepository.MODE_NET -> return networker.vkDefault(accountId)
                .users()
                .getUserWallInfo(userId, Fields.FIELDS_FULL_USER, null)
                .flatMap { user ->
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
        accountId: Int,
        owner_id: Int,
        offset: Int,
        count: Int
    ): Single<List<MarketAlbum>> {
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
        accountId: Int,
        owner_id: Int,
        album_id: Int?,
        offset: Int,
        count: Int,
        isService: Boolean
    ): Single<List<Market>> {
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
        accountId: Int,
        ids: Collection<AccessIdPair>
    ): Single<List<Market>> {
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
        accountId: Int,
        communityId: Int,
        mode: Int
    ): Single<Pair<Community?, CommunityDetails?>> {
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return getCachedGroupsFullData(accountId, communityId)
            IOwnersRepository.MODE_NET -> return networker.vkDefault(accountId)
                .groups()
                .getWallInfo(communityId.toString(), Fields.FIELDS_FULL_GROUP)
                .flatMap { dto ->
                    val community = mapCommunity(dto)
                    val details = mapCommunityDetails(dto)
                    cache.storeCommunityDbos(accountId, listOf(community))
                        .andThen(cache.storeGroupsDetails(accountId, communityId, details))
                        .andThen(getCachedGroupsFullData(accountId, communityId))
                }
        }
        return Single.error(Exception("Not yet implemented"))
    }

    override fun findFriendBirtday(accountId: Int): Single<List<User>> {
        return cache.findFriendBirtday(accountId)
    }

    override fun cacheActualOwnersData(accountId: Int, ids: Collection<Int>): Completable {
        var completable = Completable.complete()
        val dividedIds = DividedIds(ids)
        if (dividedIds.gids.nonNullNoEmpty()) {
            completable = completable.andThen(
                networker.vkDefault(accountId)
                    .groups()
                    .getById(dividedIds.gids, null, null, Fields.FIELDS_BASE_GROUP)
                    .flatMapCompletable { communities: List<VKApiCommunity>? ->
                        cache.storeCommunityDbos(
                            accountId,
                            mapCommunities(communities)
                        )
                    })
        }
        if (dividedIds.uids.nonNullNoEmpty()) {
            completable = completable.andThen(
                networker.vkDefault(accountId)
                    .users()[dividedIds.uids, null, Fields.FIELDS_BASE_USER, null]
                    .flatMapCompletable {
                        cache.storeUserDbos(
                            accountId,
                            mapUsers(it)
                        )
                    })
        }
        return completable
    }

    override fun getCommunitiesWhereAdmin(
        accountId: Int,
        admin: Boolean,
        editor: Boolean,
        moderator: Boolean
    ): Single<List<Owner>> {
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
            ",",
            object : Utils.SimpleFunction<String, String> {
                override fun apply(orig: String): String {
                    return orig
                }
            }), Fields.FIELDS_BASE_GROUP, null, 1000]
            .map { obj -> listEmptyIfNull(obj.items) }
            .map { groups ->
                val owners: MutableList<Owner> = ArrayList(groups.size)
                owners.addAll(transformCommunities(groups))
                owners
            }
    }

    override fun insertOwners(accountId: Int, entities: OwnerEntities): Completable {
        return cache.storeOwnerEntities(accountId, entities)
    }

    override fun handleStatusChange(accountId: Int, userId: Int, status: String?): Completable {
        val patch = UserPatch(userId).setStatus(UserPatch.Status(status))
        return applyPatchesThenPublish(accountId, listOf(patch))
    }

    override fun handleOnlineChanges(
        accountId: Int,
        offlineUpdates: List<UserIsOfflineUpdate>?,
        onlineUpdates: List<UserIsOnlineUpdate>?
    ): Completable {
        val patches: MutableList<UserPatch> = ArrayList()
        if (offlineUpdates.nonNullNoEmpty()) {
            for (update in offlineUpdates) {
                val lastSeeenUnixtime =
                    if (update.isTimeout) now() - 5 * 60 else update.timestamp.toLong()
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

    private fun applyPatchesThenPublish(accountId: Int, patches: List<UserPatch>): Completable {
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
            .doOnComplete { userUpdatesPublisher.onNext(updates) }
    }

    override fun observeUpdates(): Flowable<List<UserUpdate>> {
        return userUpdatesPublisher.onBackpressureBuffer()
    }

    override fun searchPeoples(
        accountId: Int,
        criteria: PeopleSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<User>> {
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
        accountId: Int,
        user_id: Int,
        count: Int,
        offset: Int
    ): Single<List<Gift>> {
        return networker.vkDefault(accountId)
            .users()
            .getGifts(user_id, count, offset)
            .flatMap { dtos ->
                val gifts = transformGifts(dtos.items)
                Single.just(gifts)
            }
    }

    override fun findBaseOwnersDataAsList(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int
    ): Single<List<Owner>> {
        if (ids.isEmpty()) {
            return Single.just(emptyList())
        }
        val dividedIds = DividedIds(ids)
        return getUsers(accountId, dividedIds.uids, mode)
            .zipWith(
                getCommunities(
                    accountId,
                    dividedIds.gids,
                    mode
                )
            ) { users: List<User>, communities: List<Community> ->
                val owners: MutableList<Owner> = ArrayList(users.size + communities.size)
                owners.addAll(users)
                owners.addAll(communities)
                owners
            }
    }

    override fun findBaseOwnersDataAsBundle(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int
    ): Single<IOwnersBundle> {
        if (ids.isEmpty()) {
            return Single.just(SparseArrayOwnersBundle(0))
        }
        val dividedIds = DividedIds(ids)
        return getUsers(accountId, dividedIds.uids, mode)
            .zipWith(getCommunities(accountId, dividedIds.gids, mode), TO_BUNDLE_FUNCTION)
    }

    override fun findBaseOwnersDataAsBundle(
        accountId: Int,
        ids: Collection<Int>,
        mode: Int,
        alreadyExists: Collection<Owner>?
    ): Single<IOwnersBundle> {
        if (ids.isEmpty()) {
            return Single.just(SparseArrayOwnersBundle(0))
        }
        val b: IOwnersBundle = SparseArrayOwnersBundle(ids.size)
        if (alreadyExists != null) {
            b.putAll(alreadyExists)
        }
        return Single.just(b)
            .flatMap { bundle ->
                val missing = bundle.getMissing(ids)
                if (missing.isEmpty()) {
                    return@flatMap Single.just(bundle)
                }
                findBaseOwnersDataAsList(accountId, missing, mode)
                    .map { owners ->
                        bundle.putAll(owners)
                        bundle
                    }
            }
    }

    override fun getBaseOwnerInfo(accountId: Int, ownerId: Int, mode: Int): Single<Owner> {
        var pOwnerId = ownerId
        if (pOwnerId == 0) {
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
        accountId: Int,
        gids: List<Int>,
        mode: Int
    ): Single<List<Community>> {
        if (gids.isEmpty()) {
            return Single.just(emptyList())
        }
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return cache.findCommunityDbosByIds(accountId, gids)
                .map { obj -> buildCommunitiesFromDbos(obj) }
            IOwnersRepository.MODE_ANY -> return cache.findCommunityDbosByIds(accountId, gids)
                .flatMap { dbos ->
                    if (dbos.size == gids.size) {
                        return@flatMap Single.just(buildCommunitiesFromDbos(dbos))
                    }
                    getActualCommunitiesAndStore(accountId, gids)
                }
            IOwnersRepository.MODE_NET -> return getActualCommunitiesAndStore(accountId, gids)
        }
        throw IllegalArgumentException("Invalid mode: $mode")
    }

    private fun getActualUsersAndStore(accountId: Int, uids: Collection<Int>): Single<List<User>> {
        return networker.vkDefault(accountId)
            .users()[uids, null, Fields.FIELDS_BASE_USER, null]
            .flatMap { dtos ->
                cache.storeUserDbos(accountId, mapUsers(dtos))
                    .andThen(Single.just(transformUsers(dtos)))
            }
    }

    private fun getActualCommunitiesAndStore(
        accountId: Int,
        gids: List<Int>
    ): Single<List<Community>> {
        return networker.vkDefault(accountId)
            .groups()
            .getById(gids, null, null, Fields.FIELDS_BASE_GROUP)
            .flatMap { dtos ->
                val communityEntities = mapCommunities(dtos)
                val communities = transformCommunities(dtos)
                cache.storeCommunityDbos(accountId, communityEntities)
                    .andThen(Single.just(communities))
            }
    }

    override fun getGroupChats(
        accountId: Int,
        groupId: Int,
        offset: Int?,
        count: Int?
    ): Single<List<GroupChats>> {
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

    private fun getUsers(accountId: Int, uids: List<Int>, mode: Int): Single<List<User>> {
        if (uids.isEmpty()) {
            return Single.just(emptyList())
        }
        when (mode) {
            IOwnersRepository.MODE_CACHE -> return cache.findUserDbosByIds(accountId, uids)
                .map { obj -> buildUsersFromDbo(obj) }
            IOwnersRepository.MODE_ANY -> return cache.findUserDbosByIds(accountId, uids)
                .flatMap { dbos ->
                    if (dbos.size == uids.size) {
                        return@flatMap Single.just(buildUsersFromDbo(dbos))
                    }
                    getActualUsersAndStore(accountId, uids)
                }
            IOwnersRepository.MODE_NET -> return getActualUsersAndStore(accountId, uids)
        }
        throw IllegalArgumentException("Invalid mode: $mode")
    }

    private class DividedIds(ids: Collection<Int>) {
        val uids: MutableList<Int>
        val gids: MutableList<Int>

        init {
            uids = LinkedList()
            gids = LinkedList()
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

    companion object {
        private val TO_BUNDLE_FUNCTION =
            BiFunction<List<User>, List<Community>, IOwnersBundle> { users: List<User>, communities: List<Community> ->
                val bundle = SparseArrayOwnersBundle(users.size + communities.size)
                bundle.putAll(users)
                bundle.putAll(communities)
                bundle
            }
    }
}
