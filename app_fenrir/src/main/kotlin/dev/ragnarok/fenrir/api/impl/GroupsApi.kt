package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IGroupsApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.GroupSettingsDto
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiBanned
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiGroupChats
import dev.ragnarok.fenrir.api.model.VKApiMarket
import dev.ragnarok.fenrir.api.model.VKApiMarketAlbum
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.GroupByIdResponse
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.api.model.response.GroupWallInfoResponse
import dev.ragnarok.fenrir.api.services.IGroupsService
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class GroupsApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IGroupsApi {
    override fun editManager(
        groupId: Long,
        userId: Long,
        role: String?,
        isContact: Boolean?,
        contactPosition: String?,
        contactEmail: String?,
        contactPhone: String?,
    ): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.editManager(
                    groupId,
                    userId,
                    role,
                    integerFromBoolean(isContact),
                    contactPosition,
                    contactEmail,
                    contactPhone
                )
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun edit(
        groupId: Long,
        title: String?,
        description: String?,
        screen_name: String?,
        access: Int?,
        website: String?,
        public_category: Int?,
        public_date: String?,
        age_limits: Int?,
        obscene_filter: Int?,
        obscene_stopwords: Int?,
        obscene_words: String?
    ): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.edit(
                    groupId,
                    title,
                    description,
                    screen_name,
                    access,
                    website,
                    public_date, age_limits, obscene_filter, obscene_stopwords, obscene_words
                )
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun unban(groupId: Long, ownerId: Long): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.unban(groupId, ownerId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun ban(
        groupId: Long,
        ownerId: Long,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Boolean?
    ): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.ban(
                    groupId,
                    ownerId,
                    endDate,
                    reason,
                    comment,
                    integerFromBoolean(commentVisible)
                )
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun getSettings(groupId: Long): Flow<GroupSettingsDto> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getSettings(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketAlbums(
        owner_id: Long,
        offset: Int,
        count: Int
    ): Flow<Items<VKApiMarketAlbum>> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getMarketAlbums(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarket(
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Flow<Items<VKApiMarket>> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getMarket(owner_id, album_id, offset, count, extended)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketServices(
        owner_id: Long,
        offset: Int,
        count: Int,
        extended: Int?
    ): Flow<Items<VKApiMarket>> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getMarketServices(owner_id, offset, count, extended)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketById(ids: Collection<AccessIdPair>): Flow<Items<VKApiMarket>> {
        val markets =
            join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getMarketById(markets, 1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBanned(
        groupId: Long,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Long?
    ): Flow<Items<VKApiBanned>> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getBanned(groupId, offset, count, fields, userId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getWallInfo(groupId: String?, fields: String?): Flow<VKApiCommunity> {
        return provideService(
            IGroupsService(),
            TokenType.USER,
            TokenType.SERVICE,
            TokenType.COMMUNITY
        )
            .flatMapConcat {
                it.getGroupWallInfo(
                    "var group_id = Args.group_id; var fields = Args.fields; var negative_group_id = -group_id; var group_info = API.groups.getById({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":group_id, \"fields\":fields}); var all_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"all\"}).count; var owner_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"owner\"}).count; var suggests_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"suggests\"}).count; var postponed_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"postponed\"}).count; var donut_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"donut\"}).count; return {\"group_info\": !group_info ? null : group_info, \"all_wall_count\":all_wall_count, \"owner_wall_count\":owner_wall_count, \"suggests_wall_count\":suggests_wall_count, \"postponed_wall_count\":postponed_wall_count, \"donut_wall_count\":donut_wall_count };",
                    groupId,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
            .map { response ->
                if (safeCountOf(response.group_info?.groups) != 1) {
                    throw NotFoundException()
                }
                createFrom(response)
            }
    }

    override fun getMembers(
        groupId: String?,
        sort: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        filter: String?
    ): Flow<Items<VKApiUser>> {
        return provideService(
            IGroupsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.getMembers(groupId, sort, offset, count, fields, filter)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?,
        type: String?,
        filter: String?,
        countryId: Int?,
        cityId: Int?,
        future: Boolean?,
        market: Boolean?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCommunity>> {
        return provideService(IGroupsService(), TokenType.USER)
            .flatMapConcat {
                it.search(
                    query, type, filter, countryId, cityId, integerFromBoolean(future),
                    integerFromBoolean(market), sort, offset, count
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun leave(groupId: Long): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER)
            .flatMapConcat {
                it.leave(groupId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun join(groupId: Long, notSure: Int?): Flow<Boolean> {
        return provideService(IGroupsService(), TokenType.USER)
            .flatMapConcat {
                it.join(groupId, notSure)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun get(
        userId: Long?,
        extended: Boolean?,
        filter: String?,
        fields: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCommunity>> {
        return provideService(IGroupsService(), TokenType.USER)
            .flatMapConcat {
                it[userId, integerFromBoolean(extended), filter, fields, offset, count]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLongPollServer(groupId: Long): Flow<GroupLongpollServer> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getLongPollServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(
        ids: Collection<Long>,
        domains: Collection<String>?,
        groupId: String?,
        fields: String?
    ): Flow<GroupByIdResponse> {
        val pds: ArrayList<String> = ArrayList(1)
        join(ids, ",")?.let { pds.add(it) }
        join(domains, ",")?.let { pds.add(it) }
        return provideService(
            IGroupsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.getById(join(pds, ","), groupId, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChats(
        groupId: Long,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiGroupChats>> {
        return provideService(IGroupsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getChats(groupId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        internal fun createFrom(info: GroupWallInfoResponse): VKApiCommunity {
            val community = info.group_info?.groups?.get(0) ?: throw NotFoundException()
            if (community.counters == null) {
                community.counters = VKApiCommunity.Counters()
            }
            info.allWallCount.requireNonNull {
                community.counters?.all_wall = it
            }
            info.ownerWallCount.requireNonNull {
                community.counters?.owner_wall = it
            }
            info.suggestsWallCount.requireNonNull {
                community.counters?.suggest_wall = it
            }
            info.postponedWallCount.requireNonNull {
                community.counters?.postponed_wall = it
            }
            info.donutWallCount.requireNonNull {
                community.counters?.donuts = it
            }
            return community
        }
    }
}
