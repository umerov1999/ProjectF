package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPhotoAlbumsContentUriFor
import dev.ragnarok.fenrir.db.column.PhotoAlbumsColumns
import dev.ragnarok.fenrir.db.interfaces.IPhotoAlbumsStorage
import dev.ragnarok.fenrir.db.model.entity.PhotoAlbumDboEntity
import dev.ragnarok.fenrir.db.model.entity.PhotoSizeEntity
import dev.ragnarok.fenrir.db.model.entity.PrivacyEntity
import dev.ragnarok.fenrir.model.criteria.PhotoAlbumsCriteria
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class PhotoAlbumsStorage(base: AppStorages) : AbsStorage(base), IPhotoAlbumsStorage {
    override fun findAlbumById(
        accountId: Long,
        ownerId: Long,
        albumId: Int
    ): Single<Optional<PhotoAlbumDboEntity>> {
        return Single.create { e: SingleEmitter<Optional<PhotoAlbumDboEntity>> ->
            val where =
                PhotoAlbumsColumns.OWNER_ID + " = ? AND " + PhotoAlbumsColumns.ALBUM_ID + " = ?"
            val args = arrayOf(ownerId.toString(), albumId.toString())
            val uri = getPhotoAlbumsContentUriFor(accountId)
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            var album: PhotoAlbumDboEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    album = mapAlbum(cursor)
                }
                cursor.close()
            }
            e.onSuccess(wrap(album))
        }
    }

    override fun findAlbumsByCriteria(criteria: PhotoAlbumsCriteria): Single<List<PhotoAlbumDboEntity>> {
        return Single.create { e: SingleEmitter<List<PhotoAlbumDboEntity>> ->
            val uri = getPhotoAlbumsContentUriFor(criteria.accountId)
            val cursor = context.contentResolver.query(
                uri,
                null,
                PhotoAlbumsColumns.OWNER_ID + " = ?",
                arrayOf(criteria.ownerId.toString()),
                null
            )
            val data: MutableList<PhotoAlbumDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    data.add(mapAlbum(cursor))
                }
                cursor.close()
            }
            e.onSuccess(data)
        }
    }

    override fun store(
        accountId: Long,
        ownerId: Long,
        albums: List<PhotoAlbumDboEntity>,
        clearBefore: Boolean
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val operations =
                ArrayList<ContentProviderOperation>(if (clearBefore) albums.size + 1 else albums.size)
            val uri = getPhotoAlbumsContentUriFor(accountId)
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .withSelection(
                            PhotoAlbumsColumns.OWNER_ID + " = ?",
                            arrayOf(ownerId.toString())
                        )
                        .build()
                )
            }
            for (dbo in albums) {
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(createCv(dbo))
                        .build()
                )
            }
            context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            e.onComplete()
        }
    }

    override fun removeAlbumById(accountId: Long, ownerId: Long, albumId: Int): Completable {
        return Completable.create { e: CompletableEmitter ->
            val where =
                PhotoAlbumsColumns.OWNER_ID + " = ? AND " + PhotoAlbumsColumns.ALBUM_ID + " = ?"
            val args = arrayOf(ownerId.toString(), albumId.toString())
            val uri = getPhotoAlbumsContentUriFor(accountId)
            context.contentResolver.delete(uri, where, args)
            e.onComplete()
        }
    }

    private fun mapAlbum(cursor: Cursor): PhotoAlbumDboEntity {
        val id = cursor.getInt(PhotoAlbumsColumns.ALBUM_ID)
        val ownerId = cursor.getLong(PhotoAlbumsColumns.OWNER_ID)
        val album = PhotoAlbumDboEntity().set(id, ownerId)
            .setTitle(cursor.getString(PhotoAlbumsColumns.TITLE))
            .setSize(cursor.getInt(PhotoAlbumsColumns.SIZE))
            .setDescription(cursor.getString(PhotoAlbumsColumns.DESCRIPTION))
            .setCanUpload(cursor.getBoolean(PhotoAlbumsColumns.CAN_UPLOAD))
            .setUpdatedTime(cursor.getLong(PhotoAlbumsColumns.UPDATED))
            .setCreatedTime(cursor.getLong(PhotoAlbumsColumns.CREATED))
            .setUploadByAdminsOnly(cursor.getBoolean(PhotoAlbumsColumns.UPLOAD_BY_ADMINS))
            .setCommentsDisabled(cursor.getBoolean(PhotoAlbumsColumns.COMMENTS_DISABLED))
        val sizesJson = cursor.getBlob(PhotoAlbumsColumns.SIZES)
        if (sizesJson.nonNullNoEmpty()) {
            album.setSizes(MsgPack.decodeFromByteArrayEx(PhotoSizeEntity.serializer(), sizesJson))
        }
        val privacyViewText =
            cursor.getBlob(PhotoAlbumsColumns.PRIVACY_VIEW)
        if (privacyViewText.nonNullNoEmpty()) {
            album.setPrivacyView(
                MsgPack.decodeFromByteArrayEx(
                    PrivacyEntity.serializer(),
                    privacyViewText
                )
            )
        }
        val privacyCommentText =
            cursor.getBlob(PhotoAlbumsColumns.PRIVACY_COMMENT)
        if (privacyCommentText.nonNullNoEmpty()) {
            album.setPrivacyComment(
                MsgPack.decodeFromByteArrayEx(
                    PrivacyEntity.serializer(),
                    privacyCommentText
                )
            )
        }
        return album
    }

    companion object {
        internal fun createCv(dbo: PhotoAlbumDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(PhotoAlbumsColumns.ALBUM_ID, dbo.id)
            cv.put(PhotoAlbumsColumns.OWNER_ID, dbo.ownerId)
            cv.put(PhotoAlbumsColumns.TITLE, dbo.title)
            cv.put(PhotoAlbumsColumns.SIZE, dbo.size)
            dbo.privacyView.ifNonNull({
                cv.put(
                    PhotoAlbumsColumns.PRIVACY_VIEW,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    PhotoAlbumsColumns.PRIVACY_VIEW
                )
            })

            dbo.privacyComment.ifNonNull({
                cv.put(
                    PhotoAlbumsColumns.PRIVACY_COMMENT,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    PhotoAlbumsColumns.PRIVACY_COMMENT
                )
            })
            cv.put(PhotoAlbumsColumns.DESCRIPTION, dbo.description)
            cv.put(PhotoAlbumsColumns.CAN_UPLOAD, dbo.isCanUpload)
            cv.put(PhotoAlbumsColumns.UPDATED, dbo.updatedTime)
            cv.put(PhotoAlbumsColumns.CREATED, dbo.createdTime)
            dbo.sizes.ifNonNull({
                cv.put(
                    PhotoAlbumsColumns.SIZES,
                    MsgPack.encodeToByteArrayEx(PhotoSizeEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    PhotoAlbumsColumns.SIZES
                )
            })
            cv.put(PhotoAlbumsColumns.UPLOAD_BY_ADMINS, dbo.isUploadByAdminsOnly)
            cv.put(PhotoAlbumsColumns.COMMENTS_DISABLED, dbo.isCommentsDisabled)
            return cv
        }
    }
}