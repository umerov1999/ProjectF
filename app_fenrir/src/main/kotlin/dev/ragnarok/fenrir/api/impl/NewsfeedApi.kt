package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.INewsfeedApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFeedList
import dev.ragnarok.fenrir.api.model.response.NewsfeedBanResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedSearchResponse
import dev.ragnarok.fenrir.api.services.INewsfeedService
import io.reactivex.rxjava3.core.Single
import kotlin.math.abs

internal class NewsfeedApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), INewsfeedApi {
    override fun getLists(listIds: Collection<Int>?): Single<Items<VKApiFeedList>> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.getLists(join(listIds, ","), 1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun saveList(title: String?, listIds: Collection<Int>?): Single<Int> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.saveList(title, join(listIds, ","))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun addBan(listIds: Collection<Int>): Single<Int> {
        val users: ArrayList<Int> = ArrayList()
        val groups: ArrayList<Int> = ArrayList()
        for (i in listIds) {
            if (i < 0) {
                groups.add(abs(i))
            } else {
                users.add(i)
            }
        }
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.addBan(join(users, ","), join(groups, ","))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteList(list_id: Int?): Single<Int> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.deleteList(list_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun ignoreItem(type: String?, owner_id: Int?, item_id: Int?): Single<Int> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.ignoreItem(type, owner_id, item_id)
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
    ): Single<NewsfeedSearchResponse> {
        return provideService(INewsfeedService(), TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .search(
                        query, integerFromBoolean(extended), count, latitude, longitude,
                        startTime, endTime, startFrom, fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getComments(
        count: Int?,
        filters: String?,
        reposts: String?,
        startTime: Long?,
        endTime: Long?,
        lastCommentsCount: Int?,
        startFrom: String?,
        fields: String?
    ): Single<NewsfeedCommentsResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getComments(
                        count,
                        filters,
                        reposts,
                        startTime,
                        endTime,
                        lastCommentsCount,
                        startFrom,
                        fields,
                        null
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMentions(
        owner_id: Int?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Single<NewsfeedCommentsResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getMentions(owner_id, count, offset, startTime, endTime)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        filters: String?, returnBanned: Boolean?, startTime: Long?,
        endTime: Long?, maxPhotoCount: Int?, sourceIds: String?,
        startFrom: String?, count: Int?, fields: String?
    ): Single<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service[filters, integerFromBoolean(returnBanned), startTime, endTime, maxPhotoCount, sourceIds, startFrom, count, fields]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getTop(
        filters: String?, returnBanned: Boolean?, startTime: Long?,
        endTime: Long?, maxPhotoCount: Int?, sourceIds: String?,
        startFrom: String?, count: Int?, fields: String?
    ): Single<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.getByType(
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

    override fun getRecommended(
        startTime: Long?, endTime: Long?,
        maxPhotoCount: Int?, startFrom: String?, count: Int?, fields: String?
    ): Single<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getRecommended(
                        startTime, endTime,
                        maxPhotoCount, startFrom, count, fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getFeedLikes(
        maxPhotoCount: Int?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Single<NewsfeedResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getFeedLikes(maxPhotoCount, startFrom, count, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteBan(listIds: Collection<Int>): Single<Int> {
        val users: ArrayList<Int> = ArrayList()
        val groups: ArrayList<Int> = ArrayList()
        for (i in listIds) {
            if (i < 0) {
                groups.add(abs(i))
            } else {
                users.add(i)
            }
        }
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service.deleteBan(join(users, ","), join(groups, ","))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBanned(): Single<NewsfeedBanResponse> {
        return provideService(INewsfeedService(), TokenType.USER)
            .flatMap { service ->
                service
                    .getBanned(
                        1,
                        Constants.MAIN_OWNER_FIELDS
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }
}