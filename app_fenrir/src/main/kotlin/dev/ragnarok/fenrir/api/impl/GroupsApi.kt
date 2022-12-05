package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IGroupsApi
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.api.model.response.GroupWallInfoResponse
import dev.ragnarok.fenrir.api.services.IGroupsService
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

internal class GroupsApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IGroupsApi {
    override fun editManager(
        groupId: Int,
        userId: Int,
        role: String?,
        isContact: Boolean?,
        contactPosition: String?,
        contactEmail: String?,
        contactPhone: String?,
    ): Completable {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service
                    .editManager(
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
        groupId: Int,
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
    ): Completable {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service
                    .edit(
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

    override fun unban(groupId: Int, ownerId: Int): Completable {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service
                    .unban(groupId, ownerId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun ban(
        groupId: Int,
        ownerId: Int,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Boolean?
    ): Completable {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service
                    .ban(
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

    override fun getSettings(groupId: Int): Single<GroupSettingsDto> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getSettings(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketAlbums(
        owner_id: Int,
        offset: Int,
        count: Int
    ): Single<Items<VKApiMarketAlbum>> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getMarketAlbums(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarket(
        owner_id: Int,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getMarket(owner_id, album_id, offset, count, extended)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketServices(
        owner_id: Int,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getMarketServices(owner_id, offset, count, extended)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMarketById(ids: Collection<AccessIdPair>): Single<Items<VKApiMarket>> {
        val markets =
            join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getMarketById(markets, 1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBanned(
        groupId: Int,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Int?
    ): Single<Items<VKApiBanned>> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getBanned(groupId, offset, count, fields, userId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getWallInfo(groupId: String?, fields: String?): Single<VKApiCommunity> {
        return provideService(
            IGroupsService::class.java,
            TokenType.USER,
            TokenType.SERVICE,
            TokenType.COMMUNITY
        )
            .flatMap { service ->
                service.getGroupWallInfo(
                    "var group_id = Args.group_id; var fields = Args.fields; var negative_group_id = -group_id; var group_info = API.groups.getById({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":group_id, \"fields\":fields}); var all_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"all\"}).count; var owner_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"owner\"}).count; var suggests_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"suggests\"}).count; var postponed_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"postponed\"}).count; var donut_wall_count = API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":negative_group_id, \"count\":1, \"filter\":\"donut\"}).count; return {\"group_info\": !group_info ? null : group_info, \"all_wall_count\":all_wall_count, \"owner_wall_count\":owner_wall_count, \"suggests_wall_count\":suggests_wall_count, \"postponed_wall_count\":postponed_wall_count, \"donut_wall_count\":donut_wall_count };",
                    groupId,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
            .map { response ->
                if (safeCountOf(response.groups) != 1) {
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
    ): Single<Items<VKApiUser>> {
        return provideService(
            IGroupsService::class.java,
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMap { service ->
                service.getMembers(groupId, sort, offset, count, fields, filter)
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
    ): Single<Items<VKApiCommunity>> {
        return provideService(IGroupsService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .search(
                        query, type, filter, countryId, cityId, integerFromBoolean(future),
                        integerFromBoolean(market), sort, offset, count
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun leave(groupId: Int): Single<Boolean> {
        return provideService(IGroupsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.leave(groupId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun join(groupId: Int, notSure: Int?): Single<Boolean> {
        return provideService(IGroupsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.join(groupId, notSure)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun get(
        userId: Int?,
        extended: Boolean?,
        filter: String?,
        fields: String?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiCommunity>> {
        return provideService(IGroupsService::class.java, TokenType.USER)
            .flatMap { service ->
                service[userId, integerFromBoolean(extended), filter, fields, offset, count]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLongPollServer(groupId: Int): Single<GroupLongpollServer> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getLongPollServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(
        ids: Collection<Int>,
        domains: Collection<String>?,
        groupId: String?,
        fields: String?
    ): Single<List<VKApiCommunity>> {
        val pds: ArrayList<String> = ArrayList(1)
        join(ids, ",")?.let { pds.add(it) }
        join(domains, ",")?.let { pds.add(it) }
        return provideService(
            IGroupsService::class.java,
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMap { service ->
                service
                    .getById(join(pds, ","), groupId, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChats(groupId: Int, offset: Int?, count: Int?): Single<Items<VKApiGroupChats>> {
        return provideService(IGroupsService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getChats(groupId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        internal fun createFrom(info: GroupWallInfoResponse): VKApiCommunity {
            val community = info.groups?.get(0) ?: throw NotFoundException()
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
