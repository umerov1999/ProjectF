package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getDocsContentUriFor
import dev.ragnarok.fenrir.db.column.DocsColumns
import dev.ragnarok.fenrir.db.interfaces.IDocsStorage
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.GraffitiDbo
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.VideoPreviewDbo
import dev.ragnarok.fenrir.db.model.entity.PhotoSizeEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.criteria.DocsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class DocsStorage(base: AppStorages) : AbsStorage(base), IDocsStorage {
    override fun get(criteria: DocsCriteria): Single<List<DocumentDboEntity>> {
        return Single.create { e: SingleEmitter<List<DocumentDboEntity>> ->
            val start = System.currentTimeMillis()
            val uri = getDocsContentUriFor(criteria.accountId)
            val where: String
            val args: Array<String>
            val filter = criteria.filter
            if (filter != null && filter != DocFilter.Type.ALL) {
                where = DocsColumns.OWNER_ID + " = ? AND " + DocsColumns.TYPE + " = ?"
                args = arrayOf(criteria.ownerId.toString(), filter.toString())
            } else {
                where = DocsColumns.OWNER_ID + " = ?"
                args = arrayOf(criteria.ownerId.toString())
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val data: MutableList<DocumentDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    data.add(map(cursor))
                }
                cursor.close()
            }
            e.onSuccess(data)
            log("DocsStorage.get", start, "count: " + data.size)
        }
    }

    override fun store(
        accountId: Long,
        ownerId: Long,
        entities: List<DocumentDboEntity>,
        clearBeforeInsert: Boolean
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val start = System.currentTimeMillis()
            val uri = getDocsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeInsert) {
                operations.add(
                    ContentProviderOperation.newDelete(uri)
                        .withSelection(DocsColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString()))
                        .build()
                )
            }
            for (entity in entities) {
                val cv = ContentValues()
                cv.put(DocsColumns.DOC_ID, entity.id)
                cv.put(DocsColumns.OWNER_ID, entity.ownerId)
                cv.put(DocsColumns.TITLE, entity.title)
                cv.put(DocsColumns.SIZE, entity.size)
                cv.put(DocsColumns.EXT, entity.ext)
                cv.put(DocsColumns.URL, entity.url)
                cv.put(DocsColumns.DATE, entity.date)
                cv.put(DocsColumns.TYPE, entity.type)
                cv.put(DocsColumns.ACCESS_KEY, entity.accessKey)
                entity.photo.ifNonNull({
                    cv.put(
                        DocsColumns.PHOTO,
                        MsgPack.encodeToByteArrayEx(PhotoSizeEntity.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocsColumns.PHOTO
                    )
                })
                entity.graffiti.ifNonNull({
                    cv.put(
                        DocsColumns.GRAFFITI,
                        MsgPack.encodeToByteArrayEx(GraffitiDbo.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocsColumns.GRAFFITI
                    )
                })
                entity.video.ifNonNull({
                    cv.put(
                        DocsColumns.VIDEO,
                        MsgPack.encodeToByteArrayEx(VideoPreviewDbo.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocsColumns.VIDEO
                    )
                })
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            e.onComplete()
            log("DocsStorage.store", start, "count: " + entities.size)
        }
    }

    override fun delete(accountId: Long, docId: Int, ownerId: Long): Completable {
        return Completable.fromAction {
            val uri = getDocsContentUriFor(accountId)
            val where = DocsColumns.DOC_ID + " = ? AND " + DocsColumns.OWNER_ID + " = ?"
            val args = arrayOf(docId.toString(), ownerId.toString())
            contentResolver.delete(uri, where, args)
        }
    }

    companion object {
        internal fun map(cursor: Cursor): DocumentDboEntity {
            val id = cursor.getInt(DocsColumns.DOC_ID)
            val ownerId = cursor.getLong(DocsColumns.OWNER_ID)
            val document = DocumentDboEntity().set(id, ownerId)
                .setTitle(cursor.getString(DocsColumns.TITLE))
                .setSize(cursor.getLong(DocsColumns.SIZE))
                .setExt(cursor.getString(DocsColumns.EXT))
                .setUrl(cursor.getString(DocsColumns.URL))
                .setType(cursor.getInt(DocsColumns.TYPE))
                .setDate(cursor.getLong(DocsColumns.DATE))
                .setAccessKey(cursor.getString(DocsColumns.ACCESS_KEY))
            val photoJson = cursor.getBlob(DocsColumns.PHOTO)
            val graffitiJson = cursor.getBlob(DocsColumns.GRAFFITI)
            val videoJson = cursor.getBlob(DocsColumns.VIDEO)
            if (photoJson.nonNullNoEmpty()) {
                document.setPhoto(
                    MsgPack.decodeFromByteArrayEx(
                        PhotoSizeEntity.serializer(),
                        photoJson
                    )
                )
            }
            if (graffitiJson.nonNullNoEmpty()) {
                document.setGraffiti(
                    MsgPack.decodeFromByteArrayEx(
                        GraffitiDbo.serializer(),
                        graffitiJson
                    )
                )
            }
            if (videoJson.nonNullNoEmpty()) {
                document.setVideo(
                    MsgPack.decodeFromByteArrayEx(
                        VideoPreviewDbo.serializer(),
                        videoJson
                    )
                )
            }
            return document
        }
    }
}