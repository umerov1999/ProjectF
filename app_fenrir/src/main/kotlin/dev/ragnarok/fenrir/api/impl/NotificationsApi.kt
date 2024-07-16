package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields.FIELDS_BASE_OWNER
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.INotificationsApi
import dev.ragnarok.fenrir.api.model.feedback.VKApiBaseFeedback
import dev.ragnarok.fenrir.api.model.response.NotificationsResponse
import dev.ragnarok.fenrir.api.services.INotificationsService
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class NotificationsApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), INotificationsApi {
    override fun markAsViewed(): Flow<Int> {
        return provideService(INotificationsService(), TokenType.USER)
            .flatMapConcat {
                it.markAsViewed
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        count: Int?,
        startFrom: String?,
        filters: String?,
        startTime: Long?,
        endTime: Long?
    ): Flow<NotificationsResponse> {
        return provideService(INotificationsService(), TokenType.USER)
            .flatMapConcat { s ->
                s[count, startFrom, filters, startTime, endTime]
                    .map(extractResponseWithErrorHandling())
                    .map { response ->
                        val realList: MutableList<VKApiBaseFeedback> =
                            ArrayList(safeCountOf(response.notifications))
                        response.notifications.requireNonNull {
                            for (n in it) {
                                n.reply.requireNonNull { r ->
                                    // fix В ответе нет этого параметра
                                    r.from_id = accountId
                                }
                                realList.add(n)
                            }
                        }
                        response.notifications = realList //without unsupported items
                        response
                    }
            }
    }

    override fun getOfficial(
        count: Int?,
        startFrom: Int?,
        filters: String?,
        startTime: Long?,
        endTime: Long?
    ): Flow<FeedbackVKOfficialList> {
        return provideService(INotificationsService(), TokenType.USER)
            .flatMapConcat {
                it.getOfficial(
                    count,
                    startFrom,
                    filters,
                    startTime,
                    endTime,
                    FIELDS_BASE_OWNER
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun hide(query: String?): Flow<Int> {
        return provideService(INotificationsService(), TokenType.USER)
            .flatMapConcat {
                it.hide(query)
                    .map(extractResponseWithErrorHandling())
            }
    }
}