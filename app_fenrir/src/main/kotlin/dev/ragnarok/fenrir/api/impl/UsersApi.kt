package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IUsersApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiGift
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.UserWallInfoResponse
import dev.ragnarok.fenrir.api.services.IUsersService
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Single

internal class UsersApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IUsersApi {
    override fun getUserWallInfo(
        userId: Long,
        fields: String?,
        nameCase: String?
    ): Single<VKApiUser> {
        return provideService(IUsersService(), TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getUserWallInfo(
                        "var user_id = Args.user_id;\n" +
                                "var fields =Args.fields;\n" +
                                "var name_case = Args.name_case;\n" +
                                "\n" +
                                "var user_info = API.users.get({\"v\":\"" + Constants.API_VERSION + "\",\"user_ids\":user_id,\n" +
                                "    \"fields\":fields, \"name_case\":name_case});\n" +
                                "\n" +
                                "var all_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"all\"}).count;\n" +
                                "    \n" +
                                "var owner_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"owner\"}).count;\n" +
                                "\n" +
                                "var postponed_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"postponed\"}).count;\n" +
                                "\n" +
                                "return {\"user_info\": user_info, \n" +
                                "    \"all_wall_count\":all_wall_count,\n" +
                                "    \"owner_wall_count\":owner_wall_count,\n" +
                                "    \"postponed_wall_count\":postponed_wall_count\n" +
                                "};", userId, fields, nameCase
                    )
                    .map(extractResponseWithErrorHandling())
                    .map { response ->
                        if (safeCountOf(response.users) != 1) {
                            throw NotFoundException()
                        }
                        createFrom(response)
                    }
            }
    }

    override fun getFollowers(
        userId: Long?,
        offset: Int?,
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService(), TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service.getFollowers(userId, offset, count, fields, nameCase)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getRequests(
        offset: Int?,
        count: Int?,
        extended: Int?,
        out: Int?,
        fields: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService(), TokenType.USER)
            .flatMap { service ->
                service.getRequests(offset, count, extended, out, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?,
        sort: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        city: Int?,
        country: Int?,
        hometown: String?,
        universityCountry: Int?,
        university: Int?,
        universityYear: Int?,
        universityFaculty: Int?,
        universityChair: Int?,
        sex: Int?,
        status: Int?,
        ageFrom: Int?,
        ageTo: Int?,
        birthDay: Int?,
        birthMonth: Int?,
        birthYear: Int?,
        online: Boolean?,
        hasPhoto: Boolean?,
        schoolCountry: Int?,
        schoolCity: Int?,
        schoolClass: Int?,
        school: Int?,
        schoolYear: Int?,
        religion: String?,
        interests: String?,
        company: String?,
        position: String?,
        groupId: Long?,
        fromList: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService(), TokenType.USER)
            .flatMap { service ->
                service
                    .search(
                        query,
                        sort,
                        offset,
                        count,
                        fields,
                        city,
                        country,
                        hometown,
                        universityCountry,
                        university,
                        universityYear,
                        universityFaculty,
                        universityChair,
                        sex,
                        status,
                        ageFrom,
                        ageTo,
                        birthDay,
                        birthMonth,
                        birthYear,
                        integerFromBoolean(online),
                        integerFromBoolean(hasPhoto),
                        schoolCountry,
                        schoolCity,
                        schoolClass,
                        school,
                        schoolYear,
                        religion,
                        interests,
                        company,
                        position,
                        groupId,
                        fromList
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun report(userId: Long?, type: String?, comment: String?): Single<Int> {
        return provideService(IUsersService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .report(userId, type, comment)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun checkAndAddFriend(userId: Long?): Single<Int> {
        return provideService(IUsersService(), TokenType.USER)
            .flatMap { service ->
                service
                    .checkAndAddFriend(
                        "var user_id = Args.user_id; if(API.users.get({\"v\":\"" + Constants.API_VERSION + "\", \"user_ids\": user_id, \"fields\": \"friend_status\"})[0].friend_status == 0) {return API.friends.add({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\": user_id});} return 0;",
                        userId
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getGifts(user_id: Long?, count: Int?, offset: Int?): Single<Items<VKApiGift>> {
        return provideService(IUsersService(), TokenType.USER)
            .flatMap { service ->
                service.getGifts(user_id, count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        userIds: Collection<Long>?,
        domains: Collection<String>?,
        fields: String?,
        nameCase: String?
    ): Single<List<VKApiUser>> {
        val ids = ArrayList<String?>(1)
        if (userIds != null) {
            ids.add(join(userIds, ","))
        }
        if (domains != null) {
            ids.add(join(domains, ","))
        }
        return provideService(
            IUsersService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMap { service ->
                service[join(ids, ","), fields, nameCase]
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        internal fun createFrom(response: UserWallInfoResponse): VKApiUser {
            val user = response.users?.get(0) ?: throw NotFoundException()
            if (user.counters == null) {
                user.counters = VKApiUser.Counters()
            }
            response.allWallCount.requireNonNull {
                user.counters?.all_wall = it
            }
            response.ownerWallCount.requireNonNull {
                user.counters?.owner_wall = it
            }
            response.postponedWallCount.requireNonNull {
                user.counters?.postponed_wall = it
            }
            return user
        }
    }
}