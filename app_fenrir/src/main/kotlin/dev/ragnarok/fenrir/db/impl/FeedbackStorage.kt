package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getNotificationsContentUriFor
import dev.ragnarok.fenrir.db.column.NotificationsColumns
import dev.ragnarok.fenrir.db.interfaces.IFeedbackStorage
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.feedback.FeedbackEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.criteria.NotificationsCriteria
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.msgpack.MsgPack

internal class FeedbackStorage(context: AppStorages) : AbsStorage(context), IFeedbackStorage {
    override fun insert(
        accountId: Long,
        dbos: List<FeedbackEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Flow<IntArray> {
        return flow {
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
                cv.put(NotificationsColumns.DATE, dbo.date)
                cv.put(
                    NotificationsColumns.CONTENT_PACK,
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
            emit(ids)
        }
    }

    override fun findByCriteria(criteria: NotificationsCriteria): Flow<List<FeedbackEntity>> {
        return flow {
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
                    NotificationsColumns.DATE + " DESC"
                )
            } else {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    NotificationsColumns.DATE + " DESC"
                )
            }
            val dtos: MutableList<FeedbackEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    val dto = mapDto(cursor)
                    dtos.add(dto)
                }
                cursor.close()
            }
            emit(dtos)
        }
    }

    private fun mapDto(cursor: Cursor): FeedbackEntity {
        val data = cursor.getBlob(NotificationsColumns.CONTENT_PACK)!!
        return MsgPack.decodeFromByteArrayEx(FeedbackEntity.serializer(), data)
    }

    override fun insertOfficial(
        accountId: Long,
        dbos: List<FeedbackVKOfficial>,
        clearBefore: Boolean
    ): Flow<IntArray> {
        return flow {
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
                cv.put(NotificationsColumns.DATE, dbo.time)
                cv.put(
                    NotificationsColumns.CONTENT_PACK,
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
            emit(ids)
        }
    }

    override fun findByCriteriaOfficial(criteria: NotificationsCriteria): Flow<List<FeedbackVKOfficial>> {
        return flow {
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
                    NotificationsColumns.DATE + " DESC"
                )
            } else {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    NotificationsColumns.DATE + " DESC"
                )
            }
            val dtos: MutableList<FeedbackVKOfficial> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    val dto = mapDtoOfficial(cursor)
                    dtos.add(dto)
                }
                cursor.close()
            }
            emit(dtos)
        }
    }

    private fun mapDtoOfficial(cursor: Cursor): FeedbackVKOfficial {
        val data = cursor.getBlob(NotificationsColumns.CONTENT_PACK)!!
        return MsgPack.decodeFromByteArrayEx(FeedbackVKOfficial.serializer(), data)
    }
}