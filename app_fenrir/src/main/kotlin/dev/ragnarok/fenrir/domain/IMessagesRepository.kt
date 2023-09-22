package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.longpoll.BadgeCountChangeUpdate
import dev.ragnarok.fenrir.api.model.longpoll.InputMessagesSetReadUpdate
import dev.ragnarok.fenrir.api.model.longpoll.MessageFlagsResetUpdate
import dev.ragnarok.fenrir.api.model.longpoll.MessageFlagsSetUpdate
import dev.ragnarok.fenrir.api.model.longpoll.OutputMessagesSetReadUpdate
import dev.ragnarok.fenrir.api.model.longpoll.ReactionMessageChangeUpdate
import dev.ragnarok.fenrir.api.model.longpoll.WriteTextInDialogUpdate
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.Conversation
import dev.ragnarok.fenrir.model.Dialog
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.MessageUpdate
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.PeerDeleting
import dev.ragnarok.fenrir.model.PeerUpdate
import dev.ragnarok.fenrir.model.ReactionAsset
import dev.ragnarok.fenrir.model.SaveMessageBuilder
import dev.ragnarok.fenrir.model.SentMsg
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.WriteText
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IMessagesRepository {
    fun observeMessagesSendErrors(): Flowable<Throwable>
    fun handleFlagsUpdates(
        accountId: Long,
        setUpdates: List<MessageFlagsSetUpdate>?,
        resetUpdates: List<MessageFlagsResetUpdate>?
    ): Completable

    fun handleReadUpdates(
        accountId: Long,
        setUpdates: List<OutputMessagesSetReadUpdate>?,
        resetUpdates: List<InputMessagesSetReadUpdate>?
    ): Completable

    fun handleUnreadBadgeUpdates(
        accountId: Long,
        updates: List<BadgeCountChangeUpdate>?
    ): Completable

    fun handleWriteUpdates(accountId: Long, updates: List<WriteTextInDialogUpdate>?): Completable
    fun handleMessageReactionsChangedUpdates(
        accountId: Long,
        updates: List<ReactionMessageChangeUpdate>?
    ): Completable

    fun observeSentMessages(): Flowable<SentMsg>
    fun observePeerUpdates(): Flowable<List<PeerUpdate>>
    fun observeMessageUpdates(): Flowable<List<MessageUpdate>>
    fun observeTextWrite(): Flowable<List<WriteText>>
    fun observePeerDeleting(): Flowable<PeerDeleting>
    fun getConversationSingle(accountId: Long, peerId: Long, mode: Mode): Single<Conversation>
    fun getConversation(accountId: Long, peerId: Long, mode: Mode): Flowable<Conversation>
    fun edit(
        accountId: Long,
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
    fun getCachedPeerMessages(accountId: Long, peerId: Long): Single<List<Message>>

    /**
     * Получить все закэшированные диалоги в локальной БД
     *
     * @param accountId идентификатор аккаунта
     * @return диалоги
     */
    fun getCachedDialogs(accountId: Long): Single<List<Dialog>>
    fun getMessagesFromLocalJSon(
        accountId: Long,
        context: Context
    ): Single<Pair<Peer, List<Message>>>

    /**
     * Сохранить в локальную БД сообщения
     *
     * @param accountId идентификатор аккаунта
     * @param messages  сообщения
     * @return Completable
     */
    fun insertMessages(accountId: Long, messages: List<VKApiMessage>): Completable

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
        accountId: Long,
        peerId: Long,
        count: Int,
        offset: Int?,
        startMessageId: Int?,
        cacheData: Boolean,
        rev: Boolean
    ): Single<List<Message>>

    fun getJsonHistory(
        accountId: Long,
        offset: Int?,
        count: Int?,
        peerId: Long
    ): Single<List<String>>

    fun getImportantMessages(
        accountId: Long,
        count: Int,
        offset: Int?,
        startMessageId: Int?
    ): Single<List<Message>>

    fun getDialogs(accountId: Long, count: Int, startMessageId: Int?): Single<List<Dialog>>
    fun insertDialog(accountId: Long, dialog: Dialog): Completable
    fun findCachedMessages(accountId: Long, ids: List<Int>): Single<List<Message>>
    fun put(builder: SaveMessageBuilder): Single<Message>
    fun sendUnsentMessage(accountIds: Collection<Long>): Single<SentMsg>
    fun enqueueAgain(accountId: Long, messageId: Int): Completable
    fun enqueueAgainList(accountId: Long, ids: Collection<Int>): Completable

    /**
     * Поиск диалогов
     *
     * @param accountId идентификатор аккаунта
     * @param count     количество результатов
     * @param q         строка поиска
     * @return список найденных диалогов
     */
    fun searchConversations(accountId: Long, count: Int, q: String?): Single<List<Conversation>>
    fun updateDialogKeyboard(accountId: Long, peerId: Long, keyboard: Keyboard?): Completable
    fun searchMessages(
        accountId: Long,
        peerId: Long?,
        count: Int,
        offset: Int,
        q: String?
    ): Single<List<Message>>

    fun getChatUsers(accountId: Long, chatId: Long): Single<List<AppChatUser>>
    fun removeChatMember(accountId: Long, chatId: Long, userId: Long): Completable
    fun addChatUsers(accountId: Long, chatId: Long, users: List<User>): Single<List<AppChatUser>>
    fun deleteChatPhoto(accountId: Long, chatId: Long): Completable
    fun deleteDialog(accountId: Long, peedId: Long): Completable
    fun deleteMessages(
        accountId: Long,
        peerId: Long,
        ids: Collection<Int>,
        forAll: Boolean,
        spam: Boolean
    ): Completable

    fun restoreMessage(accountId: Long, peerId: Long, messageId: Int): Completable
    fun editChat(accountId: Long, chatId: Long, title: String?): Completable
    fun createGroupChat(accountId: Long, users: Collection<Long>, title: String?): Single<Long>
    fun recogniseAudioMessage(
        accountId: Long,
        message_id: Int?,
        audio_message_id: String?
    ): Single<Int>

    fun setMemberRole(
        accountId: Long,
        chat_id: Long,
        member_id: Long,
        isAdmin: Boolean
    ): Completable

    fun markAsRead(accountId: Long, peerId: Long, toId: Int): Completable
    fun markAsImportant(
        accountId: Long,
        peerId: Long,
        ids: Collection<Int>,
        important: Int?
    ): Completable

    fun pin(accountId: Long, peerId: Long, message: Message?): Completable
    fun pinUnPinConversation(accountId: Long, peerId: Long, peen: Boolean): Completable
    fun markAsListened(accountId: Long, message_id: Int): Completable
    fun sendOrDeleteReaction(
        accountId: Long,
        peer_id: Long, cmid: Int, reaction_id: Int?
    ): Completable

    fun getReactionsAssets(accountId: Long): Single<List<ReactionAsset>>
}