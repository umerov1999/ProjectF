package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IFeedbackInteractor {
    fun getCachedFeedbacks(accountId: Long): Flow<List<Feedback>>
    fun getActualFeedbacks(
        accountId: Long,
        count: Int,
        startFrom: String?
    ): Flow<Pair<List<Feedback>, String?>>

    fun getActualFeedbacksOfficial(
        accountId: Long,
        count: Int?,
        startFrom: Int?
    ): Flow<FeedbackVKOfficialList>

    fun getCachedFeedbacksOfficial(accountId: Long): Flow<FeedbackVKOfficialList>
    fun markAsViewed(accountId: Long): Flow<Boolean>
    fun hide(accountId: Long, query: String?): Flow<Boolean>
}