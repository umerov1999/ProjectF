package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.MessageEditEntity
import dev.ragnarok.fenrir.db.model.MessagePatch
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity
import dev.ragnarok.fenrir.model.DraftMessage
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.model.criteria.MessagesCriteria
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IMessagesStorage : IStorage {
    fun insertPeerDbos(
        accountId: Long,
        peerId: Long,
        dbos: List<MessageDboEntity>,
        clearHistory: Boolean
    ): Flow<Boolean>

    fun insert(accountId: Long, dbos: List<MessageDboEntity>): Flow<IntArray>
    fun getByCriteria(
        criteria: MessagesCriteria,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Flow<List<MessageDboEntity>>

    fun insert(accountId: Long, peerId: Long, patch: MessageEditEntity): Flow<Int>
    fun applyPatch(accountId: Long, messageId: Int, patch: MessageEditEntity): Flow<Int>

    @CheckResult
    fun findDraftMessage(accountId: Long, peerId: Long): Flow<DraftMessage?>

    @CheckResult
    fun saveDraftMessageBody(accountId: Long, peerId: Long, text: String?): Flow<Int>

    //@CheckResult
    //Maybe<Integer> getDraftMessageId(int accountId, int peerId);
    fun getMessageStatus(accountId: Long, dbid: Int): Flow<Int>
    fun applyPatches(accountId: Long, patches: Collection<MessagePatch>): Flow<Boolean>

    @CheckResult
    fun changeMessageStatus(
        accountId: Long,
        messageId: Int,
        @MessageStatus status: Int,
        vkid: Int?,
        cmid: Int?
    ): Flow<Boolean>

    @CheckResult
    fun changeMessagesStatus(
        accountId: Long,
        ids: Collection<Int>,
        @MessageStatus status: Int
    ): Flow<Boolean>

    //@CheckResult
    //Completable updateMessageFlag(int accountId, int messageId, Collection<Pair<Integer, Boolean>> values);
    @CheckResult
    fun deleteMessage(accountId: Long, messageId: Int): Flow<Boolean>
    fun findLastSentMessageIdForPeer(accountId: Long, peerId: Long): Flow<Optional<Int>>
    fun findMessagesByIds(
        accountId: Long,
        ids: List<Int>,
        withAttachments: Boolean,
        withForwardMessages: Boolean
    ): Flow<List<MessageDboEntity>>

    fun findFirstUnsentMessage(
        accountIds: Collection<Long>,
        withAttachments: Boolean,
        withForwardMessages: Boolean
    ): Flow<Optional<Pair<Long, MessageDboEntity>>>

    fun notifyMessageHasAttachments(accountId: Long, messageId: Int): Flow<Boolean>

    ///**
    // * Получить список сообщений, которые "приаттаччены" к сообщению с идентификатором attachTo
    // *
    // * @param accountId          идентификатор аккаунта
    // * @param attachTo           идентификатор сообщения
    // * @param includeFwd         если true - рекурсивно загрузить всю иерархию сообщений (вложенные во вложенных и т.д.)
    // * @param includeAttachments - если true - включить вложения к пересланным сообщениям
    // * @param forceAttachments   если true - то алгоритм проигнорирует значение в HAS_ATTACHMENTS
    // *                           и в любом случае будет делать выборку из таблицы вложений
    // * @return список сообщений
    // */
    //@CheckResult
    //Single<List<Message>> getForwardMessages(int accountId, int attachTo, boolean includeFwd, boolean includeAttachments, boolean forceAttachments);
    @CheckResult
    fun getForwardMessageIds(
        accountId: Long,
        attachTo: Int,
        pair: Long
    ): Flow<Pair<Boolean, List<Int>>>

    //Observable<MessageUpdate> observeMessageUpdates();
    fun getMissingMessages(accountId: Long, ids: Collection<Int>): Flow<List<Int>>

    @CheckResult
    fun deleteMessages(accountId: Long, ids: Collection<Int>): Flow<Boolean>
}