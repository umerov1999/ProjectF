package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IFriendsApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFriendList
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.DeleteFriendResponse
import dev.ragnarok.fenrir.api.model.response.OnlineFriendsResponse
import dev.ragnarok.fenrir.api.services.IFriendsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class FriendsApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IFriendsApi {
    override fun getOnline(
        userId: Long,
        order: String?,
        count: Int,
        offset: Int,
        fields: String?
    ): Flow<OnlineFriendsResponse> {
        val targetOrder = if (order == null) null else toQuotes(order)
        val targetFields = if (fields == null) null else toQuotes(fields)
        val code = "var user_id = %s;\n" +
                "var count = %s;\n" +
                "var offset = %s;\n" +
                "var fields = %s;\n" +
                "\n" +
                "var uids = API.friends.getOnline({\"v\":\"" + Constants.API_VERSION + "\",\n" +
                "    \"user_id\":user_id, \n" +
                "    \"count\":count, \n" +
                "    \"offset\":offset,\n" +
                "    \"order\":%s\n" +
                "});\n" +
                "\n" +
                "var profiles = API.users.get({\"v\":\"" + Constants.API_VERSION + "\",\"user_ids\":uids, \"fields\":fields});\n" +
                "\n" +
                "return {\"uids\":uids, \"profiles\":profiles};"
        val formattedCode = String.format(code, userId, count, offset, targetFields, targetOrder)
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.getOnline(formattedCode)
                    .map(extractResponseWithErrorHandling())
            }
    }

    /*@Override
    public Single<FriendsWithCountersResponse> getWithCounters(int userId, String order, int count,
                                                               int offset, String fields) {
        String targetOrder = order == null ? null : toQuotes(order);
        String targetFields = fields == null ? null : toQuotes(fields);

        String code = "var friends = API.friends.get({" +
                "\"user_id\":" + userId + ", " +
                "\"fields\":" + targetFields + ", " +
                "\"order\":" + targetOrder + ", " +
                "\"count\":" + count + ", " +
                "\"offset\":" + offset + "}); " +

                "var counters = API.users.get({\"user_ids\":" + userId + ", \"fields\":\"counters\"})[0].counters; " +

                "return {\"friends\":friends, \"counters\":counters};";

        return provideService(IFriendsService.class)
                .flatMap(service -> service
                        .getWithMyCounters(code)
                        .map(extractResponseWithErrorHandling()));
    }*/
    override fun get(
        userId: Long?, order: String?, listId: Int?, count: Int?,
        offset: Int?, fields: String?, nameCase: String?
    ): Flow<Items<VKApiUser>> {
        return provideService(IFriendsService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it[userId, order, listId, count, offset, fields, nameCase]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getByPhones(phones: String?, fields: String?): Flow<List<VKApiUser>> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.getByPhones(phones, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getRecommendations(
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Flow<Items<VKApiUser>> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.getRecommendations(count, fields, nameCase)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteSubscriber(
        subscriber_id: Long
    ): Flow<Int> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.deleteSubscriber(subscriber_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLists(userId: Long?, returnSystem: Boolean?): Flow<Items<VKApiFriendList>> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.getLists(userId, integerFromBoolean(returnSystem))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete(userId: Long): Flow<DeleteFriendResponse> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.delete(userId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun add(userId: Long, text: String?, follow: Boolean?): Flow<Int> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.add(userId, text, integerFromBoolean(follow))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        userId: Long,
        query: String?,
        fields: String?,
        nameCase: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiUser>> {
        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat {
                it.search(userId, query, fields, nameCase, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMutual(
        sourceUid: Long?,
        targetUid: Long,
        count: Int,
        offset: Int,
        fields: String?
    ): Flow<List<VKApiUser>> {
        val code = "var source_uid = %s;\n" +
                "var target_uid = %s;\n" +
                "var count = %s;\n" +
                "var offset = %s;\n" +
                "var fields = %s;\n" +
                "\n" +
                "var uids = API.friends.getMutual({\"v\":\"" + Constants.API_VERSION + "\",\n" +
                "    \"source_uid\":source_uid, \n" +
                "    \"target_uid\":target_uid, \n" +
                "    \"count\":count, \n" +
                "    \"offset\":offset\n" +
                "});\n" +
                "\n" +
                "var profiles = API.users.get({\"v\":\"" + Constants.API_VERSION + "\",\"user_ids\":uids, \"fields\":fields});\n" +
                "\n" +
                "return {\"uids\":uids, \"profiles\":profiles};"
        val formattedCode =
            String.format(code, sourceUid, targetUid, count, offset, toQuotes(fields))

        return provideService(IFriendsService(), TokenType.USER)
            .flatMapConcat { s ->
                s.getMutual(formattedCode)
                    .map(extractResponseWithErrorHandling())
                    .map { it.profiles.orEmpty() }
            }
    }
}