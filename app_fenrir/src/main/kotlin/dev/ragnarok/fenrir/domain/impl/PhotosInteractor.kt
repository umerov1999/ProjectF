package dev.ragnarok.fenrir.domain.impl

import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.api.model.VKApiPhotoTags
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
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.PhotoAlbumsCriteria
import dev.ragnarok.fenrir.model.criteria.PhotoCriteria
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlin.math.abs

class PhotosInteractor(private val networker: INetworker, private val cache: IStorages) :
    IPhotosInteractor {
    override fun get(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        count: Int,
        offset: Int,
        rev: Boolean
    ): Single<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()[ownerId, albumId.toString(), null, rev, offset, count]
            .map { items -> listEmptyIfNull(items.items) }
            .flatMap { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosRx(accountId, ownerId, albumId, dbos, offset == 0)
                    .andThen(Single.just<List<Photo>>(photos))
            }
    }

    override fun getUsersPhoto(
        accountId: Int,
        ownerId: Int,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()
            .getUsersPhoto(ownerId, extended, sort, offset, count)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMap { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosExtendedRx(accountId, ownerId, -9000, dbos, offset == 0)
                    .andThen(Single.just<List<Photo>>(photos))
            }
    }

    override fun getAll(
        accountId: Int,
        ownerId: Int,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAll(ownerId, extended, photo_sizes, offset, count)
            .map { items -> listEmptyIfNull(items.items) }
            .flatMap { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                    dbos.add(mapPhoto(dto))
                }
                cache.photos()
                    .insertPhotosExtendedRx(accountId, ownerId, -9001, dbos, offset == 0)
                    .andThen(Single.just<List<Photo>>(photos))
            }
    }

    override fun search(
        accountId: Int,
        criteria: PhotoSearchCriteria,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>> {
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
            .flatMap { dtos ->
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                for (dto in dtos) {
                    photos.add(transform(dto))
                }
                Single.just<List<Photo>>(photos)
            }
    }

    override fun getAllCachedData(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        sortInvert: Boolean
    ): Single<List<Photo>> {
        val criteria = PhotoCriteria(accountId).setAlbumId(albumId).setOwnerId(ownerId)
            .setSortInvert(sortInvert)
        if (albumId == -15 || albumId == -9001) {
            criteria.setOrderBy(BaseColumns._ID)
        }
        if (albumId == -9001 || albumId == -9000) {
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

    override fun getAlbumById(accountId: Int, ownerId: Int, albumId: Int): Single<PhotoAlbum> {
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

    override fun getCachedAlbums(accountId: Int, ownerId: Int): Single<List<PhotoAlbum>> {
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
        accountId: Int,
        ownerId: Int?,
        photo_id: Int?,
        access_key: String?
    ): Single<List<VKApiPhotoTags>> {
        return networker.vkDefault(accountId)
            .photos().getTags(ownerId, photo_id, access_key)
            .map { items -> items }
    }

    override fun getAllComments(
        accountId: Int,
        ownerId: Int,
        album_id: Int?,
        offset: Int,
        count: Int
    ): Single<List<Comment>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAllComments(ownerId, album_id, 1, offset, count)
            .flatMap { items ->
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
                    .map<List<Comment>> { bundle: IOwnersBundle ->
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
        accountId: Int,
        ownerId: Int,
        count: Int,
        offset: Int
    ): Single<List<PhotoAlbum>> {
        return networker.vkDefault(accountId)
            .photos()
            .getAlbums(ownerId, null, offset, count, needSystem = true, needCovers = true)
            .flatMap { items ->
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
                    if (Settings.get().other().localServer.enabled && accountId == ownerId) {
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
                    .andThen(Single.just<List<PhotoAlbum>>(albums))
            }
    }

    override fun isLiked(accountId: Int, ownerId: Int, photoId: Int): Single<Boolean> {
        return networker.vkDefault(accountId)
            .likes()
            .isLiked("photo", ownerId, photoId)
    }

    override fun checkAndAddLike(
        accountId: Int,
        ownerId: Int,
        photoId: Int,
        accessKey: String?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .likes().checkAndAddLike("photo", ownerId, photoId, accessKey)
    }

    override fun like(
        accountId: Int,
        ownerId: Int,
        photoId: Int,
        add: Boolean,
        accessKey: String?
    ): Single<Int> {
        val single: Single<Int> = if (add) {
            networker.vkDefault(accountId)
                .likes()
                .add("photo", ownerId, photoId, accessKey)
        } else {
            networker.vkDefault(accountId)
                .likes()
                .delete("photo", ownerId, photoId, accessKey)
        }
        return single.flatMap { count ->
            val patch = PhotoPatch().setLike(Like(count, add))
            cache.photos()
                .applyPatch(accountId, ownerId, photoId, patch)
                .andThen(Single.just(count))
        }
    }

    override fun copy(accountId: Int, ownerId: Int, photoId: Int, accessKey: String?): Single<Int> {
        return networker.vkDefault(accountId)
            .photos()
            .copy(ownerId, photoId, accessKey)
    }

    override fun removedAlbum(accountId: Int, ownerId: Int, albumId: Int): Completable {
        return networker.vkDefault(accountId)
            .photos()
            .deleteAlbum(albumId, if (ownerId < 0) abs(ownerId) else null)
            .flatMapCompletable {
                cache.photoAlbums()
                    .removeAlbumById(accountId, ownerId, albumId)
            }
    }

    override fun deletePhoto(accountId: Int, ownerId: Int, photoId: Int): Completable {
        return networker.vkDefault(accountId)
            .photos()
            .delete(ownerId, photoId)
            .flatMapCompletable {
                val patch = PhotoPatch().setDeletion(PhotoPatch.Deletion(true))
                cache.photos()
                    .applyPatch(accountId, ownerId, photoId, patch)
            }
    }

    override fun restorePhoto(accountId: Int, ownerId: Int, photoId: Int): Completable {
        return networker.vkDefault(accountId)
            .photos()
            .restore(ownerId, photoId)
            .flatMapCompletable {
                val patch = PhotoPatch().setDeletion(PhotoPatch.Deletion(false))
                cache.photos()
                    .applyPatch(accountId, ownerId, photoId, patch)
            }
    }

    override fun getPhotosByIds(
        accountId: Int,
        ids: Collection<AccessIdPair>
    ): Single<List<Photo>> {
        val dtoPairs: MutableList<dev.ragnarok.fenrir.api.model.AccessIdPair> = ArrayList(ids.size)
        for (pair in ids) {
            dtoPairs.add(
                dev.ragnarok.fenrir.api.model.AccessIdPair(
                    pair.getId(),
                    pair.getOwnerId(), pair.getAccessKey()
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