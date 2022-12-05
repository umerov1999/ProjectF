package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFeedbackInteractor {
    fun getCachedFeedbacks(accountId: Int): Single<List<Feedback>>
    fun getActualFeedbacks(
        accountId: Int,
        count: Int,
        startFrom: String?
    ): Single<Pair<List<Feedback>, String?>>

    fun getActualFeedbacksOfficial(
        accountId: Int,
        count: Int?,
        startFrom: Int?
    ): Single<FeedbackVKOfficialList>

    fun getCachedFeedbacksOfficial(accountId: Int): Single<FeedbackVKOfficialList>
    fun markAsViewed(accountId: Int): Single<Boolean>
    fun hide(accountId: Int, query: String?): Completable
}