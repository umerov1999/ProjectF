package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.longpoll.*
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IMessagesRepository {
    fun observeMessagesSendErrors(): Flowable<Throwable>
    fun handleFlagsUpdates(
        accountId: Int,
        setUpdates: List<MessageFlagsSetUpdate>?,
        resetUpdates: List<MessageFlagsResetUpdate>?
    ): Completable

    fun handleReadUpdates(
        accountId: Int,
        setUpdates: List<OutputMessagesSetReadUpdate>?,
        resetUpdates: List<InputMessagesSetReadUpdate>?
    ): Completable

    fun handleUnreadBadgeUpdates(
        accountId: Int,
        updates: List<BadgeCountChangeUpdate>?
    ): Completable

    fun handleWriteUpdates(accountId: Int, updates: List<WriteTextInDialogUpdate>?): Completable
    fun observeSentMessages(): Flowable<SentMsg>
    fun observePeerUpdates(): Flowable<List<PeerUpdate>>
    fun observeMessageUpdates(): Flowable<List<MessageUpdate>>
    fun observeTextWrite(): Flowable<List<WriteText>>
    fun observePeerDeleting(): Flowable<PeerDeleting>
    fun getConversationSingle(accountId: Int, peerId: Int, mode: Mode): Single<Conversation>
    fun getConversation(accountId: Int, peerId: Int, mode: Mode): Flowable<Conversation>
    fun edit(
        accountId: Int,
        message: Message,
        body: String?,
        attachments: List<AbsModel>,
        keepForwardMessages: Boolean
    ): Single<Message>

    fun runSendingQueue()

    /**
     * Получить все закэшированные сообщения в локальной БД
     *
     * @param accountId идентификатор аккаунта
     * @param peerId    идентификатор диалога
     * @return полученные сообщения
     */
    fun getCachedPeerMessages(accountId: Int, peerId: Int): Single<List<Message>>

    /**
     * Получить все закэшированные диалоги в локальной БД
     *
     * @param accountId идентификатор аккаунта
     * @return диалоги
     */
    fun getCachedDialogs(accountId: Int): Single<List<Dialog>>
    fun getMessagesFromLocalJSon(
        accountId: Int,
        context: Context
    ): Single<Pair<Peer, List<Message>>>

    /**
     * Сохранить в локальную БД сообщения
     *
     * @param accountId идентификатор аккаунта
     * @param messages  сообщения
     * @return Completable
     */
    fun insertMessages(accountId: Int, messages: List<VKApiMessage>): Completable

    /**
     * Получить актуальный список сообщений для конкретного диалога
     *
     * @param accountId      идентификатор аккаунта
     * @param peerId         идентификатор диалога
     * @param count          количество сообщений
     * @param offset         сдвиг (может быть как положительным, так и отрицательным)
     * @param startMessageId идентификатор сообщения, после которого необходимо получить (если null - от последнего)
     * @param cacheData      если true - сохранить полученные данные в кэш
     * @return полученные сообщения
     */
    fun getPeerMessages(
        accountId: Int,
        peerId: Int,
        count: Int,
        offset: Int?,
        startMessageId: Int?,
        cacheData: Boolean,
        rev: Boolean
    ): Single<List<Message>>

    fun getJsonHistory(
        accountId: Int,
        offset: Int?,
        count: Int?,
        peerId: Int
    ): Single<List<String>>

    fun getImportantMessages(
        accountId: Int,
        count: Int,
        offset: Int?,
        startMessageId: Int?
    ): Single<List<Message>>

    fun getDialogs(accountId: Int, count: Int, startMessageId: Int?): Single<List<Dialog>>
    fun insertDialog(accountId: Int, dialog: Dialog): Completable
    fun findCachedMessages(accountId: Int, ids: List<Int>): Single<List<Message>>
    fun put(builder: SaveMessageBuilder): Single<Message>
    fun sendUnsentMessage(accountIds: Collection<Int>): Single<SentMsg>
    fun enqueueAgain(accountId: Int, messageId: Int): Completable
    fun enqueueAgainList(accountId: Int, ids: Collection<Int>): Completable

    /**
     * Поиск диалогов
     *
     * @param accountId идентификатор аккаунта
     * @param count     количество результатов
     * @param q         строка поиска
     * @return список найденных диалогов
     */
    fun searchConversations(accountId: Int, count: Int, q: String?): Single<List<Conversation>>
    fun updateDialogKeyboard(accountId: Int, peerId: Int, keyboard: Keyboard?): Completable
    fun searchMessages(
        accountId: Int,
        peerId: Int?,
        count: Int,
        offset: Int,
        q: String?
    ): Single<List<Message>>

    fun getChatUsers(accountId: Int, chatId: Int): Single<List<AppChatUser>>
    fun removeChatMember(accountId: Int, chatId: Int, userId: Int): Completable
    fun addChatUsers(accountId: Int, chatId: Int, users: List<User>): Single<List<AppChatUser>>
    fun deleteChatPhoto(accountId: Int, chatId: Int): Completable
    fun deleteDialog(accountId: Int, peedId: Int): Completable
    fun deleteMessages(
        accountId: Int,
        peerId: Int,
        ids: Collection<Int>,
        forAll: Boolean,
        spam: Boolean
    ): Completable

    fun restoreMessage(accountId: Int, peerId: Int, messageId: Int): Completable
    fun editChat(accountId: Int, chatId: Int, title: String?): Completable
    fun createGroupChat(accountId: Int, users: Collection<Int>, title: String?): Single<Int>
    fun recogniseAudioMessage(
        accountId: Int,
        message_id: Int?,
        audio_message_id: String?
    ): Single<Int>

    fun setMemberRole(accountId: Int, chat_id: Int, member_id: Int, isAdmin: Boolean): Completable
    fun markAsRead(accountId: Int, peerId: Int, toId: Int): Completable
    fun markAsImportant(
        accountId: Int,
        peerId: Int,
        ids: Collection<Int>,
        important: Int?
    ): Completable

    fun pin(accountId: Int, peerId: Int, message: Message?): Completable
    fun pinUnPinConversation(accountId: Int, peerId: Int, peen: Boolean): Completable
    fun markAsListened(accountId: Int, message_id: Int): Completable
}