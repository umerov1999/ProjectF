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
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class VideosInteractor(private val networker: INetworker, private val cache: IStorages) :
    IVideosInteractor {
    override fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int
    ): Single<List<Video>> {
        return networker.vkDefault(accountId)
            .video()[ownerId, null, albumId, count, offset, true]
            .flatMap { items ->
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
                    .andThen(Single.just<List<Video>>(videos))
            }
    }

    override fun getCachedVideos(
        accountId: Long,
        ownerId: Long,
        albumId: Int
    ): Single<List<Video>> {
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
    ): Single<Video> {
        val ids: Collection<AccessIdPair> = listOf(AccessIdPair(videoId, ownerId, accessKey))
        return networker.vkDefault(accountId)
            .video()[null, ids, null, null, null, true]
            .map { items ->
                items.items.nonNullNoEmpty {
                    return@map it[0]
                }
                throw NotFoundException()
            }
            .flatMap { dto ->
                if (cache) {
                    val dbo = mapVideo(dto)
                    return@flatMap this.cache.videos()
                        .insertData(accountId, ownerId, dto.album_id, listOf(dbo), false)
                        .andThen(Single.just(dto))
                }
                Single.just(dto)
            }
            .map { transform(it) }
    }

    override fun addToMy(
        accountId: Long,
        targetOwnerId: Long,
        videoOwnerId: Long,
        videoId: Int
    ): Completable {
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
    ): Completable {
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
    ): Completable {
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
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("video", ownerId, videoId, accessKey)
    }

    override fun isLiked(accountId: Long, ownerId: Long, videoId: Int): Single<Boolean> {
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
    ): Single<Pair<Int, Boolean>> {
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

    override fun getCachedAlbums(accountId: Long, ownerId: Long): Single<List<VideoAlbum>> {
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
    ): Single<List<VideoAlbum>> {
        return networker.vkDefault(accountId)
            .video()
            .getAlbumsByVideo(target_id, owner_id, video_id)
            .flatMap { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val albums: MutableList<VideoAlbum> = ArrayList(dtos.size)
                for (dto in dtos) {
                    val dbo = buildVideoAlbumDbo(dto)
                    albums.add(buildVideoAlbumFromDbo(dbo))
                }
                Single.just<List<VideoAlbum>>(albums)
            }
    }

    override fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<VideoAlbum>> {
        return networker.vkDefault(accountId)
            .video()
            .getAlbums(ownerId, offset, count, true)
            .flatMap { items ->
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
                    .andThen(Single.just<List<VideoAlbum>>(albums))
            }
    }

    override fun search(
        accountId: Long,
        criteria: VideoSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Video>> {
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