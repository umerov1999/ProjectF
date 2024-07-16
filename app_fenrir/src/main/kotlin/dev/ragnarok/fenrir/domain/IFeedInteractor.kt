package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.response.IgnoreItemResponse
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.model.FeedList
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IFeedInteractor {
    fun getCachedFeed(accountId: Long): Flow<List<News>>
    fun getActualFeed(
        accountId: Long,
        count: Int,
        startFrom: String?,
        filters: String?,
        maxPhotos: Int?,
        sourceIds: String?
    ): Flow<Pair<List<News>, String?>>

    fun search(
        accountId: Long,
        criteria: NewsFeedCriteria,
        count: Int,
        startFrom: String?
    ): Flow<Pair<List<Post>, String?>>

    fun getCachedFeedLists(accountId: Long): Flow<List<FeedList>>
    fun getActualFeedLists(accountId: Long): Flow<List<FeedList>>
    fun saveList(accountId: Long, title: String?, listIds: Collection<Long>): Flow<Int>
    fun addBan(accountId: Long, listIds: Collection<Long>): Flow<Int>
    fun deleteList(accountId: Long, list_id: Int?): Flow<Int>
    fun ignoreItem(
        accountId: Long,
        type: String?,
        owner_id: Long?,
        item_id: Int?
    ): Flow<IgnoreItemResponse>

    fun deleteBan(accountId: Long, listIds: Collection<Long>): Flow<Int>
    fun getBanned(accountId: Long): Flow<List<Owner>>
}