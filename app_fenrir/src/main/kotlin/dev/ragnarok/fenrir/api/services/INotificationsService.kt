package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.NotificationsResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import kotlinx.coroutines.flow.Flow

class INotificationsService : IServiceRest() {
    val markAsViewed: Flow<BaseResponse<Int>>
        get() = rest.request("notifications.markAsViewed", null, baseInt)

    operator fun get(
        count: Int?,
        startFrom: String?,
        filters: String?,
        startTime: Long?,
        endTime: Long?
    ): Flow<BaseResponse<NotificationsResponse>> {
        return rest.request(
            "notifications.get", form(
                "count" to count,
                "start_from" to startFrom,
                "filters" to filters,
                "start_time" to startTime,
                "end_time" to endTime
            ), base(NotificationsResponse.serializer())
        )
    }

    fun getOfficial(
        count: Int?,
        startFrom: Int?,
        filters: String?,
        startTime: Long?,
        endTime: Long?,
        fields: String?
    ): Flow<BaseResponse<FeedbackVKOfficialList>> {
        return rest.request(
            "notifications.get", form(
                "count" to count,
                "start_from" to startFrom,
                "filters" to filters,
                "start_time" to startTime,
                "end_time" to endTime,
                "fields" to fields
            ), base(FeedbackVKOfficialList.serializer())
        )
    }

    fun hide(query: String?): Flow<BaseResponse<Int>> {
        return rest.request("notifications.hide", form("query" to query), baseInt)
    }
}