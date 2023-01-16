package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFeedbackInteractor {
    fun getCachedFeedbacks(accountId: Long): Single<List<Feedback>>
    fun getActualFeedbacks(
        accountId: Long,
        count: Int,
        startFrom: String?
    ): Single<Pair<List<Feedback>, String?>>

    fun getActualFeedbacksOfficial(
        accountId: Long,
        count: Int?,
        startFrom: Int?
    ): Single<FeedbackVKOfficialList>

    fun getCachedFeedbacksOfficial(accountId: Long): Single<FeedbackVKOfficialList>
    fun markAsViewed(accountId: Long): Single<Boolean>
    fun hide(accountId: Long, query: String?): Completable
}