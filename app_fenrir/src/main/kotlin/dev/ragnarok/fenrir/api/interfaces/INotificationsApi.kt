package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.response.NotificationsResponse
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import io.reactivex.rxjava3.core.Single

interface INotificationsApi {
    @CheckResult
    fun markAsViewed(): Single<Int>

    @CheckResult
    operator fun get(
        count: Int?, startFrom: String?, filters: String?,
        startTime: Long?, endTime: Long?
    ): Single<NotificationsResponse>

    @CheckResult
    fun getOfficial(
        count: Int?,
        startFrom: Int?,
        filters: String?,
        startTime: Long?,
        endTime: Long?
    ): Single<FeedbackVKOfficialList>

    @CheckResult
    fun hide(query: String?): Single<Int>
}