package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.VKApiNews
import dev.ragnarok.fenrir.api.model.response.IgnoreItemResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.Dto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.PhotoDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.PostDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.TopicDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.VideoDto
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.NewsDboEntity
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapNews
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildComment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildNews
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformPosts
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildNewsFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.fragment.search.options.SimpleDateOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.FeedOwners
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.PhotoWithOwner
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.TopicWithOwner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.VideoWithOwner
import dev.ragnarok.fenrir.model.criteria.FeedCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.nonNullNoEmptyOrNullable
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import java.util.Locale

class FeedInteractor(
    private val networker: INetworker,
    private val stores: IStorages,
    private val mainSettings: ISettings.IMainSettings,
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
        accountId: Long,
        count: Int,
        startFrom: String?,
        filters: String?,
        maxPhotos: Int?,
        sourceIds: String?
    ): Flow<Pair<List<News>, String?>> {
        return when (sourceIds) {
            "likes", "top" -> {
                when (sourceIds) {
                    "likes" -> networker.vkDefault(accountId)
                        .newsfeed()
                        .getFeedLikes(maxPhotos, startFrom, count, Fields.FIELDS_BASE_OWNER)

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
                            Fields.FIELDS_BASE_OWNER
                        )
                }.flatMapConcat { response ->
                    val nextFrom = response.nextFrom
                    val owners = transformOwners(response.profiles, response.groups)
                    val feed = listEmptyIfNull(response.items)
                    val dbos: MutableList<NewsDboEntity> = ArrayList(feed.size)
                    val ownIds = VKOwnIds()
                    for (news in feed) {
                        if (news.source_id == 0L) {
                            continue
                        }
                        dbos.add(mapNews(news))
                        ownIds.appendNews(news)
                    }
                    val ownerEntities = mapOwners(response.profiles, response.groups)
                    stores.feed()
                        .store(accountId, dbos, ownerEntities, startFrom.isNullOrEmpty())
                        .flatMapConcat {
                            mainSettings.storeFeedNextFrom(accountId, nextFrom)
                            mainSettings.setFeedSourceIds(accountId, sourceIds)
                            ownersRepository.findBaseOwnersDataAsBundle(
                                accountId,
                                ownIds.all,
                                IOwnersRepository.MODE_ANY,
                                owners
                            )
                                .map {
                                    val news: MutableList<News> = ArrayList(feed.size)
                                    for (dto in feed) {
                                        if (dto.source_id == 0L) {
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
                ) null else sourceIds, startFrom, count, Fields.FIELDS_BASE_OWNER]
                    .flatMapConcat { response ->
                        val blockAds = mainSettings.isAd_block_story_news
                        val needStripRepost = mainSettings.isStrip_news_repost
                        val rgx = mainSettings.isBlock_news_by_words
                        val nextFrom = response.nextFrom
                        val owners = transformOwners(response.profiles, response.groups)
                        val feed = listEmptyIfNull(response.items)
                        val dbos: MutableList<NewsDboEntity> = ArrayList(feed.size)
                        val ownIds = VKOwnIds()
                        for (dto in feed) {
                            if (dto.source_id == 0L || blockAds && (dto.type == "ads" || dto.mark_as_ads != 0) || needStripRepost && dto.isOnlyRepost || containInWords(
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
                            .flatMapConcat {
                                mainSettings.storeFeedNextFrom(accountId, nextFrom)
                                mainSettings.setFeedSourceIds(accountId, sourceIds)
                                ownersRepository.findBaseOwnersDataAsBundle(
                                    accountId,
                                    ownIds.all,
                                    IOwnersRepository.MODE_ANY,
                                    owners
                                )
                                    .map {
                                        val news: MutableList<News> = ArrayList(feed.size)
                                        for (dto in feed) {
                                            if (dto.source_id == 0L || blockAds && (dto.type == "ads" || dto.mark_as_ads != 0) || needStripRepost && dto.isOnlyRepost || containInWords(
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
        accountId: Long,
        criteria: NewsFeedCriteria,
        count: Int,
        startFrom: String?
    ): Flow<Pair<List<Post>, String?>> {
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
                if ((gpsOption?.lat_gps.orZero()) < 0.1) null else gpsOption?.lat_gps,
                if ((gpsOption?.long_gps.orZero()) < 0.1) null else gpsOption?.long_gps,
                if (startDateOption?.timeUnix == 0L) null else startDateOption?.timeUnix,
                if (endDateOption?.timeUnix == 0L) null else endDateOption?.timeUnix,
                startFrom,
                Fields.FIELDS_BASE_OWNER
            )
            .flatMapConcat { response ->
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

    override fun addBan(accountId: Long, listIds: Collection<Long>): Flow<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().addBan(listIds)
    }

    override fun deleteBan(accountId: Long, listIds: Collection<Long>): Flow<Int> {
        return networker.vkDefault(accountId)
            .newsfeed().deleteBan(listIds)
    }

    override fun getBanned(accountId: Long): Flow<List<Owner>> {
        return networker.vkDefault(accountId)
            .newsfeed().getBanned().map { response ->
                transformOwners(response.profiles, response.groups)
            }
    }

    override fun ignoreItem(
        accountId: Long,
        type: String?,
        owner_id: Long?,
        item_id: Int?
    ): Flow<IgnoreItemResponse> {
        return networker.vkDefault(accountId)
            .newsfeed().ignoreItem(type, owner_id, item_id)
    }

    override fun getCachedFeed(accountId: Long): Flow<List<News>> {
        val criteria = FeedCriteria(accountId)
        return stores.feed()
            .findByCriteria(criteria)
            .flatMapConcat { dbos ->
                val ownIds = VKOwnIds()
                for (dbo in dbos) {
                    fillOwnerIds(ownIds, dbo)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map {
                        val news: MutableList<News> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            news.add(buildNewsFromDbo(dbo, it))
                        }
                        news
                    }
            }
    }

    override fun getMentions(
        accountId: Long,
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Flow<Pair<List<NewsfeedComment>, String?>> {
        return networker.vkDefault(accountId)
            .newsfeed()
            .getMentions(owner_id, count, offset, startTime, endTime)
            .flatMapConcat { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val ownIds = VKOwnIds()
                val dtos = listEmptyIfNull(response.items)
                for (dto in dtos) {
                    if (dto is PostDto) {
                        val post = dto.post ?: continue
                        ownIds.append(post)
                        ownIds.append(post.comments)
                    }
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { bundle ->
                        val comments: MutableList<NewsfeedComment> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            val comment = createFrom(dto, bundle)
                            if (comment != null) {
                                comments.add(comment)
                            }
                        }
                        create(comments, response.nextFrom)
                    }
            }
    }

    override fun getFeedListById(
        accountId: Long,
        dbId: Long,
        refresh: Boolean
    ): Flow<FeedOwners?> {
        return stores.tempStore().getFeedOwnersById(dbId).map { entity ->
            if (entity == null) {
                null
            } else {
                val ret = FeedOwners(entity.id).setTitle(entity.title)
                val ids = entity.ownersIds
                val listOwners = ArrayList<Owner>(ids?.size.orZero())
                if (ids != null) {
                    for (id in ids) {
                        if (!isActive()) {
                            break
                        }
                        val owner = ownersRepository.getBaseOwnerInfo(
                            accountId,
                            id,
                            if (refresh) IOwnersRepository.MODE_NET else IOwnersRepository.MODE_ANY
                        ).catch { emit(if (id > 0) User(id) else Community(-id)) }.single()
                        listOwners.add(owner)
                    }
                }
                ret.setOwners(listOwners)
            }
        }
    }

    companion object {
        private fun oneMentionFrom(
            commented: Commented,
            dto: CommentsDto?,
            bundle: IOwnersBundle
        ): Comment? {
            return dto?.list.nonNullNoEmptyOrNullable {
                buildComment(commented, it[it.size - 1], bundle)
            }
        }

        internal fun createFrom(dto: Dto, bundle: IOwnersBundle): NewsfeedComment? {
            if (dto is PhotoDto) {
                val photoDto = dto.photo
                val photo = transform(photoDto ?: return null)
                val commented = Commented.from(photo)
                val photoOwner = bundle.getById(photo.ownerId)
                return NewsfeedComment(PhotoWithOwner(photo, photoOwner))
                    .setComment(oneMentionFrom(commented, photoDto.comments, bundle))
            }
            if (dto is VideoDto) {
                val videoDto = dto.video
                val video = transform(videoDto ?: return null)
                val commented = Commented.from(video)
                val videoOwner = bundle.getById(video.ownerId)
                return NewsfeedComment(VideoWithOwner(video, videoOwner))
                    .setComment(oneMentionFrom(commented, videoDto.comments, bundle))
            }
            if (dto is PostDto) {
                val postDto = dto.post
                val post = transform(postDto ?: return null, bundle)
                val commented = Commented.from(post)
                return NewsfeedComment(post).setComment(
                    oneMentionFrom(
                        commented,
                        postDto.comments,
                        bundle
                    )
                )
            }
            if (dto is TopicDto) {
                val topicDto = dto.topic
                val topic = transform(topicDto ?: return null, bundle)
                topicDto.comments.requireNonNull {
                    topic.setCommentsCount(it.count)
                }
                val commented = Commented.from(topic)
                val owner = bundle.getById(topic.ownerId)
                return NewsfeedComment(TopicWithOwner(topic, owner)).setComment(
                    oneMentionFrom(
                        commented,
                        topicDto.comments,
                        bundle
                    )
                )
            }
            return null
        }
    }
}