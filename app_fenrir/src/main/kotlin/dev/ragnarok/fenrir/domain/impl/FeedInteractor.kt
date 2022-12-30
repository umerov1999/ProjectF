package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiNews
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.FeedListEntity
import dev.ragnarok.fenrir.db.model.entity.NewsDboEntity
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapNews
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildNews
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformPosts
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildNewsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.fragment.search.options.SimpleDateOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.FeedCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.IOtherSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Single
import java.util.*

class FeedInteractor(
    private val networker: INetworker,
    private val stores: IStorages,
    private val otherSettings: IOtherSettings,
    private val ownersRepository: IOwnersRepository
) : IFeedInteractor {
    private fun containInWords(rgx: Set<String>?, dto: VKApiNews): Boolean {
        if (rgx.isNullOrEmpty()) {
            return false
        }
        dto.text.nonNullNoEmpty {
            for (i in rgx) {
                if (it.lowercase(Locale.getDefault())
                        .contains(i.lowercase(Locale.getDefault()))
                ) {
                    return true
                }
            }
        }
        dto.copy_history.nonNullNoEmpty {
            for (i in it) {
                i.text.nonNullNoEmpty { pit ->
                    for (s in rgx) {
                        if (pit.lowercase(Locale.getDefault())
                                .contains(s.lowercase(Locale.getDefault()))
                        ) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    override fun getActualFeed(
        accountId: Int,
        count: Int,
        startFrom: String?,
        filters: String?,
        maxPhotos: Int?,
        sourceIds: String?
    ): Single<Pair<List<News>, String?>> {
        return when (sourceIds) {
            "likes", "recommendation", "top" -> {
                when (sourceIds) {
                    "likes" -> networker.vkDefault(accountId)
                        .newsfeed()
                        .getFeedLikes(maxPhotos, startFrom, count, Constants.MAIN_OWNER_FIELDS)
                    "recommendation" -> networker.vkDefault(accountId)
                        .newsfeed()
                        .getRecommended(
                            null,
                            null,
                            maxPhotos,
                            startFrom,
                            count,
                            Constants.MAIN_OWNER_FIELDS
                        )
                    else -> networker.vkDefault(accountId)
                        .newsfeed()
                        .getTop(
                            null,
                            null,
                            null,
                            null,
                            maxPhotos,
                            null,
                            startFrom,
                            count,
                            Constants.MAIN_OWNER_FIELDS
                        )
                }.flatMap { response ->
                    val nextFrom = response.nextFrom
                    val owners = transformOwners(response.profiles, response.groups)
                    val feed = listEmptyIfNull(response.items)
                    val dbos: MutableList<NewsDboEntity> = ArrayList(feed.size)
                    val ownIds = VKOwnIds()
                    for (news in feed) {
                        if (news.source_id == 0) {
                            continue
                        }
                        dbos.add(mapNews(news))
                        ownIds.appendNews(news)
                    }
                    val ownerEntities = mapOwners(response.profiles, response.groups)
                    stores.feed()
                        .store(accountId, dbos, ownerEntities, startFrom.isNullOrEmpty())
                        .flatMap {
                            otherSettings.storeFeedNextFrom(accountId, nextFrom)
                            otherSettings.setFeedSourceIds(accountId, sourceIds)
                            ownersRepository.findBaseOwnersDataAsBundle(
                                accountId,
                                ownIds.all,
                                IOwnersRepository.MODE_ANY,
                                owners
                            )
                                .map {
                                    val news: MutableList<News> = ArrayList(feed.size)
                                    for (dto in feed) {
                                        if (dto.source_id == 0) {
                                            continue
                                        }
                                        news.add(buildNews(dto, it))
                                    }
                                    create(news, nextFrom)
                                }
                        }
                }
            }
            else -> {
                networker.vkDefault(accountId)
                    .newsfeed()[filters, null, null, null, maxPhotos, if (setOf(
                        "updates_photos",
                        "updates_videos",
                        "updates_full",
                        "updates_audios"
                    ).contains(sourceIds)
                ) null else sourceIds, startFrom, count, Constants.MAIN_OWNER_FIELDS]
                    .flatMap { response ->
                        val blockAds = Settings.get().other().isAd_block_story_news
                        val needStripRepost = Settings.get().other().isStrip_news_repost
                        val rgx = Settings.get().other().isBlock_news_by_words
                        val nextFrom = response.nextFrom
                        val owners = transformOwners(response.profiles, response.groups)
                        val feed = listEmptyIfNull(response.items)
                        val dbos: MutableList<NewsDboEntity> = ArrayList(feed.size)
                        val ownIds = VKOwnIds()
                        for (dto in feed) {
                            if (dto.source_id == 0 || blockAds && (dto.type == "ads" || dto.mark_as_ads != 0) || needStripRepost && dto.isOnlyRepost || containInWords(
                                    rgx,
                                    dto
                                )
                            ) {
                                continue
                            }
                            dbos.add(mapNews(dto))
                            ownIds.appendNews(dto)
                        }
                        val ownerEntities = mapOwners(response.profiles, response.groups)
                        stores.feed()
                            .store(accountId, dbos, ownerEntities, startFrom.isNullOrEmpty())
                            .flatMap {
                                otherSettings.storeFeedNextFrom(accountId, nextFrom)
                                otherSettings.setFeedSourceIds(accountId, sourceIds)
                                ownersRepository.findBaseOwnersDataAsBundle(
                                    accountId,
                                    ownIds.all,
                                    IOwnersRepository.MODE_ANY,
                                    owners
                                )
                                    .map {
                                        val news: MutableList<News> = ArrayList(feed.size)
                                        for (dto in feed) {
                                            if (dto.source_id == 0 || blockAds && (dto.type == "ads" || dto.mark_as_ads != 0) || needStripRepost && dto.isOnlyRepost || containInWords(
                                                    rgx,
                                                    dto
                                                )
                                            ) {
                                                continue
                                            } else if (needStripRepost && dto.hasCopyHistory()) {
                                                dto.stripRepost()
                                            }
                                            news.add(buildNews(dto, it))
                                        }
                                        create(news, nextFrom)
                                    }
                            }
                    }
            }
        }
    }

    override fun search(
        accountId: Int,
        criteria: NewsFeedCriteria,
        count: Int,
        startFrom: String?
    ): Single<Pair<List<Post>, String?>> {
        val gpsOption = criteria.findOptionByKey<SimpleGPSOption>(NewsFeedCriteria.KEY_GPS)
        val startDateOption =
            criteria.findOptionByKey<SimpleDateOption>(NewsFeedCriteria.KEY_START_TIME)
        val endDateOption =
            criteria.findOptionByKey<SimpleDateOption>(NewsFeedCriteria.KEY_END_TIME)
        return networker.vkDefault(accountId)
            .newsfeed()
            .search(
                criteria.query,
                true,
                count,
                if ((gpsOption?.lat_gps ?: 0.0) < 0.1) null else gpsOption?.lat_gps,
                if ((gpsOption?.long_gps ?: 0.0) < 0.1) null else gpsOption?.long_gps,
                if (startDateOption?.timeUnix == 0L) null else startDateOption?.timeUnix,
                if (endDateOption?.timeUnix == 0L) null else endDateOption?.timeUnix,
                startFrom,
                Constants.MAIN_OWNER_FIELDS
            )
            .flatMap { response ->
                val dtos = listEmptyIfNull(response.items)
                val owners = transformOwners(response.profiles, response.groups)
                val ownIds = VKOwnIds()
                for (post in dtos) {
                    ownIds.append(post)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map {
                        val posts = transformPosts(dtos, it)
                        create(posts, response.nextFrom)
                    }
            }
    }

    override fun saveList(accountId: Int, title: String?, listIds: Collection<Int>): Single<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().saveList(title, listIds)
    }

    override fun addBan(accountId: Int, listIds: Collection<Int>): Single<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().addBan(listIds)
    }

    override fun deleteBan(accountId: Int, listIds: Collection<Int>): Single<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().deleteBan(listIds)
    }

    override fun getBanned(accountId: Int): Single<List<Owner>> {
        return networker.vkDefault(accountId)
            .newsfeed().getBanned().map { response ->
                transformOwners(response.profiles, response.groups)
            }
    }

    override fun deleteList(accountId: Int, list_id: Int?): Single<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().deleteList(list_id)
    }

    override fun ignoreItem(
        accountId: Int,
        type: String?,
        owner_id: Int?,
        item_id: Int?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().ignoreItem(type, owner_id, item_id)
    }

    override fun getCachedFeedLists(accountId: Int): Single<List<FeedList>> {
        val criteria = FeedSourceCriteria(accountId)
        return stores.feed()
            .getAllLists(criteria)
            .map { entities ->
                val lists: MutableList<FeedList> = ArrayList(entities.size)
                for (entity in entities) {
                    lists.add(createFeedListFromEntity(entity))
                }
                lists
            }
    }

    override fun getActualFeedLists(accountId: Int): Single<List<FeedList>> {
        return networker.vkDefault(accountId)
            .newsfeed()
            .getLists(null)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .flatMap { dtos ->
                val entities: MutableList<FeedListEntity> = ArrayList(dtos.size)
                val lists: MutableList<FeedList> = ArrayList()
                for (dto in dtos) {
                    val entity = FeedListEntity(dto.id)
                        .setTitle(dto.title)
                        .setNoReposts(dto.no_reposts)
                        .setSourceIds(dto.source_ids)
                    entities.add(entity)
                    lists.add(createFeedListFromEntity(entity))
                }
                stores.feed()
                    .storeLists(accountId, entities)
                    .andThen(Single.just<List<FeedList>>(lists))
            }
    }

    override fun getCachedFeed(accountId: Int): Single<List<News>> {
        val criteria = FeedCriteria(accountId)
        return stores.feed()
            .findByCriteria(criteria)
            .flatMap { dbos ->
                val ownIds = VKOwnIds()
                for (dbo in dbos) {
                    fillOwnerIds(ownIds, dbo)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map<List<News>> {
                        val news: MutableList<News> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            news.add(buildNewsFromDbo(dbo, it))
                        }
                        news
                    }
            }
    }

    companion object {
        internal fun createFeedListFromEntity(entity: FeedListEntity): FeedList {
            return FeedList(entity.id, entity.title)
        }
    }
}