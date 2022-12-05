package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getNotificationsContentUriFor
import dev.ragnarok.fenrir.db.column.NotificationColumns
import dev.ragnarok.fenrir.db.interfaces.IFeedbackStorage
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.feedback.FeedbackEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.criteria.NotificationsCriteria
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class FeedbackStorage(context: AppStorages) : AbsStorage(context), IFeedbackStorage {
    override fun insert(
        accountId: Int,
        dbos: List<FeedbackEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val uri = getNotificationsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            val indexes = IntArray(dbos.size)
            for (i in dbos.indices) {
                val dbo = dbos[i]
                val cv = ContentValues()
                cv.put(NotificationColumns.DATE, dbo.date)
                cv.put(
                    NotificationColumns.CONTENT_PACK,
                    MsgPack.encodeToByteArrayEx(FeedbackEntity.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            OwnersStorage.appendOwnersInsertOperations(operations, accountId, owners)
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(dbos.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emitter.onSuccess(ids)
        }
    }

    override fun findByCriteria(criteria: NotificationsCriteria): Single<List<FeedbackEntity>> {
        return Single.create { e: SingleEmitter<List<FeedbackEntity>> ->
            val range = criteria.range
            val uri = getNotificationsContentUriFor(criteria.accountId)
            val cursor: Cursor? = if (range != null) {
                val where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                val args = arrayOf(range.first.toString(), range.last.toString())
                context.contentResolver.query(
                    uri,
                    null,
                    where,
                    args,
                    NotificationColumns.DATE + " DESC"
                )
            } else {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    NotificationColumns.DATE + " DESC"
                )
            }
            val dtos: MutableList<FeedbackEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    val dto = mapDto(cursor)
                    dtos.add(dto)
                }
                cursor.close()
            }
            e.onSuccess(dtos)
        }
    }

    private fun mapDto(cursor: Cursor): FeedbackEntity {
        val data = cursor.getBlob(NotificationColumns.CONTENT_PACK)!!
        return MsgPack.decodeFromByteArrayEx(FeedbackEntity.serializer(), data)
    }


    override fun insertOfficial(
        accountId: Int,
        dbos: List<FeedbackVKOfficial>,
        clearBefore: Boolean
    ): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val uri = getNotificationsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            val indexes = IntArray(dbos.size)
            for (i in dbos.indices) {
                val dbo = dbos[i]
                val cv = ContentValues()
                cv.put(NotificationColumns.DATE, dbo.time)
                cv.put(
                    NotificationColumns.CONTENT_PACK,
                    MsgPack.encodeToByteArrayEx(FeedbackVKOfficial.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(dbos.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emitter.onSuccess(ids)
        }
    }

    override fun findByCriteriaOfficial(criteria: NotificationsCriteria): Single<List<FeedbackVKOfficial>> {
        return Single.create { e: SingleEmitter<List<FeedbackVKOfficial>> ->
            val range = criteria.range
            val uri = getNotificationsContentUriFor(criteria.accountId)
            val cursor: Cursor? = if (range != null) {
                val where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                val args = arrayOf(range.first.toString(), range.last.toString())
                context.contentResolver.query(
                    uri,
                    null,
                    where,
                    args,
                    NotificationColumns.DATE + " DESC"
                )
            } else {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    NotificationColumns.DATE + " DESC"
                )
            }
            val dtos: MutableList<FeedbackVKOfficial> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    val dto = mapDtoOfficial(cursor)
                    dtos.add(dto)
                }
                cursor.close()
            }
            e.onSuccess(dtos)
        }
    }

    private fun mapDtoOfficial(cursor: Cursor): FeedbackVKOfficial {
        val data = cursor.getBlob(NotificationColumns.CONTENT_PACK)!!
        return MsgPack.decodeFromByteArrayEx(FeedbackVKOfficial.serializer(), data)
    }
}