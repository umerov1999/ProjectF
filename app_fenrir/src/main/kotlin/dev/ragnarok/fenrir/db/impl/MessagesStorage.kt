package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getMessageContentUriFor
import dev.ragnarok.fenrir.db.RecordNotFoundException
import dev.ragnarok.fenrir.db.column.MessagesColumns
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithBackReference
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithStableAttachToId
import dev.ragnarok.fenrir.db.interfaces.Cancelable
import dev.ragnarok.fenrir.db.interfaces.IMessagesStorage
import dev.ragnarok.fenrir.db.model.MessageEditEntity
import dev.ragnarok.fenrir.db.model.MessagePatch
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity
import dev.ragnarok.fenrir.db.model.entity.ReactionEntity
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.ChatAction
import dev.ragnarok.fenrir.model.DraftMessage
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.model.criteria.MessagesCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.msgpack.MsgPack

internal class MessagesStorage(base: AppStorages) : AbsStorage(base), IMessagesStorage {
    override fun insertPeerDbos(
        accountId: Long,
        peerId: Long,
        dbos: List<MessageDboEntity>,
        clearHistory: Boolean
    ): Flow<Boolean> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>()
            if (clearHistory) {
                val uri = getMessageContentUriFor(accountId)
                val where =
                    MessagesColumns.PEER_ID + " = ? AND " + MessagesColumns.ATTACH_TO + " = ? AND " + MessagesColumns.STATUS + " = ?"
                val args = arrayOf(
                    peerId.toString(),
                    MessagesColumns.DONT_ATTACH.toString(),
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
            emit(true)
        }
    }

    override fun insert(accountId: Long, dbos: List<MessageDboEntity>): Flow<IntArray> {
        return flow {
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
            emit(ids)
        }
    }

    override fun findLastSentMessageIdForPeer(
        accountId: Long,
        peerId: Long
    ): Flow<Optional<Int>> {
        return flow {
            val uri = getMessageContentUriFor(accountId)
            val projection = arrayOf(MessagesColumns._ID)
            val where = MessagesColumns.PEER_ID + " = ?" +
                    " AND " + MessagesColumns.STATUS + " = ?" +
                    " AND " + MessagesColumns.ATTACH_TO + " = ?" +
                    " AND " + MessagesColumns.DELETED + " = ?"
            val args = arrayOf(
                peerId.toString(), MessageStatus.SENT.toString(),
                MessagesColumns.DONT_ATTACH.toString(),
                "0"
            )
            val cursor = contentResolver.query(
                uri,
                projection,
                where,
                args,
                MessagesColumns.FULL_ID + " DESC LIMIT 1"
            )
            var id: Int? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    id = cursor.getInt(MessagesColumns._ID)
                }
                cursor.close()
            }
            emit(wrap(id))
        }
    }

    private fun queryMessagesByCriteria(criteria: MessagesCriteria): Cursor? {
        val where: String
        val args: Array<String>
        if (criteria.startMessageId == null) {
            where = MessagesColumns.PEER_ID + " = ?" +
                    " AND " + MessagesColumns.ATTACH_TO + " = ?" +
                    " AND " + MessagesColumns.STATUS + " != ?"
            args = arrayOf(
                criteria.peerId.toString(),
                "0", MessageStatus.EDITING.toString()
            )
        } else {
            where = MessagesColumns.PEER_ID + " = ?" +
                    " AND " + MessagesColumns.ATTACH_TO + " = ? " +
                    " AND " + MessagesColumns.FULL_ID + " < ? " +
                    " AND " + MessagesColumns.STATUS + " != ?"
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
    ): Flow<List<MessageDboEntity>> {
        return flow {
            val start = System.currentTimeMillis()
            val cancelable = object : Cancelable {
                override suspend fun canceled(): Boolean = !isActive()
            }
            val cursor = queryMessagesByCriteria(criteria)
            val dbos = ArrayList<MessageDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
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
            emit(dbos)
        }
    }

    override fun insert(accountId: Long, peerId: Long, patch: MessageEditEntity): Flow<Int> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>()
            val cv = ContentValues()
            cv.put(MessagesColumns.PEER_ID, peerId)
            cv.put(MessagesColumns.FROM_ID, patch.senderId)
            cv.put(MessagesColumns.DATE, patch.date)
            //cv.put(MessageColumns.READ_STATE, patch.isRead());
            cv.put(MessagesColumns.OUT, patch.isOut)
            //cv.put(MessageColumns.TITLE, patch.getTitle());
            cv.put(MessagesColumns.TEXT, patch.text)
            cv.put(MessagesColumns.ENCRYPTED, patch.isEncrypted)
            cv.put(MessagesColumns.IMPORTANT, patch.isImportant)
            cv.put(MessagesColumns.DELETED, patch.isDeleted)
            cv.put(MessagesColumns.FORWARD_COUNT, safeCountOf(patch.forward))
            cv.put(MessagesColumns.HAS_ATTACHMENTS, patch.attachments.nonNullNoEmpty())
            cv.put(MessagesColumns.STATUS, patch.status)
            cv.put(MessagesColumns.ATTACH_TO, MessagesColumns.DONT_ATTACH)
            patch.extras.ifNonNull({
                cv.put(
                    MessagesColumns.EXTRAS,
                    MsgPack.encodeToByteArrayEx(
                        MapSerializer(Int.serializer(), String.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(
                    MessagesColumns.EXTRAS
                )
            })

            cv.put(MessagesColumns.PAYLOAD, patch.payload)

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
            emit(resultMessageId)
        }
    }

    override fun applyPatch(
        accountId: Long,
        messageId: Int,
        patch: MessageEditEntity
    ): Flow<Int> {
        return stores.attachments()
            .getCount(accountId, AttachToType.MESSAGE, messageId)
            .map { count ->
                val uri = getMessageContentUriFor(accountId)
                val operations = ArrayList<ContentProviderOperation>()
                val cv = ContentValues()
                cv.put(MessagesColumns.FROM_ID, patch.senderId)
                cv.put(MessagesColumns.DATE, patch.date)
                //cv.put(MessageColumns.READ_STATE, patch.isRead());
                cv.put(MessagesColumns.OUT, patch.isOut)
                //cv.put(MessageColumns.TITLE, patch.getTitle());
                cv.put(MessagesColumns.TEXT, patch.text)
                cv.put(MessagesColumns.ENCRYPTED, patch.isEncrypted)
                cv.put(MessagesColumns.IMPORTANT, patch.isImportant)
                cv.put(MessagesColumns.DELETED, patch.isDeleted)
                cv.put(MessagesColumns.FORWARD_COUNT, safeCountOf(patch.forward))
                cv.put(
                    MessagesColumns.HAS_ATTACHMENTS,
                    count + safeCountOf(patch.attachments) > 0
                )
                cv.put(MessagesColumns.STATUS, patch.status)
                cv.put(MessagesColumns.ATTACH_TO, MessagesColumns.DONT_ATTACH)

                patch.extras.ifNonNull({
                    cv.put(
                        MessagesColumns.EXTRAS,
                        MsgPack.encodeToByteArrayEx(
                            MapSerializer(
                                Int.serializer(),
                                String.serializer()
                            ), it
                        )
                    )
                }, {
                    cv.putNull(
                        MessagesColumns.EXTRAS
                    )
                })

                cv.put(MessagesColumns.PAYLOAD, patch.payload)
                val where = MessagesColumns._ID + " = ?"
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
                messageId
            }
    }

    private suspend fun fullMapDbo(
        accountId: Long,
        cursor: Cursor,
        withAttachments: Boolean,
        withForwardMessages: Boolean,
        cancelable: Cancelable
    ): MessageDboEntity {
        val dbo = baseMapDbo(cursor)
        if (withAttachments && dbo.isHasAttachments) {
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

    override fun findDraftMessage(accountId: Long, peerId: Long): Flow<DraftMessage?> {
        return flow {
            val columns = arrayOf(MessagesColumns._ID, MessagesColumns.TEXT)
            val uri = getMessageContentUriFor(accountId)
            val cursor = context.contentResolver.query(
                uri,
                columns,
                MessagesColumns.PEER_ID + " = ? AND " + MessagesColumns.STATUS + " = ?",
                arrayOf(peerId.toString(), MessageStatus.EDITING.toString()),
                null
            )
            if (isActive()) {
                var message: DraftMessage? = null
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        val id = cursor.getInt(MessagesColumns._ID)
                        val body = cursor.getString(MessagesColumns.TEXT)
                        message = DraftMessage(id, body)
                    }
                    cursor.close()
                }
                if (message != null) {
                    val count = stores.attachments()
                        .getCount(accountId, AttachToType.MESSAGE, message.id)
                        .single()
                    message.setAttachmentsCount(count)
                }
                emit(message)
            }
        }
    }

    override fun saveDraftMessageBody(accountId: Long, peerId: Long, text: String?): Flow<Int> {
        return flow {
            val start = System.currentTimeMillis()
            val uri = getMessageContentUriFor(accountId)
            val cv = ContentValues()
            cv.put(MessagesColumns.TEXT, text)
            cv.put(MessagesColumns.PEER_ID, peerId)
            cv.put(MessagesColumns.STATUS, MessageStatus.EDITING)
            val cr = contentResolver
            var existDraftMessageId = findDraftMessageId(accountId, peerId)
            if (existDraftMessageId != null) {
                cr.update(
                    uri,
                    cv,
                    MessagesColumns._ID + " = ?",
                    arrayOf(existDraftMessageId.toString())
                )
            } else {
                val resultUri = cr.insert(uri, cv)
                existDraftMessageId = resultUri?.lastPathSegment?.toInt() ?: -1
            }
            log("saveDraftMessageBody", start)
            emit(existDraftMessageId)
        }
    }

    override fun applyPatches(accountId: Long, patches: Collection<MessagePatch>): Flow<Boolean> {
        return flow {
            val uri = getMessageContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(patches.size)
            for (patch in patches) {
                patch.reaction.requireNonNull { to ->
                    val cv = ContentValues()

                    if (!to.keepMyReaction) {
                        cv.put(MessagesColumns.REACTION_ID, to.reactionId)
                    }
                    to.reactions.ifNonNullNoEmpty({
                        cv.put(
                            MessagesColumns.REACTIONS,
                            MsgPack.encodeToByteArrayEx(
                                ListSerializer(ReactionEntity.serializer()),
                                it
                            )
                        )
                    }, {
                        cv.putNull(
                            MessagesColumns.REACTIONS
                        )
                    })

                    if (cv.size() > 0) {
                        operations.add(
                            ContentProviderOperation.newUpdate(uri)
                                .withValues(cv)
                                .withSelection(
                                    MessagesColumns.PEER_ID + " = ? AND " + MessagesColumns.CONVERSATION_MESSAGE_ID + " = ?",
                                    arrayOf(patch.peerId.toString(), patch.messageId.toString())
                                )
                                .build()
                        )
                    }
                }

                val cv = ContentValues()
                patch.deletion.requireNonNull {
                    cv.put(MessagesColumns.DELETED, it.deleted)
                    cv.put(MessagesColumns.DELETED_FOR_ALL, it.deletedForAll)
                }
                patch.important.requireNonNull {
                    cv.put(MessagesColumns.IMPORTANT, it.important)
                }
                if (cv.size() == 0) continue
                operations.add(
                    ContentProviderOperation.newUpdate(uri)
                        .withValues(cv)
                        .withSelection(
                            MessagesColumns._ID + " = ?",
                            arrayOf(patch.messageId.toString())
                        )
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun getMessageStatus(accountId: Long, dbid: Int): Flow<Int> {
        return flow {
            val cursor = contentResolver.query(
                getMessageContentUriFor(accountId),
                arrayOf(MessagesColumns.STATUS),
                MessagesColumns.FULL_ID + " = ?",
                arrayOf(dbid.toString()),
                null
            )
            var result: Int? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    result = cursor.getInt(MessagesColumns.STATUS)
                }
                cursor.close()
            }
            if (result == null) {
                throw RecordNotFoundException("Message with id $dbid not found")
            }
            emit(result)
        }
    }

    private fun findDraftMessageId(accountId: Long, peerId: Long): Int? {
        val columns = arrayOf(MessagesColumns._ID)
        val uri = getMessageContentUriFor(accountId)
        val cursor = context.contentResolver.query(
            uri,
            columns,
            MessagesColumns.PEER_ID + " = ? AND " + MessagesColumns.STATUS + " = ?",
            arrayOf(peerId.toString(), MessageStatus.EDITING.toString()),
            null
        )
        var id: Int? = null
        if (cursor != null) {
            if (cursor.moveToNext()) {
                id = cursor.getInt(MessagesColumns._ID)
            }
            cursor.close()
        }
        return id
    }

    override fun changeMessageStatus(
        accountId: Long,
        messageId: Int,
        @MessageStatus status: Int,
        vkid: Int?,
        cmid: Int?
    ): Flow<Boolean> {
        return flow {
            val contentValues = ContentValues()
            contentValues.put(MessagesColumns.STATUS, status)
            if (vkid != null) {
                contentValues.put(MessagesColumns._ID, vkid)
            }
            if (cmid != null) {
                contentValues.put(MessagesColumns.CONVERSATION_MESSAGE_ID, cmid)
            }
            val uri = getMessageContentUriFor(accountId)
            val count = context.contentResolver.update(
                uri, contentValues,
                MessagesColumns._ID + " = ?", arrayOf(messageId.toString())
            )
            if (count > 0) {
                emit(true)
            } else {
                throw NotFoundException()
            }
        }
    }

    override fun deleteMessage(accountId: Long, messageId: Int): Flow<Boolean> {
        require(messageId != 0) { "Invalid message id: $messageId" }
        return flow {
            val uri = getMessageContentUriFor(accountId)
            val count = context.contentResolver.delete(
                uri,
                MessagesColumns._ID + " = ?",
                arrayOf(messageId.toString())
            )
            emit(count > 0)
        }
    }

    override fun deleteMessages(accountId: Long, ids: Collection<Int>): Flow<Boolean> {
        return flow {
            val copy: Set<Int> = HashSet(ids)
            val uri = getMessageContentUriFor(accountId)
            val where = MessagesColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val count = context.contentResolver.delete(uri, where, null)
            emit(count > 0)
        }
    }

    override fun changeMessagesStatus(
        accountId: Long,
        ids: Collection<Int>,
        @MessageStatus status: Int
    ): Flow<Boolean> {
        return flow {
            val copy: Set<Int> = HashSet(ids)
            val contentValues = ContentValues()
            contentValues.put(MessagesColumns.STATUS, status)
            val uri = getMessageContentUriFor(accountId)
            val where = MessagesColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val count = context.contentResolver.update(
                uri, contentValues,
                where, null
            )
            if (count > 0) {
                emit(true)
            } else {
                throw NotFoundException()
            }
        }
    }

    override fun getMissingMessages(accountId: Long, ids: Collection<Int>): Flow<List<Int>> {
        return flow {
            val copy: MutableSet<Int> = HashSet(ids)
            val uri = getMessageContentUriFor(accountId)
            val projection = arrayOf(MessagesColumns._ID)
            val where = MessagesColumns.FULL_ID + " IN(" + join(",", copy) + ")"
            val cursor = contentResolver.query(uri, projection, where, null, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(MessagesColumns._ID)
                    copy.remove(id)
                }
                cursor.close()
            }
            emit(ArrayList(copy))
        }
    }

    private suspend fun getForwardMessages(
        accountId: Long,
        attachTo: Int,
        withAttachments: Boolean,
        cancelable: Cancelable
    ): List<MessageDboEntity> {
        val uri = getMessageContentUriFor(accountId)
        val where = MessagesColumns.ATTACH_TO + " = ?"
        val args = arrayOf(attachTo.toString())
        val cursor =
            contentResolver.query(uri, null, where, args, MessagesColumns.FULL_ID + " DESC")
        val dbos: MutableList<MessageDboEntity> = ArrayList(safeCountOf(cursor))
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (cancelable.canceled()) {
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
        accountId: Long,
        ids: List<Int>,
        withAttachments: Boolean,
        withForwardMessages: Boolean
    ): Flow<List<MessageDboEntity>> {
        return flow {
            val uri = getMessageContentUriFor(accountId)
            val where: String
            val args: Array<String>?
            if (ids.size == 1) {
                where = MessagesColumns._ID + " = ?"
                args = arrayOf(ids[0].toString())
            } else {
                where = MessagesColumns.FULL_ID + " IN (" + join(",", ids) + ")"
                args = null
            }
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            val cancelable = object : Cancelable {
                override suspend fun canceled(): Boolean = !isActive()
            }
            val dbos = ArrayList<MessageDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    val dbo = fullMapDbo(
                        accountId,
                        cursor,
                        withAttachments,
                        withForwardMessages,
                        cancelable
                    )
                    val position = dbos.size - cursor.position
                    dbos.add(position, dbo)
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun findFirstUnsentMessage(
        accountIds: Collection<Long>,
        withAttachments: Boolean,
        withForwardMessages: Boolean
    ): Flow<Optional<Pair<Long, MessageDboEntity>>> {
        return flow {
            val where = MessagesColumns.STATUS + " = ?"
            val args = arrayOf(MessageStatus.QUEUE.toString())
            val orderBy = MessagesColumns._ID + " ASC LIMIT 1"
            for (accountId in accountIds) {
                if (!isActive()) {
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
                            withAttachments,
                            withForwardMessages,
                            object : Cancelable {
                                override suspend fun canceled(): Boolean = !isActive()
                            })
                    }
                    cursor.close()
                }
                if (entity != null) {
                    emit(wrap(create(accountId, entity)))
                    return@flow
                }
            }
            emit(empty())
        }
    }

    override fun notifyMessageHasAttachments(accountId: Long, messageId: Int): Flow<Boolean> {
        return flow {
            val cv = ContentValues()
            cv.put(MessagesColumns.HAS_ATTACHMENTS, true)
            val uri = getMessageContentUriFor(accountId)
            val where = MessagesColumns._ID + " = ?"
            val args = arrayOf(messageId.toString())
            contentResolver.update(uri, cv, where, args)
            emit(true)
        }
    }

    override fun getForwardMessageIds(
        accountId: Long,
        attachTo: Int,
        pair: Long
    ): Flow<Pair<Boolean, List<Int>>> {
        return flow {
            val uri = getMessageContentUriFor(accountId)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MessagesColumns.ORIGINAL_ID, MessagesColumns.PEER_ID),
                MessagesColumns.ATTACH_TO + " = ?",
                arrayOf(attachTo.toString()),
                MessagesColumns.FULL_ID + " DESC"
            )
            val ids = ArrayList<Int>(safeCountOf(cursor))
            var from_peer: Long? = null
            var isFirst = true
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    if (isFirst) {
                        isFirst = false
                        from_peer =
                            cursor.getLong(MessagesColumns.PEER_ID)
                    }
                    ids.add(cursor.getInt(MessagesColumns.ORIGINAL_ID))
                }
                cursor.close()
            }
            emit(Pair(ids.size == 1 && pair == from_peer, ids))
        }
    }

    companion object {
        private const val ORDER_BY = MessagesColumns.FULL_STATUS + ", " + MessagesColumns.FULL_ID
        fun appendDboOperation(
            accountId: Long,
            dbo: MessageDboEntity,
            target: MutableList<ContentProviderOperation>,
            attachToId: Int?,
            attachToIndex: Int?
        ): Int {
            val cv = ContentValues()
            if (attachToId != null) {
                // если есть ID сообщения, к которому прикреплено dbo
                cv.put(MessagesColumns.ATTACH_TO, attachToId)
            } else if (attachToIndex == null) {
                // если сообщение не прикреплено к другому
                cv.put(MessagesColumns._ID, dbo.id)
                cv.put(MessagesColumns.ATTACH_TO, MessagesColumns.DONT_ATTACH)
            }
            cv.put(MessagesColumns.PEER_ID, dbo.peerId)
            cv.put(MessagesColumns.FROM_ID, dbo.fromId)
            cv.put(MessagesColumns.DATE, dbo.date)
            //cv.put(MessageColumns.READ_STATE, dbo.isRead());
            cv.put(MessagesColumns.OUT, dbo.isOut)
            //cv.put(MessageColumns.TITLE, dbo.getTitle());
            cv.put(MessagesColumns.TEXT, dbo.text)
            cv.put(MessagesColumns.ENCRYPTED, dbo.isEncrypted)
            cv.put(MessagesColumns.IMPORTANT, dbo.isImportant)
            cv.put(MessagesColumns.DELETED, dbo.isDeleted)
            cv.put(MessagesColumns.FORWARD_COUNT, dbo.forwardCount)
            cv.put(MessagesColumns.HAS_ATTACHMENTS, dbo.isHasAttachments)
            cv.put(MessagesColumns.STATUS, dbo.status)
            cv.put(MessagesColumns.ORIGINAL_ID, dbo.originalId)
            cv.put(MessagesColumns.ACTION, dbo.action)
            cv.put(MessagesColumns.ACTION_MID, dbo.actionMemberId)
            cv.put(MessagesColumns.ACTION_EMAIL, dbo.actionEmail)
            cv.put(MessagesColumns.ACTION_TEXT, dbo.actionText)
            cv.put(MessagesColumns.PHOTO_50, dbo.photo50)
            cv.put(MessagesColumns.PHOTO_100, dbo.photo100)
            cv.put(MessagesColumns.PHOTO_200, dbo.photo200)
            cv.put(MessagesColumns.RANDOM_ID, dbo.randomId)
            dbo.extras.ifNonNull({
                cv.put(
                    MessagesColumns.EXTRAS,
                    MsgPack.encodeToByteArrayEx(
                        MapSerializer(Int.serializer(), String.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(
                    MessagesColumns.EXTRAS
                )
            })

            dbo.keyboard.ifNonNull({
                cv.put(
                    MessagesColumns.KEYBOARD,
                    MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    MessagesColumns.KEYBOARD
                )
            })
            dbo.reactions.ifNonNull({
                cv.put(
                    MessagesColumns.REACTIONS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(ReactionEntity.serializer()), it)
                )
            }, {
                cv.putNull(
                    MessagesColumns.REACTIONS
                )
            })
            cv.put(MessagesColumns.UPDATE_TIME, dbo.updateTime)
            cv.put(MessagesColumns.CONVERSATION_MESSAGE_ID, dbo.conversation_message_id)
            cv.put(MessagesColumns.REACTION_ID, dbo.reaction_id)
            cv.put(MessagesColumns.PAYLOAD, dbo.payload)
            val uri = getMessageContentUriFor(accountId)
            val builder = ContentProviderOperation.newInsert(uri)
                .withValues(cv)

            // если сообщение прикреплено к другому, но его ID на данный момент неизвестен
            if (attachToId == null && attachToIndex != null) {
                builder.withValueBackReference(MessagesColumns.ATTACH_TO, attachToIndex)
            }
            val index = addToListAndReturnIndex(target, builder.build())
            if (dbo.isHasAttachments) {
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
                cursor.getInt(MessagesColumns.STATUS)
            @ChatAction val action =
                cursor.getInt(MessagesColumns.ACTION)
            val id = cursor.getInt(MessagesColumns._ID)
            val peerId = cursor.getLong(MessagesColumns.PEER_ID)
            val fromId = cursor.getLong(MessagesColumns.FROM_ID)
            var extras: Map<Int, String>? = null
            var keyboard: KeyboardEntity? = null
            var reactions: List<ReactionEntity>? = null
            val extrasText = cursor.getBlob(MessagesColumns.EXTRAS)
            if (extrasText.nonNullNoEmpty()) {
                extras = MsgPack.decodeFromByteArrayEx(
                    MapSerializer(
                        Int.serializer(),
                        String.serializer()
                    ), extrasText
                )
            }
            val keyboardText =
                cursor.getBlob(MessagesColumns.KEYBOARD)
            if (keyboardText.nonNullNoEmpty()) {
                keyboard = MsgPack.decodeFromByteArrayEx(KeyboardEntity.serializer(), keyboardText)
            }

            val reactionText =
                cursor.getBlob(MessagesColumns.REACTIONS)
            if (reactionText.nonNullNoEmpty()) {
                reactions = MsgPack.decodeFromByteArrayEx(
                    ListSerializer(ReactionEntity.serializer()),
                    reactionText
                )
            }
            return MessageDboEntity().set(id, peerId, fromId)
                .setEncrypted(cursor.getBoolean(MessagesColumns.ENCRYPTED))
                .setStatus(status)
                .setAction(action)
                .setExtras(extras)
                .setText(cursor.getString(MessagesColumns.TEXT)) //.setRead(cursor.getBoolean(MessageColumns.READ_STATE))
                .setOut(cursor.getBoolean(MessagesColumns.OUT))
                .setStatus(status)
                .setDate(cursor.getLong(MessagesColumns.DATE))
                .setHasAttachments(cursor.getBoolean(MessagesColumns.HAS_ATTACHMENTS))
                .setForwardCount(cursor.getInt(MessagesColumns.FORWARD_COUNT))
                .setDeleted(cursor.getBoolean(MessagesColumns.DELETED))
                .setDeletedForAll(cursor.getBoolean(MessagesColumns.DELETED_FOR_ALL)) //.setTitle(cursor.getString(MessageColumns.TITLE))
                .setOriginalId(cursor.getInt(MessagesColumns.ORIGINAL_ID))
                .setImportant(cursor.getBoolean(MessagesColumns.IMPORTANT))
                .setAction(action)
                .setActionMemberId(cursor.getLong(MessagesColumns.ACTION_MID))
                .setActionEmail(cursor.getString(MessagesColumns.ACTION_EMAIL))
                .setActionText(cursor.getString(MessagesColumns.ACTION_TEXT))
                .setPhoto50(cursor.getString(MessagesColumns.PHOTO_50))
                .setPhoto100(cursor.getString(MessagesColumns.PHOTO_100))
                .setPhoto200(cursor.getString(MessagesColumns.PHOTO_200))
                .setRandomId(cursor.getLong(MessagesColumns.RANDOM_ID))
                .setUpdateTime(cursor.getLong(MessagesColumns.UPDATE_TIME))
                .setConversationMessageId(cursor.getInt(MessagesColumns.CONVERSATION_MESSAGE_ID))
                .setReactionId(cursor.getInt(MessagesColumns.REACTION_ID))
                .setPayload(cursor.getString(MessagesColumns.PAYLOAD))
                .setKeyboard(keyboard)
                .setReactions(reactions)
        }
    }
}