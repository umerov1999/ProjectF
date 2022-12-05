package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getMessageContentUriFor
import dev.ragnarok.fenrir.db.RecordNotFoundException
import dev.ragnarok.fenrir.db.column.MessageColumns
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithBackReference
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithStableAttachToId
import dev.ragnarok.fenrir.db.interfaces.Cancelable
import dev.ragnarok.fenrir.db.interfaces.IMessagesStorage
import dev.ragnarok.fenrir.db.model.MessageEditEntity
import dev.ragnarok.fenrir.db.model.MessagePatch
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.ChatAction
import dev.ragnarok.fenrir.model.DraftMessage
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.model.criteria.MessagesCriteria
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

internal class MessagesStorage(base: AppStorages) : AbsStorage(base), IMessagesStorage {
    override fun insertPeerDbos(
        accountId: Int,
        peerId: Int,
        dbos: List<MessageDboEntity>,
        clearHistory: Boolean
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val operations = ArrayList<ContentProviderOperation>()
            if (clearHistory) {
                val uri = getMessageContentUriFor(accountId)
                val where =
                    MessageColumns.PEER_ID + " = ? AND " + MessageColumns.ATTACH_TO + " = ? AND " + MessageColumns.STATUS + " = ?"
                val args = arrayOf(
                    peerId.toString(),
                    MessageColumns.DONT_ATTACH.toString(),
                    MessageStatus.SENT.toString()
                )
                operations.add(
                    ContentProviderOperation.newDelete(uri).withSelection(where, args).build()
                )
            }
            for (dbo in dbos) {
                appendDboOperation(accountId, dbo, operations, null, null)
            }
            context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun insert(accountId: Int, dbos: List<MessageDboEntity>): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val operations = ArrayList<ContentProviderOperation>()
            val indexes = IntArray(dbos.size)
            for (i in dbos.indices) {
                val dbo = dbos[i]
                val index = appendDboOperation(accountId, dbo, operations, null, null)
                indexes[i] = index
            }
            val results =
                context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(dbos.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emitter.onSuccess(ids)
        }
    }

    override fun findLastSentMessageIdForPeer(accounId: Int, peerId: Int): Single<Optional<Int>> {
        return Single.create { emitter: SingleEmitter<Optional<Int>> ->
            val uri = getMessageContentUriFor(accounId)
            val projection = arrayOf(MessageColumns._ID)
            val where = MessageColumns.PEER_ID + " = ?" +
                    " AND " + MessageColumns.STATUS + " = ?" +
                    " AND " + MessageColumns.ATTACH_TO + " = ?" +
                    " AND " + MessageColumns.DELETED + " = ?"
            val args = arrayOf(
                peerId.toString(), MessageStatus.SENT.toString(),
                MessageColumns.DONT_ATTACH.toString(),
                "0"
            )
            val cursor = contentResolver.query(
                uri,
                projection,
                where,
                args,
                MessageColumns.FULL_ID + " DESC LIMIT 1"
            )
            var id: Int? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    id = cursor.getInt(MessageColumns._ID)
                }
                cursor.close()
            }
            emitter.onSuccess(wrap(id))
        }
    }

    private fun queryMessagesByCriteria(criteria: MessagesCriteria): Cursor? {
        val where: String
        val args: Array<String>
        if (criteria.startMessageId == null) {
            where = MessageColumns.PEER_ID + " = ?" +
                    " AND " + MessageColumns.ATTACH_TO + " = ?" +
                    " AND " + MessageColumns.STATUS + " != ?"
            args = arrayOf(
                criteria.peerId.toString(),
                "0", MessageStatus.EDITING.toString()
            )
        } else {
            where = MessageColumns.PEER_ID + " = ?" +
                    " AND " + MessageColumns.ATTACH_TO + " = ? " +
                    " AND " + MessageColumns.FULL_ID + " < ? " +
                    " AND " + MessageColumns.STATUS + " != ?"
            args = arrayOf(
                criteria.peerId.toString(),
                "0", criteria.startMessageId.toString(), MessageStatus.EDITING.toString()
            )
        }
        val uri = getMessageContentUriFor(criteria.accountId)
        return context.contentResolver.query(uri, null, where, args, ORDER_BY)
    }

    override fun getByCriteria(
        criteria: MessagesCriteria,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<List<MessageDboEntity>> {
        return Single.create { emitter: SingleEmitter<List<MessageDboEntity>> ->
            val start = System.currentTimeMillis()
            val cancelable = object : Cancelable {
                override val isOperationCancelled: Boolean
                    get() = emitter.isDisposed
            }
            val cursor = queryMessagesByCriteria(criteria)
            val dbos = ArrayList<MessageDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    val dbo = fullMapDbo(
                        criteria.accountId,
                        cursor,
                        withAtatchments,
                        withForwardMessages,
                        cancelable
                    )
                    val position = dbos.size - cursor.position
                    dbos.add(position, dbo)
                }
                cursor.close()
            }
            log("MessagesStorage.getByCriteria", start, "count: " + dbos.size)
            emitter.onSuccess(dbos)
        }
    }

    override fun insert(accountId: Int, peerId: Int, patch: MessageEditEntity): Single<Int> {
        return Single.create { emitter: SingleEmitter<Int> ->
            val operations = ArrayList<ContentProviderOperation>()
            val cv = ContentValues()
            cv.put(MessageColumns.PEER_ID, peerId)
            cv.put(MessageColumns.FROM_ID, patch.senderId)
            cv.put(MessageColumns.DATE, patch.date)
            //cv.put(MessageColumns.READ_STATE, patch.isRead());
            cv.put(MessageColumns.OUT, patch.isOut)
            //cv.put(MessageColumns.TITLE, patch.getTitle());
            cv.put(MessageColumns.BODY, patch.body)
            cv.put(MessageColumns.ENCRYPTED, patch.isEncrypted)
            cv.put(MessageColumns.IMPORTANT, patch.isImportant)
            cv.put(MessageColumns.DELETED, patch.isDeleted)
            cv.put(MessageColumns.FORWARD_COUNT, safeCountOf(patch.forward))
            cv.put(MessageColumns.HAS_ATTACHMENTS, patch.attachments.nonNullNoEmpty())
            cv.put(MessageColumns.STATUS, patch.status)
            cv.put(MessageColumns.ATTACH_TO, MessageColumns.DONT_ATTACH)
            patch.extras.ifNonNull({
                cv.put(
                    MessageColumns.EXTRAS,
                    MsgPack.encodeToByteArrayEx(
                        MapSerializer(Int.serializer(), String.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(
                    MessageColumns.EXTRAS
                )
            })

            cv.put(MessageColumns.PAYLOAD, patch.payload)
            patch.keyboard.ifNonNull({
                cv.put(
                    MessageColumns.KEYBOARD,
                    MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    MessageColumns.KEYBOARD
                )
            })

            // Other fileds is NULL
            val uri = getMessageContentUriFor(accountId)
            val builder = ContentProviderOperation.newInsert(uri).withValues(cv)
            val index = addToListAndReturnIndex(operations, builder.build())
            patch.attachments.nonNullNoEmpty {
                for (attachmentEntity in it) {
                    appendAttachOperationWithBackReference(
                        operations,
                        accountId,
                        AttachToType.MESSAGE,
                        index,
                        attachmentEntity
                    )
                }
            }
            patch.forward.nonNullNoEmpty {
                for (fwdDbo in it) {
                    appendDboOperation(accountId, fwdDbo, operations, null, index)
                }
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val resultMessageId = extractId(results[index])
            emitter.onSuccess(resultMessageId)
        }
    }

    override fun applyPatch(accountId: Int, messageId: Int, patch: MessageEditEntity): Single<Int> {
        return stores.attachments()
            .getCount(accountId, AttachToType.MESSAGE, messageId)
            .flatMap { count ->
                Single
                    .create { emitter: SingleEmitter<Int> ->
                        val uri = getMessageContentUriFor(accountId)
                        val operations = ArrayList<ContentProviderOperation>()
                        val cv = ContentValues()
                        cv.put(MessageColumns.FROM_ID, patch.senderId)
                        cv.put(MessageColumns.DATE, patch.date)
                        //cv.put(MessageColumns.READ_STATE, patch.isRead());
                        cv.put(MessageColumns.OUT, patch.isOut)
                        //cv.put(MessageColumns.TITLE, patch.getTitle());
                        cv.put(MessageColumns.BODY, patch.body)
                        cv.put(MessageColumns.ENCRYPTED, patch.isEncrypted)
                        cv.put(MessageColumns.IMPORTANT, patch.isImportant)
                        cv.put(MessageColumns.DELETED, patch.isDeleted)
                        cv.put(MessageColumns.FORWARD_COUNT, safeCountOf(patch.forward))
                        cv.put(
                            MessageColumns.HAS_ATTACHMENTS,
                            count + safeCountOf(patch.attachments) > 0
                        )
                        cv.put(MessageColumns.STATUS, patch.status)
                        cv.put(MessageColumns.ATTACH_TO, MessageColumns.DONT_ATTACH)

                        patch.extras.ifNonNull({
                            cv.put(
                                MessageColumns.EXTRAS,
                                MsgPack.encodeToByteArrayEx(
                                    MapSerializer(
                                        Int.serializer(),
                                        String.serializer()
                                    ), it
                                )
                            )
                        }, {
                            cv.putNull(
                                MessageColumns.EXTRAS
                            )
                        })

                        cv.put(MessageColumns.PAYLOAD, patch.payload)
                        patch.keyboard.ifNonNull({
                            cv.put(
                                MessageColumns.KEYBOARD,
                                MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
                            )
                        }, {
                            cv.putNull(
                                MessageColumns.KEYBOARD
                            )
                        })
                        val where = MessageColumns._ID + " = ?"
                        val args = arrayOf(messageId.toString())
                        operations.add(
                            ContentProviderOperation.newUpdate(uri).withValues(cv)
                                .withSelection(where, args).build()
                        )
                        patch.attachments.nonNullNoEmpty {
                            for (entity in it) {
                                appendAttachOperationWithStableAttachToId(
                                    operations,
                                    accountId,
                                    AttachToType.MESSAGE,
                                    messageId,
                                    entity
                                )
                            }
                        }
                        patch.forward.nonNullNoEmpty {
                            for (dbo in it) {
                                appendDboOperation(accountId, dbo, operations, messageId, null)
                            }
                        }
                        contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
                        emitter.onSuccess(messageId)
                    }
            }
    }

    private fun fullMapDbo(
        accountId: Int,
        cursor: Cursor,
        withAttachments: Boolean,
        withForwardMessages: Boolean,
        cancelable: Cancelable
    ): MessageDboEntity {
        val dbo = baseMapDbo(cursor)
        if (withAttachments && dbo.isHasAttachmens) {
            val attachments = stores
                .attachments()
                .getAttachmentsDbosSync(accountId, AttachToType.MESSAGE, dbo.id, cancelable)
            dbo.setAttachments(attachments)
        } else {
            dbo.setAttachments(null)
        }
        if (withForwardMessages && dbo.forwardCount > 0) {
            val fwds = getForwardMessages(accountId, dbo.id, withAttachments, cancelable)
            dbo.setForwardMessages(fwds)
        } else {
            dbo.setForwardMessages(null)
        }
        return dbo
    }

    override fun findDraftMessage(accountId: Int, peerId: Int): Maybe<DraftMessage> {
        return Maybe.create { e: MaybeEmitter<DraftMessage> ->
            val columns = arrayOf(MessageColumns._ID, MessageColumns.BODY)
            val uri = getMessageContentUriFor(accountId)
            val cursor = context.contentResolver.query(
                uri,
                columns,
                MessageColumns.PEER_ID + " = ? AND " + MessageColumns.STATUS + " = ?",
                arrayOf(peerId.toString(), MessageStatus.EDITING.toString()),
                null
            )
            if (e.isDisposed) return@create
            var message: DraftMessage? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val id = cursor.getInt(MessageColumns._ID)
                    val body = cursor.getString(MessageColumns.BODY)
                    message = DraftMessage(id, body)
                }
                cursor.close()
            }
            if (message != null) {
                val count = stores.attachments()
                    .getCount(accountId, AttachToType.MESSAGE, message.getId())
                    .blockingGet()
                message.setAttachmentsCount(count)
                e.onSuccess(message)
            }
            e.onComplete()
        }
    }

    override fun saveDraftMessageBody(acocuntId: Int, peerId: Int, body: String?): Single<Int> {
        return Single.create { e: SingleEmitter<Int> ->
            val start = System.currentTimeMillis()
            val uri = getMessageContentUriFor(acocuntId)
            val cv = ContentValues()
            cv.put(MessageColumns.BODY, body)
            cv.put(MessageColumns.PEER_ID, peerId)
            cv.put(MessageColumns.STATUS, MessageStatus.EDITING)
            val cr = contentResolver
            var existDraftMessageId = findDraftMessageId(acocuntId, peerId)
            //.blockingGet();
            if (existDraftMessageId != null) {
                cr.update(
                    uri,
                    cv,
                    MessageColumns._ID + " = ?",
                    arrayOf(existDraftMessageId.toString())
                )
            } else {
                val resultUri = cr.insert(uri, cv)
                existDraftMessageId = resultUri?.lastPathSegment?.toInt() ?: -1
            }
            e.onSuccess(existDraftMessageId)
            log("saveDraftMessageBody", start)
        }
    }

    override fun applyPatches(accountId: Int, patches: Collection<MessagePatch>): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val uri = getMessageContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(patches.size)
            for (patch in patches) {
                val cv = ContentValues()
                if (patch.deletion != null) {
                    cv.put(MessageColumns.DELETED, patch.deletion?.deleted == true)
                    cv.put(MessageColumns.DELETED_FOR_ALL, patch.deletion?.deletedForAll == true)
                }
                if (patch.important != null) {
                    cv.put(MessageColumns.IMPORTANT, patch.important?.important == true)
                }
                if (cv.size() == 0) continue
                operations.add(
                    ContentProviderOperation.newUpdate(uri)
                        .withValues(cv)
                        .withSelection(
                            MessageColumns._ID + " = ?",
                            arrayOf(patch.messageId.toString())
                        )
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun getMessageStatus(accountId: Int, dbid: Int): Single<Int> {
        return Single.fromCallable {
            val cursor = contentResolver.query(
                getMessageContentUriFor(accountId),
                arrayOf(MessageColumns.STATUS),
                MessageColumns.FULL_ID + " = ?",
                arrayOf(dbid.toString()),
                null
            )
            var result: Int? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    result = cursor.getInt(MessageColumns.STATUS)
                }
                cursor.close()
            }
            if (result == null) {
                throw RecordNotFoundException("Message with id $dbid not found")
            }
            result
        }
    }

    private fun findDraftMessageId(accountId: Int, peerId: Int): Int? {
        val columns = arrayOf(MessageColumns._ID)
        val uri = getMessageContentUriFor(accountId)
        val cursor = context.contentResolver.query(
            uri,
            columns,
            MessageColumns.PEER_ID + " = ? AND " + MessageColumns.STATUS + " = ?",
            arrayOf(peerId.toString(), MessageStatus.EDITING.toString()),
            null
        )
        var id: Int? = null
        if (cursor != null) {
            if (cursor.moveToNext()) {
                id = cursor.getInt(MessageColumns._ID)
            }
            cursor.close()
        }
        return id
    }

    override fun changeMessageStatus(
        accountId: Int,
        messageId: Int,
        @MessageStatus status: Int,
        vkid: Int?
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val contentValues = ContentValues()
            contentValues.put(MessageColumns.STATUS, status)
            if (vkid != null) {
                contentValues.put(MessageColumns._ID, vkid)
            }
            val uri = getMessageContentUriFor(accountId)
            val count = context.contentResolver.update(
                uri, contentValues,
                MessageColumns._ID + " = ?", arrayOf(messageId.toString())
            )
            if (count > 0) {
                e.onComplete()
            } else {
                e.onError(NotFoundException())
            }
        }
    }

    override fun deleteMessage(accountId: Int, messageId: Int): Single<Boolean> {
        require(messageId != 0) { "Invalid message id: $messageId" }
        return Single.create { e: SingleEmitter<Boolean> ->
            val uri = getMessageContentUriFor(accountId)
            val count = context.contentResolver.delete(
                uri,
                MessageColumns._ID + " = ?",
                arrayOf(messageId.toString())
            )
            e.onSuccess(count > 0)
        }
    }

    override fun deleteMessages(accountId: Int, ids: Collection<Int>): Single<Boolean> {
        return Single.create { e: SingleEmitter<Boolean> ->
            val copy: Set<Int> = HashSet(ids)
            val uri = getMessageContentUriFor(accountId)
            val where = MessageColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val count = context.contentResolver.delete(uri, where, null)
            e.onSuccess(count > 0)
        }
    }

    override fun changeMessagesStatus(
        accountId: Int,
        ids: Collection<Int>,
        @MessageStatus status: Int
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val copy: Set<Int> = HashSet(ids)
            val contentValues = ContentValues()
            contentValues.put(MessageColumns.STATUS, status)
            val uri = getMessageContentUriFor(accountId)
            val where = MessageColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val count = context.contentResolver.update(
                uri, contentValues,
                where, null
            )
            if (count > 0) {
                e.onComplete()
            } else {
                e.onError(NotFoundException())
            }
        }
    }

    override fun getMissingMessages(accountId: Int, ids: Collection<Int>): Single<List<Int>> {
        return Single.create { e: SingleEmitter<List<Int>> ->
            val copy: MutableSet<Int> = HashSet(ids)
            val uri = getMessageContentUriFor(accountId)
            val projection = arrayOf(MessageColumns._ID)
            val where = MessageColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val cursor = contentResolver.query(uri, projection, where, null, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(MessageColumns._ID)
                    copy.remove(id)
                }
                cursor.close()
            }
            e.onSuccess(ArrayList(copy))
        }
    }

    private fun getForwardMessages(
        accountId: Int,
        attachTo: Int,
        withAttachments: Boolean,
        cancelable: Cancelable
    ): List<MessageDboEntity> {
        val uri = getMessageContentUriFor(accountId)
        val where = MessageColumns.ATTACH_TO + " = ?"
        val args = arrayOf(attachTo.toString())
        val cursor = contentResolver.query(uri, null, where, args, MessageColumns.FULL_ID + " DESC")
        val dbos: MutableList<MessageDboEntity> = ArrayList(safeCountOf(cursor))
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (cancelable.isOperationCancelled) {
                    break
                }
                val dbo = fullMapDbo(accountId, cursor, withAttachments, true, cancelable)

                // Хз куда это еще влепить
                //dbo.setRead(true);
                dbo.setOut(dbo.fromId == accountId)
                dbos.add(dbos.size - cursor.position, dbo)
            }
            cursor.close()
        }
        return dbos
    }

    override fun findMessagesByIds(
        accountId: Int,
        ids: List<Int>,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<List<MessageDboEntity>> {
        return Single.create { emitter: SingleEmitter<List<MessageDboEntity>> ->
            val uri = getMessageContentUriFor(accountId)
            val where: String
            val args: Array<String>?
            if (ids.size == 1) {
                where = MessageColumns._ID + " = ?"
                args = arrayOf(ids[0].toString())
            } else {
                where = MessageColumns.FULL_ID + " IN (" + join(",", ids) + ")"
                args = null
            }
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            val cancelable = object : Cancelable {
                override val isOperationCancelled: Boolean
                    get() = emitter.isDisposed
            }
            val dbos = ArrayList<MessageDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    val dbo = fullMapDbo(
                        accountId,
                        cursor,
                        withAtatchments,
                        withForwardMessages,
                        cancelable
                    )
                    val position = dbos.size - cursor.position
                    dbos.add(position, dbo)
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun findFirstUnsentMessage(
        accountIds: Collection<Int>,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<Optional<Pair<Int, MessageDboEntity>>> {
        return Single.create { emitter: SingleEmitter<Optional<Pair<Int, MessageDboEntity>>> ->
            val where = MessageColumns.STATUS + " = ?"
            val args = arrayOf(MessageStatus.QUEUE.toString())
            val orderBy = MessageColumns._ID + " ASC LIMIT 1"
            for (accountId in accountIds) {
                if (emitter.isDisposed) {
                    break
                }
                val uri = getMessageContentUriFor(accountId)
                val cursor = contentResolver.query(uri, null, where, args, orderBy)
                var entity: MessageDboEntity? = null
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        entity = fullMapDbo(
                            accountId,
                            cursor,
                            withAtatchments,
                            withForwardMessages,
                            object : Cancelable {
                                override val isOperationCancelled: Boolean
                                    get() = emitter.isDisposed
                            })
                    }
                    cursor.close()
                }
                if (entity != null) {
                    emitter.onSuccess(wrap(create(accountId, entity)))
                    return@create
                }
            }
            emitter.onSuccess(empty())
        }
    }

    override fun notifyMessageHasAttachments(accountId: Int, messageId: Int): Completable {
        return Completable.fromAction {
            val cv = ContentValues()
            cv.put(MessageColumns.HAS_ATTACHMENTS, true)
            val uri = getMessageContentUriFor(accountId)
            val where = MessageColumns._ID + " = ?"
            val args = arrayOf(messageId.toString())
            contentResolver.update(uri, cv, where, args)
        }
    }

    override fun getForwardMessageIds(
        accountId: Int,
        attachTo: Int,
        pair: Int
    ): Single<Pair<Boolean, List<Int>>> {
        return Single.create { e: SingleEmitter<Pair<Boolean, List<Int>>> ->
            val uri = getMessageContentUriFor(accountId)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MessageColumns.ORIGINAL_ID, MessageColumns.PEER_ID),
                MessageColumns.ATTACH_TO + " = ?",
                arrayOf(attachTo.toString()),
                MessageColumns.FULL_ID + " DESC"
            )
            val ids = ArrayList<Int>(safeCountOf(cursor))
            var from_peer: Int? = null
            var isFirst = true
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    if (isFirst) {
                        isFirst = false
                        from_peer =
                            cursor.getInt(MessageColumns.PEER_ID)
                    }
                    ids.add(cursor.getInt(MessageColumns.ORIGINAL_ID))
                }
                cursor.close()
            }
            e.onSuccess(Pair(ids.size == 1 && pair == from_peer, ids))
        }
    }

    companion object {
        private const val ORDER_BY = MessageColumns.FULL_STATUS + ", " + MessageColumns.FULL_ID
        fun appendDboOperation(
            accountId: Int,
            dbo: MessageDboEntity,
            target: MutableList<ContentProviderOperation>,
            attachToId: Int?,
            attachToIndex: Int?
        ): Int {
            val cv = ContentValues()
            if (attachToId != null) {
                // если есть ID сообщения, к которому прикреплено dbo
                cv.put(MessageColumns.ATTACH_TO, attachToId)
            } else if (attachToIndex == null) {
                // если сообщение не прикреплено к другому
                cv.put(MessageColumns._ID, dbo.id)
                cv.put(MessageColumns.ATTACH_TO, MessageColumns.DONT_ATTACH)
            }
            cv.put(MessageColumns.PEER_ID, dbo.peerId)
            cv.put(MessageColumns.FROM_ID, dbo.fromId)
            cv.put(MessageColumns.DATE, dbo.date)
            //cv.put(MessageColumns.READ_STATE, dbo.isRead());
            cv.put(MessageColumns.OUT, dbo.isOut)
            //cv.put(MessageColumns.TITLE, dbo.getTitle());
            cv.put(MessageColumns.BODY, dbo.body)
            cv.put(MessageColumns.ENCRYPTED, dbo.isEncrypted)
            cv.put(MessageColumns.IMPORTANT, dbo.isImportant)
            cv.put(MessageColumns.DELETED, dbo.isDeleted)
            cv.put(MessageColumns.FORWARD_COUNT, dbo.forwardCount)
            cv.put(MessageColumns.HAS_ATTACHMENTS, dbo.isHasAttachmens)
            cv.put(MessageColumns.STATUS, dbo.status)
            cv.put(MessageColumns.ORIGINAL_ID, dbo.originalId)
            cv.put(MessageColumns.ACTION, dbo.action)
            cv.put(MessageColumns.ACTION_MID, dbo.actionMemberId)
            cv.put(MessageColumns.ACTION_EMAIL, dbo.actionEmail)
            cv.put(MessageColumns.ACTION_TEXT, dbo.actionText)
            cv.put(MessageColumns.PHOTO_50, dbo.photo50)
            cv.put(MessageColumns.PHOTO_100, dbo.photo100)
            cv.put(MessageColumns.PHOTO_200, dbo.photo200)
            cv.put(MessageColumns.RANDOM_ID, dbo.randomId)
            dbo.extras.ifNonNull({
                cv.put(
                    MessageColumns.EXTRAS,
                    MsgPack.encodeToByteArrayEx(
                        MapSerializer(Int.serializer(), String.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(
                    MessageColumns.EXTRAS
                )
            })

            dbo.keyboard.ifNonNull({
                cv.put(
                    MessageColumns.KEYBOARD,
                    MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    MessageColumns.KEYBOARD
                )
            })
            cv.put(MessageColumns.UPDATE_TIME, dbo.updateTime)
            cv.put(MessageColumns.PAYLOAD, dbo.payload)
            val uri = getMessageContentUriFor(accountId)
            val builder = ContentProviderOperation.newInsert(uri)
                .withValues(cv)

            // если сообщение прикреплено к другому, но его ID на данный момент неизвестен
            if (attachToId == null && attachToIndex != null) {
                builder.withValueBackReference(MessageColumns.ATTACH_TO, attachToIndex)
            }
            val index = addToListAndReturnIndex(target, builder.build())
            if (dbo.isHasAttachmens) {
                dbo.getAttachments().nonNullNoEmpty {
                    for (attachmentEntity in it) {
                        appendAttachOperationWithBackReference(
                            target,
                            accountId,
                            AttachToType.MESSAGE,
                            index,
                            attachmentEntity
                        )
                    }
                }
            }
            if (dbo.forwardCount > 0) {
                dbo.forwardMessages.nonNullNoEmpty {
                    for (fwdDbo in it) {
                        appendDboOperation(accountId, fwdDbo, target, null, index)
                    }
                }
            }
            return index
        }

        internal fun baseMapDbo(cursor: Cursor): MessageDboEntity {
            @MessageStatus val status =
                cursor.getInt(MessageColumns.STATUS)
            @ChatAction val action =
                cursor.getInt(MessageColumns.ACTION)
            val id = cursor.getInt(MessageColumns._ID)
            val peerId = cursor.getInt(MessageColumns.PEER_ID)
            val fromId = cursor.getInt(MessageColumns.FROM_ID)
            var extras: Map<Int, String>? = null
            var keyboard: KeyboardEntity? = null
            val extrasText = cursor.getBlob(MessageColumns.EXTRAS)
            if (extrasText.nonNullNoEmpty()) {
                extras = MsgPack.decodeFromByteArrayEx(
                    MapSerializer(
                        Int.serializer(),
                        String.serializer()
                    ), extrasText
                )
            }
            val keyboardText =
                cursor.getBlob(MessageColumns.KEYBOARD)
            if (keyboardText.nonNullNoEmpty()) {
                keyboard = MsgPack.decodeFromByteArrayEx(KeyboardEntity.serializer(), keyboardText)
            }
            return MessageDboEntity().set(id, peerId, fromId)
                .setEncrypted(cursor.getBoolean(MessageColumns.ENCRYPTED))
                .setStatus(status)
                .setAction(action)
                .setExtras(extras)
                .setBody(cursor.getString(MessageColumns.BODY)) //.setRead(cursor.getBoolean(MessageColumns.READ_STATE))
                .setOut(cursor.getBoolean(MessageColumns.OUT))
                .setStatus(status)
                .setDate(cursor.getLong(MessageColumns.DATE))
                .setHasAttachmens(cursor.getBoolean(MessageColumns.HAS_ATTACHMENTS))
                .setForwardCount(cursor.getInt(MessageColumns.FORWARD_COUNT))
                .setDeleted(cursor.getBoolean(MessageColumns.DELETED))
                .setDeletedForAll(cursor.getBoolean(MessageColumns.DELETED_FOR_ALL)) //.setTitle(cursor.getString(MessageColumns.TITLE))
                .setOriginalId(cursor.getInt(MessageColumns.ORIGINAL_ID))
                .setImportant(cursor.getBoolean(MessageColumns.IMPORTANT))
                .setAction(action)
                .setActionMemberId(cursor.getInt(MessageColumns.ACTION_MID))
                .setActionEmail(cursor.getString(MessageColumns.ACTION_EMAIL))
                .setActionText(cursor.getString(MessageColumns.ACTION_TEXT))
                .setPhoto50(cursor.getString(MessageColumns.PHOTO_50))
                .setPhoto100(cursor.getString(MessageColumns.PHOTO_100))
                .setPhoto200(cursor.getString(MessageColumns.PHOTO_200))
                .setRandomId(cursor.getLong(MessageColumns.RANDOM_ID))
                .setUpdateTime(cursor.getLong(MessageColumns.UPDATE_TIME))
                .setPayload(cursor.getString(MessageColumns.PAYLOAD))
                .setKeyboard(keyboard)
        }
    }
}