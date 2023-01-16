package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.model.FeedList
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Single

interface IFeedInteractor {
    fun getCachedFeed(accountId: Long): Single<List<News>>
    fun getActualFeed(
        accountId: Long,
        count: Int,
        startFrom: String?,
        filters: String?,
        maxPhotos: Int?,
        sourceIds: String?
    ): Single<Pair<List<News>, String?>>

    fun search(
        accountId: Long,
        criteria: NewsFeedCriteria,
        count: Int,
        startFrom: String?
    ): Single<Pair<List<Post>, String?>>

    fun getCachedFeedLists(accountId: Long): Single<List<FeedList>>
    fun getActualFeedLists(accountId: Long): Single<List<FeedList>>
    fun saveList(accountId: Long, title: String?, listIds: Collection<Long>): Single<Int>
    fun addBan(accountId: Long, listIds: Collection<Long>): Single<Int>
    fun deleteList(accountId: Long, list_id: Int?): Single<Int>
    fun ignoreItem(accountId: Long, type: String?, owner_id: Long?, item_id: Int?): Single<Int>
    fun deleteBan(accountId: Long, listIds: Collection<Long>): Single<Int>
    fun getBanned(accountId: Long): Single<List<Owner>>
}