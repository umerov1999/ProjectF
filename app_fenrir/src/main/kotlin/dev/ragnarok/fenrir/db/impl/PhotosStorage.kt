package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPhotosContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPhotosExtendedContentUriFor
import dev.ragnarok.fenrir.db.column.PhotosColumns
import dev.ragnarok.fenrir.db.column.PhotosExtendedColumns
import dev.ragnarok.fenrir.db.interfaces.IPhotosStorage
import dev.ragnarok.fenrir.db.model.PhotoPatch
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.db.model.entity.PhotoSizeEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.criteria.PhotoCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class PhotosStorage(base: AppStorages) : AbsStorage(base), IPhotosStorage {
    override fun insertPhotosRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Completable {
        return Completable.fromAction {
            val operations =
                ArrayList<ContentProviderOperation>(if (clearBefore) photos.size + 1 else photos.size)
            val uri = getPhotosContentUriFor(accountId)
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .withSelection(
                            PhotosColumns.OWNER_ID + " = ? AND " + PhotosColumns.ALBUM_ID + " = ?",
                            arrayOf(ownerId.toString(), albumId.toString())
                        )
                        .build()
                )
            }
            for (dbo in photos) {
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(getCV(dbo))
                        .build()
                )
            }
            context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
        }
    }

    override fun insertPhotosExtendedRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Completable {
        return Completable.fromAction {
            val operations =
                ArrayList<ContentProviderOperation>(if (clearBefore) photos.size + 1 else photos.size)
            val uri = getPhotosExtendedContentUriFor(accountId)
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .withSelection(
                            PhotosExtendedColumns.DB_OWNER_ID + " = ? AND " + PhotosExtendedColumns.DB_ALBUM_ID + " = ?",
                            arrayOf(ownerId.toString(), albumId.toString())
                        )
                        .build()
                )
            }
            for (dbo in photos) {
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(getExtendedCV(dbo, ownerId, albumId))
                        .build()
                )
            }
            context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
        }
    }

    override fun findPhotosExtendedByCriteriaRx(criteria: PhotoCriteria): Single<List<PhotoDboEntity>> {
        return Single.create { e: SingleEmitter<List<PhotoDboEntity>> ->
            val selection = getSelectionExtendedForCriteria(criteria)
            val orderBy =
                if (criteria.orderBy == null) PhotosExtendedColumns.DATE + (if (!criteria.sortInvert) " DESC" else " ASC") else criteria.orderBy
            val uri = getPhotosExtendedContentUriFor(criteria.accountId)
            val cursor = context.contentResolver.query(uri, null, selection, null, orderBy)
            val photos = ArrayList<PhotoDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    photos.add(mapPhotoDbo(cursor))
                }
                cursor.close()
            }
            e.onSuccess(photos)
        }
    }

    override fun findPhotosByCriteriaRx(criteria: PhotoCriteria): Single<List<PhotoDboEntity>> {
        return Single.create { e: SingleEmitter<List<PhotoDboEntity>> ->
            val selection = getSelectionForCriteria(criteria)
            val orderBy =
                if (criteria.orderBy == null) PhotosColumns.PHOTO_ID + (if (!criteria.sortInvert) " DESC" else " ASC") else criteria.orderBy
            val uri = getPhotosContentUriFor(criteria.accountId)
            val cursor = context.contentResolver.query(uri, null, selection, null, orderBy)
            val photos = ArrayList<PhotoDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    photos.add(mapPhotoDbo(cursor))
                }
                cursor.close()
            }
            e.onSuccess(photos)
        }
    }

    override fun applyPatch(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        patch: PhotoPatch
    ): Completable {
        return Completable.fromAction {
            val cv = ContentValues()
            patch.like.requireNonNull {
                cv.put(PhotosColumns.LIKES, it.count)
                cv.put(PhotosColumns.USER_LIKES, it.isLiked)
            }
            patch.deletion.requireNonNull {
                cv.put(PhotosColumns.DELETED, it.isDeleted)
            }
            if (cv.size() > 0) {
                val uri = getPhotosContentUriFor(accountId)
                val where = PhotosColumns.PHOTO_ID + " = ? AND " + PhotosColumns.OWNER_ID + " = ?"
                val args = arrayOf(photoId.toString(), ownerId.toString())
                contentResolver.update(uri, cv, where, args)
            }
        }
    }

    private fun mapPhotoDbo(cursor: Cursor): PhotoDboEntity {
        var sizes: PhotoSizeEntity? = null
        val sizesJson = cursor.getBlob(PhotosColumns.SIZES)
        if (sizesJson.nonNullNoEmpty()) {
            sizes = MsgPack.decodeFromByteArrayEx(PhotoSizeEntity.serializer(), sizesJson)
        }
        val id = cursor.getInt(PhotosColumns.PHOTO_ID)
        val ownerId = cursor.getLong(PhotosColumns.OWNER_ID)
        return PhotoDboEntity().set(id, ownerId)
            .setSizes(sizes)
            .setAlbumId(cursor.getInt(PhotosColumns.ALBUM_ID))
            .setWidth(cursor.getInt(PhotosColumns.WIDTH))
            .setHeight(cursor.getInt(PhotosColumns.HEIGHT))
            .setText(cursor.getString(PhotosColumns.TEXT))
            .setDate(cursor.getLong(PhotosColumns.DATE))
            .setUserLikes(cursor.getBoolean(PhotosColumns.USER_LIKES))
            .setCanComment(cursor.getBoolean(PhotosColumns.CAN_COMMENT))
            .setLikesCount(cursor.getInt(PhotosColumns.LIKES))
            .setCommentsCount(cursor.getInt(PhotosColumns.COMMENTS))
            .setTagsCount(cursor.getInt(PhotosColumns.TAGS))
            .setAccessKey(cursor.getString(PhotosColumns.ACCESS_KEY))
            .setDeleted(cursor.getBoolean(PhotosColumns.DELETED))
    }

    companion object {
        internal fun getCV(dbo: PhotoDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(PhotosColumns.PHOTO_ID, dbo.id)
            cv.put(PhotosColumns.ALBUM_ID, dbo.albumId)
            cv.put(PhotosColumns.OWNER_ID, dbo.ownerId)
            cv.put(PhotosColumns.WIDTH, dbo.width)
            cv.put(PhotosColumns.HEIGHT, dbo.height)
            cv.put(PhotosColumns.TEXT, dbo.text)
            cv.put(PhotosColumns.DATE, dbo.date)
            dbo.sizes.ifNonNull({
                cv.put(
                    PhotosColumns.SIZES,
                    MsgPack.encodeToByteArrayEx(PhotoSizeEntity.serializer(), it)
                )
            }, {
                cv.putNull(PhotosColumns.SIZES)
            })
            cv.put(PhotosColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(PhotosColumns.CAN_COMMENT, dbo.isCanComment)
            cv.put(PhotosColumns.LIKES, dbo.likesCount)
            cv.put(PhotosColumns.REPOSTS, dbo.repostsCount)
            cv.put(PhotosColumns.COMMENTS, dbo.commentsCount)
            cv.put(PhotosColumns.TAGS, dbo.tagsCount)
            cv.put(PhotosColumns.ACCESS_KEY, dbo.accessKey)
            cv.put(PhotosColumns.DELETED, dbo.isDeleted)
            return cv
        }

        internal fun getExtendedCV(
            dbo: PhotoDboEntity,
            ownerId: Long,
            albumInt: Int
        ): ContentValues {
            val cv = ContentValues()
            cv.put(PhotosExtendedColumns.DB_ALBUM_ID, albumInt)
            cv.put(PhotosExtendedColumns.DB_OWNER_ID, ownerId)
            cv.put(PhotosExtendedColumns.PHOTO_ID, dbo.id)
            cv.put(PhotosExtendedColumns.ALBUM_ID, dbo.albumId)
            cv.put(PhotosExtendedColumns.OWNER_ID, dbo.ownerId)
            cv.put(PhotosExtendedColumns.WIDTH, dbo.width)
            cv.put(PhotosExtendedColumns.HEIGHT, dbo.height)
            cv.put(PhotosExtendedColumns.TEXT, dbo.text)
            cv.put(PhotosExtendedColumns.DATE, dbo.date)
            dbo.sizes.ifNonNull({
                cv.put(
                    PhotosExtendedColumns.SIZES,
                    MsgPack.encodeToByteArrayEx(PhotoSizeEntity.serializer(), it)
                )
            }, {
                cv.putNull(PhotosExtendedColumns.SIZES)
            })
            cv.put(PhotosExtendedColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(PhotosExtendedColumns.CAN_COMMENT, dbo.isCanComment)
            cv.put(PhotosExtendedColumns.LIKES, dbo.likesCount)
            cv.put(PhotosExtendedColumns.REPOSTS, dbo.repostsCount)
            cv.put(PhotosExtendedColumns.COMMENTS, dbo.commentsCount)
            cv.put(PhotosExtendedColumns.TAGS, dbo.tagsCount)
            cv.put(PhotosExtendedColumns.ACCESS_KEY, dbo.accessKey)
            cv.put(PhotosExtendedColumns.DELETED, dbo.isDeleted)
            return cv
        }

        internal fun getSelectionForCriteria(criteria: PhotoCriteria): String {
            var selection = "1 = 1"
            selection = selection + " AND " + PhotosColumns.OWNER_ID + " = " + criteria.ownerId
            selection = selection + " AND " + PhotosColumns.ALBUM_ID + " = " + criteria.albumId
            val range = criteria.range
            if (range != null) {
                selection = selection + " AND " + BaseColumns._ID + " >= " + range.first +
                        " AND " + BaseColumns._ID + " <= " + criteria.range?.last.orZero()
            }
            return selection
        }

        internal fun getSelectionExtendedForCriteria(criteria: PhotoCriteria): String {
            var selection = "1 = 1"
            selection =
                selection + " AND " + PhotosExtendedColumns.DB_OWNER_ID + " = " + criteria.ownerId
            selection =
                selection + " AND " + PhotosExtendedColumns.DB_ALBUM_ID + " = " + criteria.albumId
            val range = criteria.range
            if (range != null) {
                selection = selection + " AND " + BaseColumns._ID + " >= " + range.first +
                        " AND " + BaseColumns._ID + " <= " + criteria.range?.last.orZero()
            }
            return selection
        }
    }
}