package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.model.FeedList
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Single

interface IFeedInteractor {
    fun getCachedFeed(accountId: Int): Single<List<News>>
    fun getActualFeed(
        accountId: Int,
        count: Int,
        startFrom: String?,
        filters: String?,
        maxPhotos: Int?,
        sourceIds: String?
    ): Single<Pair<List<News>, String?>>

    fun search(
        accountId: Int,
        criteria: NewsFeedCriteria,
        count: Int,
        startFrom: String?
    ): Single<Pair<List<Post>, String?>>

    fun getCachedFeedLists(accountId: Int): Single<List<FeedList>>
    fun getActualFeedLists(accountId: Int): Single<List<FeedList>>
    fun saveList(accountId: Int, title: String?, listIds: Collection<Int>): Single<Int>
    fun addBan(accountId: Int, listIds: Collection<Int>): Single<Int>
    fun deleteList(accountId: Int, list_id: Int?): Single<Int>
    fun ignoreItem(accountId: Int, type: String?, owner_id: Int?, item_id: Int?): Single<Int>
    fun deleteBan(accountId: Int, listIds: Collection<Int>): Single<Int>
    fun getBanned(accountId: Int): Single<List<Owner>>
}