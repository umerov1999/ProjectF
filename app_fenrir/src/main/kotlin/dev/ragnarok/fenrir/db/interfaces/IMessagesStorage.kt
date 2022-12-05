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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface IMessagesStorage : IStorage {
    fun insertPeerDbos(
        accountId: Int,
        peerId: Int,
        dbos: List<MessageDboEntity>,
        clearHistory: Boolean
    ): Completable

    fun insert(accountId: Int, dbos: List<MessageDboEntity>): Single<IntArray>
    fun getByCriteria(
        criteria: MessagesCriteria,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<List<MessageDboEntity>>

    fun insert(accountId: Int, peerId: Int, patch: MessageEditEntity): Single<Int>
    fun applyPatch(accountId: Int, messageId: Int, patch: MessageEditEntity): Single<Int>

    @CheckResult
    fun findDraftMessage(accountId: Int, peerId: Int): Maybe<DraftMessage>

    @CheckResult
    fun saveDraftMessageBody(acocuntId: Int, peerId: Int, body: String?): Single<Int>

    //@CheckResult
    //Maybe<Integer> getDraftMessageId(int accountId, int peerId);
    fun getMessageStatus(accountId: Int, dbid: Int): Single<Int>
    fun applyPatches(accountId: Int, patches: Collection<MessagePatch>): Completable

    @CheckResult
    fun changeMessageStatus(
        accountId: Int,
        messageId: Int,
        @MessageStatus status: Int,
        vkid: Int?
    ): Completable

    @CheckResult
    fun changeMessagesStatus(
        accountId: Int,
        ids: Collection<Int>,
        @MessageStatus status: Int
    ): Completable

    //@CheckResult
    //Completable updateMessageFlag(int accountId, int messageId, Collection<Pair<Integer, Boolean>> values);
    @CheckResult
    fun deleteMessage(accountId: Int, messageId: Int): Single<Boolean>
    fun findLastSentMessageIdForPeer(accounId: Int, peerId: Int): Single<Optional<Int>>
    fun findMessagesByIds(
        accountId: Int,
        ids: List<Int>,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<List<MessageDboEntity>>

    fun findFirstUnsentMessage(
        accountIds: Collection<Int>,
        withAtatchments: Boolean,
        withForwardMessages: Boolean
    ): Single<Optional<Pair<Int, MessageDboEntity>>>

    fun notifyMessageHasAttachments(accountId: Int, messageId: Int): Completable

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
        accountId: Int,
        attachTo: Int,
        pair: Int
    ): Single<Pair<Boolean, List<Int>>>

    //Observable<MessageUpdate> observeMessageUpdates();
    fun getMissingMessages(accountId: Int, ids: Collection<Int>): Single<List<Int>>

    @CheckResult
    fun deleteMessages(accountId: Int, ids: Collection<Int>): Single<Boolean>
}