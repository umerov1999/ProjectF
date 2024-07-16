package dev.ragnarok.fenrir.domain.impl

import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.PhotoPatch
import dev.ragnarok.fenrir.db.model.PhotoPatch.Like
import dev.ragnarok.fenrir.db.model.entity.PhotoAlbumDboEntity
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.buildPhotoAlbumDbo
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapPhoto
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildComment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.mapPhotoAlbum
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.options.SimpleDateOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.model.AccessIdPairModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoTags
import dev.ragnarok.fenrir.model.criteria.PhotoAlbumsCriteria
import dev.ragnarok.fenrir.model.criteria.PhotoCriteria
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class PhotosInteractor(private val networker: INetworker, private val cache: IStorages) :
    IPhotosInteractor {
    override fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int,
        rev: Boolean
    ): Flow<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()[ownerId, albumId.toString(), null, rev, offset, count]
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosRx(accountId, ownerId, albumId, dbos, offset == 0)
                    .map {
                        photos
                    }
            }
    }

    override fun getUsersPhoto(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()
            .getUsersPhoto(ownerId, extended, sort, offset, count)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosExtendedRx(accountId, ownerId, -9000, dbos, offset == 0)
                    .map {
                        photos
                    }
            }
    }

    override fun getAll(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAll(ownerId, extended, photo_sizes, offset, count)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMapConcat { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosExtendedRx(accountId, ownerId, -9001, dbos, offset == 0)
                    .map {
                        photos
                    }
            }
    }

    override fun search(
        accountId: Long,
        criteria: PhotoSearchCriteria,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>> {
        val sortOption = criteria.findOptionByKey<SpinnerOption>(PhotoSearchCriteria.KEY_SORT)
        val sort = sortOption?.value?.id
        val radius = criteria.extractNumberValueFromOption(PhotoSearchCriteria.KEY_RADIUS)
        val gpsOption = criteria.findOptionByKey<SimpleGPSOption>(PhotoSearchCriteria.KEY_GPS)
        val startDateOption =
            criteria.findOptionByKey<SimpleDateOption>(PhotoSearchCriteria.KEY_START_TIME)
        val endDateOption =
            criteria.findOptionByKey<SimpleDateOption>(PhotoSearchCriteria.KEY_END_TIME)
        return networker.vkDefault(accountId)
            .photos()
            .search(
                criteria.query,
                if ((gpsOption?.lat_gps ?: 0.0) < 0.1) null else gpsOption?.lat_gps,
                if ((gpsOption?.long_gps ?: 0.0) < 0.1) null else gpsOption?.long_gps,
                sort,
                radius,
                if (startDateOption?.timeUnix == 0L) null else startDateOption?.timeUnix,
                if (endDateOption?.timeUnix == 0L) null else endDateOption?.timeUnix,
                offset,
                count
            )
            .map { items -> listEmptyIfNull(items.items) }
            .map { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                }
                photos
            }
    }

    override fun getAllCachedData(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        sortInvert: Boolean
    ): Flow<List<Photo>> {
        val criteria = PhotoCriteria(accountId).setAlbumId(albumId).setOwnerId(ownerId)
            .setSortInvert(sortInvert)
        if (albumId == -9001 || albumId == -9000) {
            criteria.setOrderBy(BaseColumns._ID)
            return cache.photos()
                .findPhotosExtendedByCriteriaRx(criteria)
                .map { op ->
                    mapAll(
                        op
                    ) { map(it) }
                }
        }
        return cache.photos()
            .findPhotosByCriteriaRx(criteria)
            .map { op ->
                mapAll(
                    op
                ) { map(it) }
            }
    }

    override fun getAlbumById(accountId: Long, ownerId: Long, albumId: Int): Flow<PhotoAlbum> {
        return networker.vkDefault(accountId)
            .photos()
            .getAlbums(ownerId, listOf(albumId), null, null, needSystem = true, needCovers = true)
            .map { items -> listEmptyIfNull(items.items) }
            .map { dtos ->
                if (dtos.isEmpty()) {
                    throw NotFoundException()
                }
                var pos = -1
                for (i in dtos.indices) {
                    if (dtos[i].id == albumId) {
                        pos = i
                        break
                    }
                }
                if (pos == -1) {
                    throw NotFoundException()
                }
                transform(dtos[pos])
            }
    }

    override fun getCachedAlbums(accountId: Long, ownerId: Long): Flow<List<PhotoAlbum>> {
        val criteria = PhotoAlbumsCriteria(accountId, ownerId)
        return cache.photoAlbums()
            .findAlbumsByCriteria(criteria)
            .map { entities ->
                mapAll(
                    entities
                ) { mapPhotoAlbum(it) }
            }
    }

    override fun getTags(
        accountId: Long,
        ownerId: Long?,
        photo_id: Int?,
        access_key: String?
    ): Flow<List<PhotoTags>> {
        return networker.vkDefault(accountId)
            .photos().getTags(ownerId, photo_id, access_key).map { it1 ->
                mapAll(
                    it1
                ) { transform(it) }
            }
    }

    override fun getAllComments(
        accountId: Long,
        ownerId: Long,
        album_id: Int?,
        offset: Int,
        count: Int
    ): Flow<List<Comment>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAllComments(ownerId, album_id, 1, offset, count)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(items.items)
                val ownids = VKOwnIds()
                for (dto in dtos) {
                    ownids.append(dto)
                }
                owners
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ownids.all,
                        IOwnersRepository.MODE_ANY,
                        emptyList()
                    )
                    .map { bundle ->
                        val dbos: MutableList<Comment> = ArrayList(dtos.size)
                        for (i in dtos) {
                            val commented = Commented(i.pid, ownerId, CommentedType.PHOTO, null)
                            dbos.add(buildComment(commented, i, bundle))
                        }
                        dbos
                    }
            }
    }

    private fun checkPhotoAlbumExist(id: Int, list: List<VKApiPhotoAlbum>): Boolean {
        for (i in list) {
            if (i.id == id) {
                return true
            }
        }
        return false
    }

    override fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<PhotoAlbum>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAlbums(ownerId, null, offset, count, needSystem = true, needCovers = true)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(items.items)
                val dbos: MutableList<PhotoAlbumDboEntity> = ArrayList(dtos.size)
                val albums: MutableList<PhotoAlbum> = ArrayList(dbos.size)
                if (offset == 0) {
                    val Allph = VKApiPhotoAlbum()
                    Allph.title = "All photos"
                    Allph.id = -9001
                    Allph.owner_id = ownerId
                    Allph.size = -1
                    dbos.add(buildPhotoAlbumDbo(Allph))
                    albums.add(transform(Allph))
                    if (!checkPhotoAlbumExist(
                            -9000,
                            dtos
                        ) && accountId != ownerId && ownerId >= 0
                    ) {
                        val Usrph = VKApiPhotoAlbum()
                        Usrph.title = "With User photos"
                        Usrph.id = -9000
                        Usrph.owner_id = ownerId
                        Usrph.size = -1
                        dbos.add(buildPhotoAlbumDbo(Usrph))
                        albums.add(transform(Usrph))
                    }
                    if (!checkPhotoAlbumExist(-7, dtos)) {
                        val GrpPhotos = VKApiPhotoAlbum()
                        GrpPhotos.title = "Wall Photos"
                        GrpPhotos.id = -7
                        GrpPhotos.owner_id = ownerId
                        GrpPhotos.size = -1
                        dbos.add(buildPhotoAlbumDbo(GrpPhotos))
                        albums.add(transform(GrpPhotos))
                    }
                    if (Settings.get().main().localServer.enabled && accountId == ownerId) {
                        val Srvph = VKApiPhotoAlbum()
                        Srvph.title = "Local Server"
                        Srvph.id = -311
                        Srvph.owner_id = ownerId
                        Srvph.size = -1
                        dbos.add(buildPhotoAlbumDbo(Srvph))
                        albums.add(transform(Srvph))
                    }
                }
                for (dto in dtos) {
                    dbos.add(buildPhotoAlbumDbo(dto))
                    albums.add(transform(dto))
                }
                cache.photoAlbums()
                    .store(accountId, ownerId, dbos, offset == 0)
                    .map {
                        albums
                    }
            }
    }

    override fun isLiked(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .likes()
            .isLiked("photo", ownerId, photoId)
    }

    override fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        accessKey: String?
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("photo", ownerId, photoId, accessKey)
    }

    override fun like(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        add: Boolean,
        accessKey: String?
    ): Flow<Int> {
        val single = if (add) {
            networker.vkDefault(accountId)
                .likes()
                .add("photo", ownerId, photoId, accessKey)
        } else {
            networker.vkDefault(accountId)
                .likes()
                .delete("photo", ownerId, photoId, accessKey)
        }
        return single.flatMapConcat { count ->
            val patch = PhotoPatch().setLike(Like(count, add))
            cache.photos()
                .applyPatch(accountId, ownerId, photoId, patch)
                .map {
                    count
                }
        }
    }

    override fun copy(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        accessKey: String?
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .photos()
            .copy(ownerId, photoId, accessKey)
    }

    override fun removedAlbum(accountId: Long, ownerId: Long, albumId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .photos()
            .deleteAlbum(albumId, if (ownerId < 0) abs(ownerId) else null)
            .flatMapConcat {
                cache.photoAlbums()
                    .removeAlbumById(accountId, ownerId, albumId)
            }
    }

    override fun deletePhoto(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .photos()
            .delete(ownerId, photoId)
            .flatMapConcat {
                val patch = PhotoPatch().setDeletion(PhotoPatch.Deletion(true))
                cache.photos()
                    .applyPatch(accountId, ownerId, photoId, patch)
            }
    }

    override fun restorePhoto(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .photos()
            .restore(ownerId, photoId)
            .flatMapConcat {
                val patch = PhotoPatch().setDeletion(PhotoPatch.Deletion(false))
                cache.photos()
                    .applyPatch(accountId, ownerId, photoId, patch)
            }
    }

    override fun getPhotosByIds(
        accountId: Long,
        ids: Collection<AccessIdPairModel>
    ): Flow<List<Photo>> {
        val dtoPairs: MutableList<AccessIdPair> = ArrayList(ids.size)
        for (pair in ids) {
            dtoPairs.add(
                AccessIdPair(
                    pair.id,
                    pair.ownerId, pair.accessKey
                )
            )
        }
        return networker.vkDefault(accountId)
            .photos()
            .getById(dtoPairs)
            .map { dtos ->
                mapAll(
                    dtos
                ) { transform(it) }
            }
    }
}