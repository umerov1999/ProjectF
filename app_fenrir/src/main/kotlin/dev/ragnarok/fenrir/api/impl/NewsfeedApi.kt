package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.INewsfeedApi
import dev.ragnarok.fenrir.api.model.response.IgnoreItemResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedBanResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedSearchResponse
import dev.ragnarok.fenrir.api.services.INewsfeedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlin.math.abs

internal class NewsfeedApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), INewsfeedApi {

    override fun addBan(listIds: Collection<Long>): Flow<Int> {
        val users: ArrayList<Long> = ArrayList()
        val groups: ArrayList<Long> = ArrayList()
        for (i in listIds) {
            if (i < 0) {
                groups.add(abs(i))
            } else {
                users.add(i)
            }
        }
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.addBan(join(users, ","), join(groups, ","))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun ignoreItem(
        type: String?,
        owner_id: Long?,
        item_id: Int?
    ): Flow<IgnoreItemResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.ignoreItem(type, owner_id, item_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?,
        extended: Boolean?,
        count: Int?,
        latitude: Double?,
        longitude: Double?,
        startTime: Long?,
        endTime: Long?,
        startFrom: String?,
        fields: String?
    ): Flow<NewsfeedSearchResponse> {
        return provideService(INewsfeedService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.search(
                    query, integerFromBoolean(extended), count, latitude, longitude,
                    startTime, endTime, startFrom, fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMentions(
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Flow<NewsfeedCommentsResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.getMentions(owner_id, count, offset, startTime, endTime)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        filters: String?, returnBanned: Boolean?, startTime: Long?,
        endTime: Long?, maxPhotoCount: Int?, sourceIds: String?,
        startFrom: String?, count: Int?, fields: String?
    ): Flow<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it[filters, integerFromBoolean(returnBanned), startTime, endTime, maxPhotoCount, sourceIds, startFrom, count, fields]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getTop(
        filters: String?, returnBanned: Boolean?, startTime: Long?,
        endTime: Long?, maxPhotoCount: Int?, sourceIds: String?,
        startFrom: String?, count: Int?, fields: String?
    ): Flow<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.getByType(
                    "top",
                    filters,
                    integerFromBoolean(returnBanned),
                    startTime,
                    endTime,
                    maxPhotoCount,
                    sourceIds,
                    startFrom,
                    count,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getFeedLikes(
        maxPhotoCount: Int?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Flow<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.getFeedLikes(maxPhotoCount, startFrom, count, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteBan(listIds: Collection<Long>): Flow<Int> {
        val users: ArrayList<Long> = ArrayList()
        val groups: ArrayList<Long> = ArrayList()
        for (i in listIds) {
            if (i < 0) {
                groups.add(abs(i))
            } else {
                users.add(i)
            }
        }
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.deleteBan(join(users, ","), join(groups, ","))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBanned(): Flow<NewsfeedBanResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMapConcat {
                it.getBanned(
                    1,
                    Fields.FIELDS_BASE_OWNER
                )
                    .map(extractResponseWithErrorHandling())
            }
    }
}