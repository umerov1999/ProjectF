package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.response.NotificationsResponse
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import kotlinx.coroutines.flow.Flow

interface INotificationsApi {
    @CheckResult
    fun markAsViewed(): Flow<Int>

    @CheckResult
    operator fun get(
        count: Int?, startFrom: String?, filters: String?,
        startTime: Long?, endTime: Long?
    ): Flow<NotificationsResponse>

    @CheckResult
    fun getOfficial(
        count: Int?,
        startFrom: Int?,
        filters: String?,
        startTime: Long?,
        endTime: Long?
    ): Flow<FeedbackVKOfficialList>

    @CheckResult
    fun hide(query: String?): Flow<Int>
}