package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getDocsContentUriFor
import dev.ragnarok.fenrir.db.column.DocColumns
import dev.ragnarok.fenrir.db.interfaces.IDocsStorage
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.GraffitiDbo
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.VideoPreviewDbo
import dev.ragnarok.fenrir.db.model.entity.PhotoSizeEntity
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.criteria.DocsCriteria
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
                where = DocColumns.OWNER_ID + " = ? AND " + DocColumns.TYPE + " = ?"
                args = arrayOf(criteria.ownerId.toString(), filter.toString())
            } else {
                where = DocColumns.OWNER_ID + " = ?"
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
        accountId: Int,
        ownerId: Int,
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
                        .withSelection(DocColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString()))
                        .build()
                )
            }
            for (entity in entities) {
                val cv = ContentValues()
                cv.put(DocColumns.DOC_ID, entity.id)
                cv.put(DocColumns.OWNER_ID, entity.ownerId)
                cv.put(DocColumns.TITLE, entity.title)
                cv.put(DocColumns.SIZE, entity.size)
                cv.put(DocColumns.EXT, entity.ext)
                cv.put(DocColumns.URL, entity.url)
                cv.put(DocColumns.DATE, entity.date)
                cv.put(DocColumns.TYPE, entity.type)
                cv.put(DocColumns.ACCESS_KEY, entity.accessKey)
                entity.photo.ifNonNull({
                    cv.put(
                        DocColumns.PHOTO,
                        MsgPack.encodeToByteArrayEx(PhotoSizeEntity.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocColumns.PHOTO
                    )
                })
                entity.graffiti.ifNonNull({
                    cv.put(
                        DocColumns.GRAFFITI,
                        MsgPack.encodeToByteArrayEx(GraffitiDbo.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocColumns.GRAFFITI
                    )
                })
                entity.video.ifNonNull({
                    cv.put(
                        DocColumns.VIDEO,
                        MsgPack.encodeToByteArrayEx(VideoPreviewDbo.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        DocColumns.VIDEO
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

    override fun delete(accountId: Int, docId: Int, ownerId: Int): Completable {
        return Completable.fromAction {
            val uri = getDocsContentUriFor(accountId)
            val where = DocColumns.DOC_ID + " = ? AND " + DocColumns.OWNER_ID + " = ?"
            val args = arrayOf(docId.toString(), ownerId.toString())
            contentResolver.delete(uri, where, args)
        }
    }

    companion object {
        internal fun map(cursor: Cursor): DocumentDboEntity {
            val id = cursor.getInt(DocColumns.DOC_ID)
            val ownerId = cursor.getInt(DocColumns.OWNER_ID)
            val document = DocumentDboEntity().set(id, ownerId)
                .setTitle(cursor.getString(DocColumns.TITLE))
                .setSize(cursor.getLong(DocColumns.SIZE))
                .setExt(cursor.getString(DocColumns.EXT))
                .setUrl(cursor.getString(DocColumns.URL))
                .setType(cursor.getInt(DocColumns.TYPE))
                .setDate(cursor.getLong(DocColumns.DATE))
                .setAccessKey(cursor.getString(DocColumns.ACCESS_KEY))
            val photoJson = cursor.getBlob(DocColumns.PHOTO)
            val graffitiJson = cursor.getBlob(DocColumns.GRAFFITI)
            val videoJson = cursor.getBlob(DocColumns.VIDEO)
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