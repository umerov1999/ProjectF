package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.VideoAlbumDboEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.buildVideoAlbumDbo
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapVideo
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildVideoAlbumFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildVideoFromDbo
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.model.VideoAlbumCriteria
import dev.ragnarok.fenrir.model.VideoCriteria
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class VideosInteractor(private val networker: INetworker, private val cache: IStorages) :
    IVideosInteractor {
    override fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int
    ): Flow<List<Video>> {
        return networker.vkDefault(accountId)
            .video()[ownerId, null, albumId, count, offset, true]
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val dbos: MutableList<VideoDboEntity> = ArrayList(dtos.size)
                val videos: MutableList<Video> = ArrayList(dbos.size)
                for (dto in dtos) {
                    dbos.add(mapVideo(dto))
                    videos.add(transform(dto))
                }
                cache.videos()
                    .insertData(accountId, ownerId, albumId, dbos, offset == 0)
                    .map {
                        videos
                    }
            }
    }

    override fun getCachedVideos(
        accountId: Long,
        ownerId: Long,
        albumId: Int
    ): Flow<List<Video>> {
        val criteria = VideoCriteria(accountId, ownerId, albumId)
        return cache.videos()
            .findByCriteria(criteria)
            .map { dbos ->
                val videos: MutableList<Video> = ArrayList(dbos.size)
                for (dbo in dbos) {
                    videos.add(buildVideoFromDbo(dbo))
                }
                videos
            }
    }

    override fun getById(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        cache: Boolean
    ): Flow<Video> {
        val ids: Collection<AccessIdPair> = listOf(AccessIdPair(videoId, ownerId, accessKey))
        return networker.vkDefault(accountId)
            .video()[null, ids, null, null, null, true]
            .map { items ->
                val tmp = items.items
                if (tmp.isNullOrEmpty()) {
                    throw NotFoundException()
                } else {
                    tmp[0]
                }
            }
            .flatMapConcat { dto ->
                if (cache) {
                    val dbo = mapVideo(dto)
                    this.cache.videos()
                        .insertData(accountId, ownerId, dto.album_id, listOf(dbo), false)
                        .map {
                            dto
                        }
                } else {
                    toFlow(dto)
                }
            }
            .map { transform(it) }
    }

    override fun addToMy(
        accountId: Long,
        targetOwnerId: Long,
        videoOwnerId: Long,
        videoId: Int
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .video()
            .addVideo(targetOwnerId, videoId, videoOwnerId)
            .ignoreElement()
    }

    override fun edit(
        accountId: Long,
        ownerId: Long,
        video_id: Int,
        name: String?,
        desc: String?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .video()
            .edit(ownerId, video_id, name, desc)
            .ignoreElement()
    }

    override fun delete(
        accountId: Long,
        videoId: Int?,
        ownerId: Long?,
        targetId: Long?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .video()
            .deleteVideo(videoId, ownerId, targetId)
            .ignoreElement()
    }

    override fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("video", ownerId, videoId, accessKey)
    }

    override fun isLiked(accountId: Long, ownerId: Long, videoId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .likes()
            .isLiked("video", ownerId, videoId)
    }

    override fun likeOrDislike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        like: Boolean
    ): Flow<Pair<Int, Boolean>> {
        return if (like) {
            networker.vkDefault(accountId)
                .likes()
                .add("video", ownerId, videoId, accessKey)
                .map { integer -> create(integer, true) }
        } else {
            networker.vkDefault(accountId)
                .likes()
                .delete("video", ownerId, videoId, accessKey)
                .map { integer -> create(integer, false) }
        }
    }

    override fun getCachedAlbums(accountId: Long, ownerId: Long): Flow<List<VideoAlbum>> {
        val criteria = VideoAlbumCriteria(accountId, ownerId)
        return cache.videoAlbums()
            .findByCriteria(criteria)
            .map { dbos ->
                val albums: MutableList<VideoAlbum> = ArrayList(dbos.size)
                for (dbo in dbos) {
                    albums.add(buildVideoAlbumFromDbo(dbo))
                }
                albums
            }
    }

    override fun getAlbumsByVideo(
        accountId: Long,
        target_id: Long,
        owner_id: Long,
        video_id: Int
    ): Flow<List<VideoAlbum>> {
        return networker.vkDefault(accountId)
            .video()
            .getAlbumsByVideo(target_id, owner_id, video_id)
            .map { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val albums: MutableList<VideoAlbum> = ArrayList(dtos.size)
                for (dto in dtos) {
                    val dbo = buildVideoAlbumDbo(dto)
                    albums.add(buildVideoAlbumFromDbo(dbo))
                }
                albums
            }
    }

    override fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<VideoAlbum>> {
        return networker.vkDefault(accountId)
            .video()
            .getAlbums(ownerId, offset, count, true)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val dbos: MutableList<VideoAlbumDboEntity> = ArrayList(dtos.size)
                val albums: MutableList<VideoAlbum> = ArrayList(dbos.size)
                for (dto in dtos) {
                    val dbo = buildVideoAlbumDbo(dto)
                    dbos.add(dbo)
                    albums.add(buildVideoAlbumFromDbo(dbo))
                }
                cache.videoAlbums()
                    .insertData(accountId, ownerId, dbos, offset == 0)
                    .map {
                        albums
                    }
            }
    }

    override fun search(
        accountId: Long,
        criteria: VideoSearchCriteria,
        count: Int,
        offset: Int
    ): Flow<List<Video>> {
        val sortOption = criteria.findOptionByKey<SpinnerOption>(VideoSearchCriteria.KEY_SORT)
        val sort = sortOption?.value?.id
        val hd = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_HD)
        val adult = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_ADULT)
        val filters = buildFiltersByCriteria(criteria)
        val searchOwn = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_SEARCH_OWN)
        val longer = criteria.extractNumberValueFromOption(VideoSearchCriteria.KEY_DURATION_FROM)
        val shoter = criteria.extractNumberValueFromOption(VideoSearchCriteria.KEY_DURATION_TO)
        return networker.vkDefault(accountId)
            .video()
            .search(
                criteria.query,
                sort,
                hd,
                adult,
                filters,
                searchOwn,
                offset,
                longer,
                shoter,
                count,
                false
            )
            .map { response ->
                val dtos = listEmptyIfNull(response.items)
                val videos: MutableList<Video> = ArrayList(dtos.size)
                for (dto in dtos) {
                    videos.add(transform(dto))
                }
                videos
            }
    }

    companion object {
        internal fun buildFiltersByCriteria(criteria: VideoSearchCriteria): String? {
            val youtube = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_YOUTUBE)
            val vimeo = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_VIMEO)
            val shortVideos = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_SHORT)
            val longVideos = criteria.extractBoleanValueFromOption(VideoSearchCriteria.KEY_LONG)
            val list = ArrayList<String>()
            if (youtube) {
                list.add("youtube")
            }
            if (vimeo) {
                list.add("vimeo")
            }
            if (shortVideos) {
                list.add("short")
            }
            if (longVideos) {
                list.add("long")
            }
            return if (list.isEmpty()) null else join(",", list)
        }
    }
}