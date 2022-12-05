package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getCommentsContentUriFor
import dev.ragnarok.fenrir.db.column.CommentsColumns
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithBackReference
import dev.ragnarok.fenrir.db.interfaces.Cancelable
import dev.ragnarok.fenrir.db.interfaces.ICommentsStorage
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.exception.DatabaseException
import dev.ragnarok.fenrir.model.CommentUpdate
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.DraftComment
import dev.ragnarok.fenrir.model.criteria.CommentsCriteria
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.serialization.builtins.ListSerializer

internal class CommentsStorage(base: AppStorages) : AbsStorage(base), ICommentsStorage {
    private val minorUpdatesPublisher: PublishSubject<CommentUpdate> = PublishSubject.create()
    private val mStoreLock = Any()
    override fun insert(
        accountId: Int,
        sourceId: Int,
        sourceOwnerId: Int,
        sourceType: Int,
        dbos: List<CommentEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBefore) {
                val delete = ContentProviderOperation
                    .newDelete(getCommentsContentUriFor(accountId))
                    .withSelection(
                        CommentsColumns.SOURCE_ID + " = ? " +
                                " AND " + CommentsColumns.SOURCE_OWNER_ID + " = ? " +
                                " AND " + CommentsColumns.COMMENT_ID + " != ? " +
                                " AND " + CommentsColumns.SOURCE_TYPE + " = ?", arrayOf(
                            sourceId.toString(),
                            sourceOwnerId.toString(),
                            CommentsColumns.PROCESSING_COMMENT_ID.toString(),
                            sourceType.toString()
                        )
                    ).build()
                operations.add(delete)
            }
            val indexes = IntArray(dbos.size)
            for (i in dbos.indices) {
                val dbo = dbos[i]
                val mainPostHeaderOperation = ContentProviderOperation
                    .newInsert(getCommentsContentUriFor(accountId))
                    .withValues(getCV(sourceId, sourceOwnerId, sourceType, dbo))
                    .build()
                val mainPostHeaderIndex =
                    addToListAndReturnIndex(operations, mainPostHeaderOperation)
                indexes[i] = mainPostHeaderIndex
                dbo.getAttachments().nonNullNoEmpty {
                    for (attachmentEntity in it) {
                        appendAttachOperationWithBackReference(
                            operations,
                            accountId,
                            AttachToType.COMMENT,
                            mainPostHeaderIndex,
                            attachmentEntity
                        )
                    }
                }
            }
            if (owners != null) {
                OwnersStorage.appendOwnersInsertOperations(operations, accountId, owners)
            }
            var results: Array<ContentProviderResult>
            synchronized(mStoreLock) {
                results = context.contentResolver.applyBatch(
                    FenrirContentProvider.AUTHORITY,
                    operations
                )
            }
            val ids = IntArray(dbos.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emitter.onSuccess(ids)
        }
    }

    private fun createCursorByCriteria(criteria: CommentsCriteria): Cursor? {
        val uri = getCommentsContentUriFor(criteria.accountId)
        val range = criteria.range
        val commented = criteria.commented
        return if (range == null) {
            contentResolver.query(
                uri, null,
                CommentsColumns.SOURCE_ID + " = ? AND " +
                        CommentsColumns.SOURCE_OWNER_ID + " = ? AND " +
                        CommentsColumns.SOURCE_TYPE + " = ? AND " +
                        CommentsColumns.COMMENT_ID + " != ?", arrayOf(
                    commented.sourceId.toString(),
                    commented.sourceOwnerId.toString(),
                    commented.sourceType.toString(),
                    CommentsColumns.PROCESSING_COMMENT_ID.toString()
                ),
                CommentsColumns.COMMENT_ID + " DESC"
            )
        } else {
            contentResolver.query(
                uri,
                null,
                BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?",
                arrayOf(range.first.toString(), range.last.toString()),
                CommentsColumns.COMMENT_ID + " DESC"
            )
        }
    }

    override fun getDbosByCriteria(criteria: CommentsCriteria): Single<List<CommentEntity>> {
        return Single.create { emitter: SingleEmitter<List<CommentEntity>> ->
            val cursor = createCursorByCriteria(criteria)
            val cancelation = object : Cancelable {
                override val isOperationCancelled: Boolean
                    get() = emitter.isDisposed
            }
            val dbos: MutableList<CommentEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(
                        mapDbo(
                            criteria.accountId, cursor,
                            includeAttachments = true,
                            forceAttachments = false,
                            cancelable = cancelation
                        )
                    )
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun findEditingComment(accountId: Int, commented: Commented): Maybe<DraftComment> {
        return Maybe.create { e: MaybeEmitter<DraftComment> ->
            val cursor = contentResolver.query(
                getCommentsContentUriFor(accountId), null,
                CommentsColumns.COMMENT_ID + " = ? AND " +
                        CommentsColumns.SOURCE_ID + " = ? AND " +
                        CommentsColumns.SOURCE_OWNER_ID + " = ? AND " +
                        CommentsColumns.SOURCE_TYPE + " = ?", arrayOf(
                    CommentsColumns.PROCESSING_COMMENT_ID.toString(),
                    commented.sourceId.toString(),
                    commented.sourceOwnerId.toString(),
                    commented.sourceType.toString()
                ), null
            )
            var comment: DraftComment? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val dbid = cursor.getInt(BaseColumns._ID)
                    val body = cursor.getString(CommentsColumns.TEXT)
                    comment = DraftComment(dbid).setBody(body)
                }
                cursor.close()
            }
            if (comment != null) {
                e.onSuccess(comment)
            }
            e.onComplete()
        }.flatMap { comment ->
            stores
                .attachments()
                .getCount(accountId, AttachToType.COMMENT, comment.getId())
                .flatMapMaybe {
                    Maybe.just(
                        comment.setAttachmentsCount(
                            it
                        )
                    )
                }
        }
    }

    override fun saveDraftComment(
        accountId: Int,
        commented: Commented,
        text: String?,
        replyToUser: Int,
        replyToComment: Int
    ): Single<Int> {
        return Single.create { e: SingleEmitter<Int> ->
            val start = System.currentTimeMillis()
            var id = findEditingCommentId(accountId, commented)
            val contentValues = ContentValues()
            contentValues.put(CommentsColumns.COMMENT_ID, CommentsColumns.PROCESSING_COMMENT_ID)
            contentValues.put(CommentsColumns.TEXT, text)
            contentValues.put(CommentsColumns.SOURCE_ID, commented.sourceId)
            contentValues.put(CommentsColumns.SOURCE_OWNER_ID, commented.sourceOwnerId)
            contentValues.put(CommentsColumns.SOURCE_TYPE, commented.sourceType)
            contentValues.put(CommentsColumns.FROM_ID, accountId)
            contentValues.put(CommentsColumns.DATE, now())
            contentValues.put(CommentsColumns.REPLY_TO_USER, replyToUser)
            contentValues.put(CommentsColumns.REPLY_TO_COMMENT, replyToComment)
            contentValues.put(CommentsColumns.THREADS_COUNT, 0)
            contentValues.putNull(CommentsColumns.THREADS)
            contentValues.put(CommentsColumns.LIKES, 0)
            contentValues.put(CommentsColumns.USER_LIKES, 0)
            val commentsWithAccountUri = getCommentsContentUriFor(accountId)
            if (id == null) {
                val uri = contentResolver.insert(commentsWithAccountUri, contentValues)
                if (uri == null) {
                    e.onError(DatabaseException("Result URI is null"))
                    return@create
                }
                id = uri.pathSegments[1].toInt()
            } else {
                contentResolver.update(
                    commentsWithAccountUri, contentValues,
                    BaseColumns._ID + " = ?", arrayOf(id.toString())
                )
            }
            e.onSuccess(id)
            log("CommentsStorage.saveDraftComment", start, "id: $id")
        }
    }

    override fun commitMinorUpdate(update: CommentUpdate): Completable {
        return Completable.fromAction {
            val cv = ContentValues()
            update.getLikeUpdate().requireNonNull {
                cv.put(CommentsColumns.USER_LIKES, it.isUserLikes())
                cv.put(CommentsColumns.LIKES, it.getCount())
            }
            update.getDeleteUpdate().requireNonNull {
                cv.put(CommentsColumns.DELETED, it.isDeleted())
            }
            val uri = getCommentsContentUriFor(update.getAccountId())
            val where =
                CommentsColumns.SOURCE_OWNER_ID + " = ? AND " + CommentsColumns.COMMENT_ID + " = ?"
            val args =
                arrayOf(
                    update.getCommented().sourceOwnerId.toString(),
                    update.getCommentId().toString()
                )
            contentResolver.update(uri, cv, where, args)
            minorUpdatesPublisher.onNext(update)
        }
    }

    override fun observeMinorUpdates(): Observable<CommentUpdate> {
        return minorUpdatesPublisher
    }

    override fun deleteByDbid(accountId: Int, dbid: Int): Completable {
        return Completable.fromAction {
            val uri = getCommentsContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(dbid.toString())
            contentResolver.delete(uri, where, args)
        }
    }

    private fun findEditingCommentId(aid: Int, commented: Commented): Int? {
        val projection = arrayOf(BaseColumns._ID)
        val cursor = contentResolver.query(
            getCommentsContentUriFor(aid), projection,
            CommentsColumns.COMMENT_ID + " = ? AND " +
                    CommentsColumns.SOURCE_ID + " = ? AND " +
                    CommentsColumns.SOURCE_OWNER_ID + " = ? AND " +
                    CommentsColumns.SOURCE_TYPE + " = ?", arrayOf(
                CommentsColumns.PROCESSING_COMMENT_ID.toString(),
                commented.sourceId.toString(),
                commented.sourceOwnerId.toString(),
                commented.sourceType.toString()
            ), null
        )
        var result: Int? = null
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getInt(0)
            }
            cursor.close()
        }
        return result
    }

    private fun mapDbo(
        accountId: Int,
        cursor: Cursor,
        includeAttachments: Boolean,
        forceAttachments: Boolean,
        cancelable: Cancelable
    ): CommentEntity {
        val attachmentsCount =
            cursor.getInt(CommentsColumns.ATTACHMENTS_COUNT)
        val dbid = cursor.getInt(BaseColumns._ID)
        val sourceId = cursor.getInt(CommentsColumns.SOURCE_ID)
        val sourceOwnerId =
            cursor.getInt(CommentsColumns.SOURCE_OWNER_ID)
        val sourceType = cursor.getInt(CommentsColumns.SOURCE_TYPE)
        val sourceAccessKey =
            cursor.getString(CommentsColumns.SOURCE_ACCESS_KEY)
        val id = cursor.getInt(CommentsColumns.COMMENT_ID)
        val threadsJson = cursor.getBlob(CommentsColumns.THREADS)
        val dbo = CommentEntity().set(sourceId, sourceOwnerId, sourceType, sourceAccessKey, id)
            .setFromId(cursor.getInt(CommentsColumns.FROM_ID))
            .setDate(cursor.getLong(CommentsColumns.DATE))
            .setText(cursor.getString(CommentsColumns.TEXT))
            .setReplyToUserId(cursor.getInt(CommentsColumns.REPLY_TO_USER))
            .setThreadsCount(cursor.getInt(CommentsColumns.THREADS_COUNT))
            .setReplyToComment(cursor.getInt(CommentsColumns.REPLY_TO_COMMENT))
            .setLikesCount(cursor.getInt(CommentsColumns.LIKES))
            .setUserLikes(cursor.getBoolean(CommentsColumns.USER_LIKES))
            .setCanLike(cursor.getBoolean(CommentsColumns.CAN_LIKE))
            .setCanEdit(cursor.getBoolean(CommentsColumns.CAN_EDIT))
            .setDeleted(cursor.getBoolean(CommentsColumns.DELETED))
        if (threadsJson != null) {
            dbo.setThreads(
                MsgPack.decodeFromByteArrayEx(
                    ListSerializer(CommentEntity.serializer()),
                    threadsJson
                )
            )
        }
        if (includeAttachments && (attachmentsCount > 0 || forceAttachments)) {
            dbo.setAttachments(
                stores
                    .attachments()
                    .getAttachmentsDbosSync(accountId, AttachToType.COMMENT, dbid, cancelable)
            )
        }
        return dbo
    }

    companion object {
        fun getCV(
            sourceId: Int,
            sourceOwnerId: Int,
            sourceType: Int,
            dbo: CommentEntity
        ): ContentValues {
            val cv = ContentValues()
            cv.put(CommentsColumns.COMMENT_ID, dbo.id)
            cv.put(CommentsColumns.FROM_ID, dbo.fromId)
            cv.put(CommentsColumns.DATE, dbo.date)
            cv.put(CommentsColumns.TEXT, dbo.text)
            cv.put(CommentsColumns.REPLY_TO_USER, dbo.replyToUserId)
            cv.put(CommentsColumns.REPLY_TO_COMMENT, dbo.replyToComment)
            cv.put(CommentsColumns.THREADS_COUNT, dbo.threadsCount)
            dbo.threads.ifNonNullNoEmpty({
                cv.put(
                    CommentsColumns.THREADS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(CommentEntity.serializer()), it)
                )
            }, {
                cv.putNull(CommentsColumns.THREADS)
            })
            cv.put(CommentsColumns.LIKES, dbo.likesCount)
            cv.put(CommentsColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(CommentsColumns.CAN_LIKE, dbo.isCanLike)
            cv.put(CommentsColumns.ATTACHMENTS_COUNT, dbo.attachmentsCount)
            cv.put(CommentsColumns.SOURCE_ID, sourceId)
            cv.put(CommentsColumns.SOURCE_OWNER_ID, sourceOwnerId)
            cv.put(CommentsColumns.SOURCE_TYPE, sourceType)
            cv.put(CommentsColumns.DELETED, dbo.isDeleted)
            return cv
        }
    }

}