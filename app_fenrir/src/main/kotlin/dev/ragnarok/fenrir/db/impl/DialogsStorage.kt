package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.content.edit
import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getDialogsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPeersContentUriFor
import dev.ragnarok.fenrir.db.PeerStateEntity
import dev.ragnarok.fenrir.db.column.DialogsColumns
import dev.ragnarok.fenrir.db.column.DialogsColumns.getCV
import dev.ragnarok.fenrir.db.column.PeersColumns
import dev.ragnarok.fenrir.db.interfaces.IDialogsStorage
import dev.ragnarok.fenrir.db.model.PeerPatch
import dev.ragnarok.fenrir.db.model.entity.DialogDboEntity
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity
import dev.ragnarok.fenrir.db.model.entity.PeerDialogEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.Chat
import dev.ragnarok.fenrir.model.ChatAction
import dev.ragnarok.fenrir.model.criteria.DialogsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyListFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.msgpack.MsgPack

internal class DialogsStorage(base: AppStorages) : AbsStorage(base), IDialogsStorage {
    private val unreadDialogsCounter = createPublishSubject<Pair<Long, Int>>()
    private val preferences: SharedPreferences =
        base.getSharedPreferences("dialogs_prefs", Context.MODE_PRIVATE)

    override fun getUnreadDialogsCount(accountId: Long): Int {
        synchronized(this) { return preferences.getInt(unreadKeyFor(accountId), 0) }
    }

    override fun observeUnreadDialogsCount(): SharedFlow<Pair<Long, Int>> {
        return unreadDialogsCounter
    }

    override fun getDialogs(criteria: DialogsCriteria): Flow<List<DialogDboEntity>> {
        return flow {
            val start = System.currentTimeMillis()
            val uri = getDialogsContentUriFor(criteria.accountId)
            val cursor = context.contentResolver.query(
                uri, null, null,
                null, DialogsColumns.MAJOR_ID + " DESC, " + DialogsColumns.MINOR_ID + " DESC"
            )
            val dbos: MutableList<DialogDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapEntity(cursor))
                }
                cursor.close()
            }
            log("getDialogs", start)
            emit(dbos)
        }
    }

    override fun removePeerWithId(accountId: Long, peerId: Long): Flow<Boolean> {
        return flow {
            val uri = getDialogsContentUriFor(accountId)
            contentResolver.delete(uri, BaseColumns._ID + " = ?", arrayOf(peerId.toString()))
            emit(true)
        }
    }

    override fun insertDialogs(
        accountId: Long,
        dbos: List<DialogDboEntity>,
        clearBefore: Boolean
    ): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val uri = getDialogsContentUriFor(accountId)
            val peersUri = getPeersContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBefore) {
                operations.add(ContentProviderOperation.newDelete(uri).build())
            }
            for (entity in dbos) {
                val peerDialog = entity.toPeerDialog()
                operations.add(
                    ContentProviderOperation.newInsert(uri).withValues(createCv(entity)).build()
                )
                operations.add(
                    ContentProviderOperation
                        .newInsert(peersUri)
                        .withValues(createPeerCv(peerDialog))
                        .build()
                )
                entity.message?.let {
                    MessagesStorage.appendDboOperation(
                        accountId,
                        it,
                        operations,
                        null,
                        null
                    )
                }
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            log(
                "DialogsStorage.insertDialogs",
                start,
                "count: " + dbos.size + ", clearBefore: " + clearBefore
            )
            emit(true)
        }
    }

    private fun createCv(entity: DialogDboEntity): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, entity.peerId)
        cv.put(DialogsColumns.UNREAD, entity.unreadCount)
        cv.put(DialogsColumns.TITLE, entity.title)
        cv.put(DialogsColumns.IN_READ, entity.inRead)
        cv.put(DialogsColumns.OUT_READ, entity.outRead)
        cv.put(DialogsColumns.PHOTO_50, entity.photo50)
        cv.put(DialogsColumns.PHOTO_100, entity.photo100)
        cv.put(DialogsColumns.PHOTO_200, entity.photo200)
        cv.put(DialogsColumns.LAST_MESSAGE_ID, entity.message?.id.orZero())
        cv.put(DialogsColumns.ACL, entity.acl)
        cv.put(DialogsColumns.IS_GROUP_CHANNEL, entity.isGroupChannel)
        cv.put(DialogsColumns.MAJOR_ID, entity.major_id)
        cv.put(DialogsColumns.MINOR_ID, entity.minor_id)
        return cv
    }

    private fun createPeerCv(entity: PeerDialogEntity): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, entity.peerId)
        cv.put(PeersColumns.UNREAD, entity.unreadCount)
        cv.put(PeersColumns.TITLE, entity.title)
        cv.put(PeersColumns.IN_READ, entity.inRead)
        cv.put(PeersColumns.OUT_READ, entity.outRead)
        cv.put(PeersColumns.PHOTO_50, entity.photo50)
        cv.put(PeersColumns.PHOTO_100, entity.photo100)
        cv.put(PeersColumns.PHOTO_200, entity.photo200)
        entity.currentKeyboard.ifNonNull({
            cv.put(
                PeersColumns.KEYBOARD,
                MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
            )
        }, {
            cv.putNull(PeersColumns.KEYBOARD)
        })
        entity.pinned.ifNonNull({
            cv.put(
                PeersColumns.PINNED,
                MsgPack.encodeToByteArrayEx(MessageDboEntity.serializer(), it)
            )
        }, {
            cv.putNull(PeersColumns.PINNED)
        })
        cv.put(PeersColumns.ACL, entity.acl)
        cv.put(PeersColumns.IS_GROUP_CHANNEL, entity.isGroupChannel)
        cv.put(PeersColumns.MAJOR_ID, entity.major_id)
        cv.put(PeersColumns.MINOR_ID, entity.minor_id)
        cv.put(PeersColumns.LAST_MESSAGE_ID, entity.lastMessageId)
        return cv
    }

    override fun savePeerDialog(accountId: Long, entity: PeerDialogEntity): Flow<Boolean> {
        return flow {
            val uri = getPeersContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            operations.add(
                ContentProviderOperation.newInsert(uri).withValues(createPeerCv(entity)).build()
            )
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun updateDialogKeyboard(
        accountId: Long,
        peerId: Long,
        keyboardEntity: KeyboardEntity?
    ): Flow<Boolean> {
        return flow {
            val uri = getPeersContentUriFor(accountId)
            val args = arrayOf(peerId.toString())
            val operations = ArrayList<ContentProviderOperation>(1)
            val cv = ContentValues()
            keyboardEntity.ifNonNull({
                cv.put(
                    PeersColumns.KEYBOARD,
                    MsgPack.encodeToByteArrayEx(KeyboardEntity.serializer(), it)
                )
            }, {
                cv.putNull(PeersColumns.KEYBOARD)
            })
            operations.add(
                ContentProviderOperation.newUpdate(uri)
                    .withSelection(BaseColumns._ID + " = ?", args)
                    .withValues(cv)
                    .build()
            )
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun findPeerStates(
        accountId: Long,
        ids: Collection<Long>
    ): Flow<List<PeerStateEntity>> {
        return if (ids.isEmpty()) {
            emptyListFlow()
        } else flow {
            val projection = arrayOf(
                BaseColumns._ID,
                PeersColumns.UNREAD,
                PeersColumns.IN_READ,
                PeersColumns.OUT_READ,
                PeersColumns.LAST_MESSAGE_ID
            )
            val uri = getPeersContentUriFor(accountId)
            val where = BaseColumns._ID + " IN (" + join(",", ids) + ")"
            val cursor = contentResolver.query(uri, projection, where, null, null)
            val entities: MutableList<PeerStateEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val entity =
                        PeerStateEntity(cursor.getLong(BaseColumns._ID))
                            .setInRead(cursor.getInt(PeersColumns.IN_READ))
                            .setOutRead(cursor.getInt(PeersColumns.OUT_READ))
                            .setLastMessageId(
                                cursor.getInt(PeersColumns.LAST_MESSAGE_ID)
                            )
                            .setUnreadCount(cursor.getInt(PeersColumns.UNREAD))
                    entities.add(entity)
                }
                cursor.close()
            }
            emit(entities)
        }
    }

    override fun findPeerDialog(accountId: Long, peerId: Long): Flow<Optional<PeerDialogEntity>> {
        return flow {
            val projection = arrayOf(
                PeersColumns.UNREAD,
                PeersColumns.TITLE,
                PeersColumns.IN_READ,
                PeersColumns.OUT_READ,
                PeersColumns.PHOTO_50,
                PeersColumns.PHOTO_100,
                PeersColumns.PHOTO_200,
                PeersColumns.KEYBOARD,
                PeersColumns.PINNED,
                PeersColumns.LAST_MESSAGE_ID,
                PeersColumns.ACL,
                PeersColumns.IS_GROUP_CHANNEL,
                PeersColumns.MAJOR_ID,
                PeersColumns.MINOR_ID
            )
            val uri = getPeersContentUriFor(accountId)
            val cursor = contentResolver.query(
                uri, projection,
                PeersColumns.FULL_ID + " = ?", arrayOf(peerId.toString()), null
            )
            var entity: PeerDialogEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val pinJson = cursor.getBlob(PeersColumns.PINNED)
                    val keyboardJson = cursor.getBlob(PeersColumns.KEYBOARD)
                    entity = PeerDialogEntity(peerId)
                        .setUnreadCount(cursor.getInt(PeersColumns.UNREAD))
                        .setTitle(cursor.getString(PeersColumns.TITLE))
                        .setPhoto200(cursor.getString(PeersColumns.PHOTO_200))
                        .setPhoto100(cursor.getString(PeersColumns.PHOTO_100))
                        .setPhoto50(cursor.getString(PeersColumns.PHOTO_50))
                        .setInRead(cursor.getInt(PeersColumns.IN_READ))
                        .setOutRead(cursor.getInt(PeersColumns.OUT_READ))
                        .setPinned(
                            if (pinJson == null) null else MsgPack.decodeFromByteArrayEx(
                                MessageDboEntity.serializer(),
                                pinJson
                            )
                        )
                        .setCurrentKeyboard(
                            if (keyboardJson == null) null else MsgPack.decodeFromByteArrayEx(
                                KeyboardEntity.serializer(),
                                keyboardJson
                            )
                        )
                        .setLastMessageId(cursor.getInt(PeersColumns.LAST_MESSAGE_ID))
                        .setAcl(cursor.getInt(PeersColumns.ACL))
                        .setMajor_id(cursor.getInt(PeersColumns.MAJOR_ID))
                        .setMinor_id(cursor.getInt(PeersColumns.MINOR_ID))
                        .setGroupChannel(cursor.getBoolean(PeersColumns.IS_GROUP_CHANNEL))
                }
                cursor.close()
            }
            emit(wrap(entity))
        }
    }

    override fun setUnreadDialogsCount(accountId: Long, unreadCount: Int) {
        synchronized(this) {
            preferences.edit {
                putInt(unreadKeyFor(accountId), unreadCount)
            }
        }
        unreadDialogsCounter.myEmit(Pair(accountId, unreadCount))
    }

    override fun getMissingGroupChats(
        accountId: Long,
        ids: Collection<Long>
    ): Flow<Collection<Long>> {
        return flow {
            if (ids.isEmpty()) {
                emit(emptyList())
            } else {
                val peerIds: MutableSet<Long> = HashSet(ids)
                val projection = arrayOf(BaseColumns._ID)
                val uri = getDialogsContentUriFor(accountId)
                val cursor = contentResolver.query(
                    uri, projection,
                    DialogsColumns.FULL_ID + " IN (" + join(",", peerIds) + ")", null, null
                )
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val peerId = cursor.getLong(BaseColumns._ID)
                        peerIds.remove(peerId)
                    }
                    cursor.close()
                }
                emit(peerIds)
            }
        }
    }

    override fun insertChats(accountId: Long, chats: List<VKApiChat>): Flow<Boolean> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>(chats.size)
            for (chat in chats) {
                operations.add(
                    ContentProviderOperation
                        .newInsert(getDialogsContentUriFor(accountId))
                        .withValues(getCV(chat))
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun applyPatches(accountId: Long, patches: List<PeerPatch>): Flow<Boolean> {
        return flow {
            val dialogsUri = getDialogsContentUriFor(accountId)
            val peersUri = getPeersContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(patches.size * 2)
            for (patch in patches) {
                val dialogscv = ContentValues()
                val peerscv = ContentValues()
                patch.inRead.requireNonNull {
                    dialogscv.put(DialogsColumns.IN_READ, it.id)
                    peerscv.put(PeersColumns.IN_READ, it.id)
                }
                patch.unread.requireNonNull {
                    dialogscv.put(DialogsColumns.UNREAD, it.count)
                    peerscv.put(PeersColumns.UNREAD, it.count)
                }
                patch.outRead.requireNonNull {
                    dialogscv.put(DialogsColumns.OUT_READ, it.id)
                    peerscv.put(PeersColumns.OUT_READ, it.id)
                }
                patch.lastMessage.requireNonNull {
                    dialogscv.put(DialogsColumns.LAST_MESSAGE_ID, it.id)
                    peerscv.put(PeersColumns.LAST_MESSAGE_ID, it.id)
                    dialogscv.put(DialogsColumns.MINOR_ID, it.id)
                    peerscv.put(PeersColumns.MINOR_ID, it.id)
                }
                patch.pin.requireNonNull {
                    it.pinned.ifNonNull({ dd ->
                        peerscv.put(
                            PeersColumns.PINNED,
                            MsgPack.encodeToByteArrayEx(MessageDboEntity.serializer(), dd)
                        )
                    }, {
                        peerscv.putNull(PeersColumns.PINNED)
                    })
                }
                patch.title.requireNonNull {
                    peerscv.put(PeersColumns.TITLE, it.title)
                    dialogscv.put(DialogsColumns.TITLE, it.title)
                }
                val args = arrayOf(patch.id.toString())
                if (dialogscv.size() > 0) {
                    operations.add(
                        ContentProviderOperation.newUpdate(dialogsUri)
                            .withSelection(BaseColumns._ID + " = ?", args)
                            .withValues(dialogscv)
                            .build()
                    )
                }
                if (peerscv.size() > 0) {
                    operations.add(
                        ContentProviderOperation.newUpdate(peersUri)
                            .withSelection(BaseColumns._ID + " = ?", args)
                            .withValues(peerscv)
                            .build()
                    )
                }
            }
            if (operations.nonNullNoEmpty()) {
                contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            }
            emit(true)
        }
    }

    override fun findChatById(accountId: Long, peerId: Long): Flow<Optional<Chat>> {
        return flow {
            val projection = arrayOf(
                DialogsColumns.TITLE,
                DialogsColumns.PHOTO_200,
                DialogsColumns.PHOTO_100,
                DialogsColumns.PHOTO_50
            )
            val uri = getDialogsContentUriFor(accountId)
            val cursor = contentResolver.query(
                uri, projection,
                DialogsColumns.FULL_ID + " = ?", arrayOf(peerId.toString()), null
            )
            var chat: Chat? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    chat = Chat(peerId)
                    chat.setTitle(cursor.getString(DialogsColumns.TITLE))
                        .setPhoto200(cursor.getString(DialogsColumns.PHOTO_200))
                        .setPhoto100(cursor.getString(DialogsColumns.PHOTO_100))
                        .setPhoto50(cursor.getString(DialogsColumns.PHOTO_50))
                }
                cursor.close()
            }
            emit(wrap(chat))
        }
    }

    private fun mapEntity(cursor: Cursor): DialogDboEntity {
        @ChatAction val action =
            cursor.getInt(DialogsColumns.FOREIGN_MESSAGE_ACTION)
        val encrypted =
            cursor.getBoolean(DialogsColumns.FOREIGN_MESSAGE_ENCRYPTED)
        val messageId = cursor.getInt(DialogsColumns.LAST_MESSAGE_ID)
        val peerId = cursor.getLong(BaseColumns._ID)
        val fromId =
            cursor.getLong(DialogsColumns.FOREIGN_MESSAGE_FROM_ID)
        val message = MessageDboEntity().set(messageId, peerId, fromId)
            .setText(cursor.getString(DialogsColumns.FOREIGN_MESSAGE_TEXT))
            .setDate(cursor.getLong(DialogsColumns.FOREIGN_MESSAGE_DATE))
            .setOut(cursor.getBoolean(DialogsColumns.FOREIGN_MESSAGE_OUT))
            .setHasAttachments(cursor.getBoolean(DialogsColumns.FOREIGN_MESSAGE_HAS_ATTACHMENTS))
            .setForwardCount(cursor.getInt(DialogsColumns.FOREIGN_MESSAGE_FWD_COUNT))
            .setConversationMessageId(cursor.getInt(DialogsColumns.FOREIGN_MESSAGE_CMID))
            .setAction(action)
            .setEncrypted(encrypted)
        return DialogDboEntity(peerId)
            .setMessage(message)
            .setInRead(cursor.getInt(DialogsColumns.IN_READ))
            .setOutRead(cursor.getInt(DialogsColumns.OUT_READ))
            .setTitle(cursor.getString(DialogsColumns.TITLE))
            .setPhoto50(cursor.getString(DialogsColumns.PHOTO_50))
            .setPhoto100(cursor.getString(DialogsColumns.PHOTO_100))
            .setPhoto200(cursor.getString(DialogsColumns.PHOTO_200))
            .setUnreadCount(cursor.getInt(DialogsColumns.UNREAD))
            .setLastMessageId(cursor.getInt(DialogsColumns.LAST_MESSAGE_ID))
            .setAcl(cursor.getInt(DialogsColumns.ACL))
            .setMajor_id(cursor.getInt(DialogsColumns.MAJOR_ID))
            .setMinor_id(cursor.getInt(DialogsColumns.MINOR_ID))
            .setGroupChannel(cursor.getBoolean(DialogsColumns.IS_GROUP_CHANNEL))
    }

    companion object {
        internal fun unreadKeyFor(accountId: Long): String {
            return "unread$accountId"
        }
    }

}
