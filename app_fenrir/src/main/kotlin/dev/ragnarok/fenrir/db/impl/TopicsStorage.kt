package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getTopicsContentUriFor
import dev.ragnarok.fenrir.db.column.GroupsColumns
import dev.ragnarok.fenrir.db.column.TopicsColumns
import dev.ragnarok.fenrir.db.impl.OwnersStorage.Companion.appendOwnersInsertOperations
import dev.ragnarok.fenrir.db.interfaces.ITopicsStore
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PollDboEntity
import dev.ragnarok.fenrir.db.model.entity.TopicDboEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.criteria.TopicsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlin.math.abs

internal class TopicsStorage(base: AppStorages) : AbsStorage(base), ITopicsStore {
    override fun getByCriteria(criteria: TopicsCriteria): Single<List<TopicDboEntity>> {
        return Single.create { e: SingleEmitter<List<TopicDboEntity>> ->
            val uri = getTopicsContentUriFor(criteria.accountId)
            val where: String
            val args: Array<String>
            if (criteria.range != null) {
                val range = criteria.range
                where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(range?.first.toString(), range?.last.toString())
            } else {
                where = TopicsColumns.OWNER_ID + " = ?"
                args = arrayOf(criteria.ownerId.toString())
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val topics = ArrayList<TopicDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    topics.add(mapDbo(cursor))
                }
                cursor.close()
            }
            e.onSuccess(topics)
        }
    }

    override fun store(
        accountId: Long,
        ownerId: Long,
        topics: List<TopicDboEntity>,
        owners: OwnerEntities?,
        canAddTopic: Boolean,
        defaultOrder: Int,
        clearBefore: Boolean
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val operations = ArrayList<ContentProviderOperation>()
            val uri = getTopicsContentUriFor(accountId)
            if (owners != null) {
                appendOwnersInsertOperations(operations, accountId, owners)
            }
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation.newDelete(uri)
                        .withSelection(TopicsColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString()))
                        .build()
                )
            }
            for (dbo in topics) {
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(getCV(dbo))
                        .build()
                )
            }
            val cv = ContentValues()
            cv.put(GroupsColumns.CAN_ADD_TOPICS, canAddTopic)
            cv.put(GroupsColumns.TOPICS_ORDER, defaultOrder)
            operations.add(
                ContentProviderOperation
                    .newUpdate(getGroupsContentUriFor(accountId))
                    .withValues(cv)
                    .withSelection(BaseColumns._ID + " = ?", arrayOf(abs(ownerId).toString()))
                    .build()
            )
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            e.onComplete()
        }
    }

    override fun attachPoll(
        accountId: Long,
        ownerId: Long,
        topicId: Int,
        pollDbo: PollDboEntity?
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val cv = ContentValues()
            pollDbo.ifNonNull({
                cv.put(
                    TopicsColumns.ATTACHED_POLL,
                    MsgPack.encodeToByteArrayEx(PollDboEntity.serializer(), it)
                )
            }, {
                cv.putNull(TopicsColumns.ATTACHED_POLL)
            })
            val uri = getTopicsContentUriFor(accountId)
            val where = TopicsColumns.TOPIC_ID + " = ? AND " + TopicsColumns.OWNER_ID + " = ?"
            val args = arrayOf(topicId.toString(), topicId.toString())
            contentResolver.update(uri, cv, where, args)
            e.onComplete()
        }
    }

    companion object {
        fun getCV(dbo: TopicDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(TopicsColumns.TOPIC_ID, dbo.id)
            cv.put(TopicsColumns.OWNER_ID, dbo.ownerId)
            cv.put(TopicsColumns.TITLE, dbo.title)
            cv.put(TopicsColumns.CREATED, dbo.createdTime)
            cv.put(TopicsColumns.CREATED_BY, dbo.creatorId)
            cv.put(TopicsColumns.UPDATED, dbo.lastUpdateTime)
            cv.put(TopicsColumns.UPDATED_BY, dbo.updatedBy)
            cv.put(TopicsColumns.IS_CLOSED, dbo.isClosed)
            cv.put(TopicsColumns.IS_FIXED, dbo.isFixed)
            cv.put(TopicsColumns.COMMENTS, dbo.commentsCount)
            cv.put(TopicsColumns.FIRST_COMMENT, dbo.firstComment)
            cv.put(TopicsColumns.LAST_COMMENT, dbo.lastComment)
            dbo.poll.ifNonNull({
                cv.put(
                    TopicsColumns.ATTACHED_POLL,
                    MsgPack.encodeToByteArrayEx(PollDboEntity.serializer(), it)
                )
            }, {
                cv.putNull(TopicsColumns.ATTACHED_POLL)
            })
            return cv
        }

        internal fun mapDbo(cursor: Cursor): TopicDboEntity {
            val id = cursor.getInt(TopicsColumns.TOPIC_ID)
            val ownerId = cursor.getLong(TopicsColumns.OWNER_ID)
            val dbo = TopicDboEntity().set(id, ownerId)
                .setTitle(cursor.getString(TopicsColumns.TITLE))
                .setCreatedTime(cursor.getLong(TopicsColumns.CREATED))
                .setCreatorId(cursor.getLong(TopicsColumns.CREATED_BY))
                .setLastUpdateTime(cursor.getLong(TopicsColumns.UPDATED))
                .setUpdatedBy(cursor.getLong(TopicsColumns.UPDATED_BY))
                .setClosed(cursor.getBoolean(TopicsColumns.IS_CLOSED))
                .setFixed(cursor.getBoolean(TopicsColumns.IS_FIXED))
                .setCommentsCount(cursor.getInt(TopicsColumns.COMMENTS))
                .setFirstComment(cursor.getString(TopicsColumns.FIRST_COMMENT))
                .setLastComment(cursor.getString(TopicsColumns.LAST_COMMENT))
            val pollJson =
                cursor.getBlob(TopicsColumns.ATTACHED_POLL)
            if (pollJson.nonNullNoEmpty()) {
                dbo.setPoll(MsgPack.decodeFromByteArrayEx(PollDboEntity.serializer(), pollJson))
            }
            return dbo
        }
    }
}