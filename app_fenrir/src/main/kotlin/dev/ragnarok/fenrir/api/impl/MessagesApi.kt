package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IMessagesApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiJsonString
import dev.ragnarok.fenrir.api.model.VKApiLongpollServer
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.AttachmentsHistoryResponse
import dev.ragnarok.fenrir.api.model.response.ConversationDeleteResult
import dev.ragnarok.fenrir.api.model.response.ConversationMembersResponse
import dev.ragnarok.fenrir.api.model.response.ConversationsResponse
import dev.ragnarok.fenrir.api.model.response.DialogsResponse
import dev.ragnarok.fenrir.api.model.response.ItemsProfilesGroupsResponse
import dev.ragnarok.fenrir.api.model.response.LongpollHistoryResponse
import dev.ragnarok.fenrir.api.model.response.MessageHistoryResponse
import dev.ragnarok.fenrir.api.model.response.MessageImportantResponse
import dev.ragnarok.fenrir.api.services.IMessageService
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

internal class MessagesApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IMessagesApi {
    private fun serviceRx(vararg tokenTypes: Int): Single<IMessageService> {
        return provideService(IMessageService(), *tokenTypes)
    }

    override fun edit(
        peerId: Long,
        messageId: Int,
        message: String?,
        attachments: List<IAttachmentToken>?,
        keepFwd: Boolean,
        keepSnippets: Boolean?
    ): Completable {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service
                    .editMessage(
                        peerId,
                        messageId,
                        message,
                        atts,
                        integerFromBoolean(keepFwd),
                        integerFromBoolean(keepSnippets)
                    )
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun removeChatMember(chatId: Long, memberId: Long): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .removeChatUser(chatId, memberId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun deleteChatPhoto(chatId: Long): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .deleteChatPhoto(chatId)
                    .map(extractResponseWithErrorHandling())
                    .map { response -> response.message_id != 0 }
            }
    }

    override fun addChatUser(chatId: Long, userId: Long): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .addChatUser(chatId, userId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun getChat(
        chatId: Long?,
        chatIds: Collection<Long>?,
        fields: String?,
        name_case: String?
    ): Single<List<VKApiChat>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getChat(chatId, join(chatIds, ","), fields, name_case)
                    .map(extractResponseWithErrorHandling())
                    .map { response -> listEmptyIfNull(response.chats) }
            }
    }

    override fun getConversationMembers(
        peer_id: Long?,
        fields: String?
    ): Single<ConversationMembersResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getConversationMembers(peer_id, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun editChat(chatId: Long, title: String?): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .editChat(chatId, title)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun createChat(userIds: Collection<Long>, title: String?): Single<Long> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .createChat(join(userIds, ","), title)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun recogniseAudioMessage(message_id: Int?, audio_message_id: String?): Single<Int> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .recogniseAudioMessage(message_id, audio_message_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setMemberRole(peer_id: Long?, member_id: Long?, role: String?): Single<Int> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .setMemberRole(peer_id, member_id, role)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteDialog(peerId: Long): Single<ConversationDeleteResult> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .deleteDialog(peerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun restore(messageId: Int): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .restore(messageId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun delete(
        messageIds: Collection<Int>,
        deleteForAll: Boolean?,
        spam: Boolean?
    ): Single<Map<String, Int>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .delete(
                        join(messageIds, ","),
                        integerFromBoolean(deleteForAll),
                        integerFromBoolean(spam)
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun markAsRead(peerId: Long?, startMessageId: Int?): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .markAsRead(peerId, startMessageId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun markAsImportant(messageIds: Collection<Int>, important: Int?): Single<List<Int>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .markAsImportant(join(messageIds, ","), important)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setActivity(peerId: Long, typing: Boolean): Single<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .setActivity(peerId, if (typing) "typing" else null)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun search(
        query: String?,
        peerId: Long?,
        date: Long?,
        previewLength: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiMessage>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .search(query, peerId, date, previewLength, offset, count)
                    .map(extractResponseWithErrorHandling())
            } /*.map(response -> {
                            fixMessageList(response.items);
                            return response;
                        })*/

    }

    override fun getLongPollHistory(
        ts: Long?,
        pts: Long?,
        previewLength: Int?,
        onlines: Boolean?,
        fields: String?,
        eventsLimit: Int?,
        msgsLimit: Int?,
        max_msg_id: Int?
    ): Single<LongpollHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getLongPollHistory(
                        ts, pts, previewLength, integerFromBoolean(onlines), fields,
                        eventsLimit, msgsLimit, max_msg_id
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getHistoryAttachments(
        peerId: Long,
        mediaType: String?,
        startFrom: String?,
        photoSizes: Int?,
        count: Int?,
        fields: String?
    ): Single<AttachmentsHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getHistoryAttachments(peerId, mediaType, startFrom, count, photoSizes, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun send(
        randomId: Long?, peerId: Long?, domain: String?, message: String?,
        latitude: Double?, longitude: Double?, attachments: Collection<IAttachmentToken>?,
        forwardMessages: Collection<Int>?, stickerId: Int?, payload: String?, reply_to: Int?
    ): Single<Int> {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .send(
                        randomId, peerId, domain, message, latitude, longitude, atts,
                        join(forwardMessages, ","), stickerId, payload, reply_to
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getConversations(
        peers: List<Long>,
        extended: Boolean?,
        fields: String?
    ): Single<ItemsProfilesGroupsResponse<VKApiConversation>> {
        val ids = join(peers, ",") { obj: Any -> obj.toString() }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getConversationsById(ids, integerFromBoolean(extended), fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getDialogs(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Single<DialogsResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getDialogs(offset, count, startMessageId, integerFromBoolean(extended), fields)
                    .map(extractResponseWithErrorHandling())
            } /*.map(response -> {
                            if (response.dialogs != null) {
                                for (VKApiDialog dialog : response.dialogs) {
                                    fixMessage(dialog.message);
                                }
                            }
                            return response;
                        })*/

    }

    override fun unpin(peerId: Long): Completable {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service.unpin(peerId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun pin(peerId: Long, messageId: Int): Completable {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service.pin(peerId, messageId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun pinUnPinConversation(peerId: Long, peen: Boolean): Completable {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                (if (peen) service.pinConversation(peerId) else service.unpinConversation(peerId))
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun markAsListened(message_id: Int): Completable {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapCompletable { service ->
                service.markAsListened(message_id)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun getById(identifiers: Collection<Int>?): Single<List<VKApiMessage>> {
        val ids = join(identifiers, ",")
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getById(ids, null)
                    .map(extractResponseWithErrorHandling())
                    .map { listEmptyIfNull(it.items) }
            }
    }

    override fun getHistory(
        offset: Int?,
        count: Int?,
        peerId: Long,
        startMessageId: Int?,
        rev: Boolean?,
        extended: Boolean?,
        fields: String?
    ): Single<MessageHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getHistory(
                        offset,
                        count,
                        peerId,
                        startMessageId,
                        integerFromBoolean(rev),
                        integerFromBoolean(extended),
                        fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getJsonHistory(
        offset: Int?,
        count: Int?,
        peerId: Long
    ): Single<Items<VKApiJsonString>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getJsonHistory(offset, count, peerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getImportantMessages(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Single<MessageImportantResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getImportantMessages(
                        offset,
                        count,
                        startMessageId,
                        integerFromBoolean(extended),
                        fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLongpollServer(needPts: Boolean, lpVersion: Int): Single<VKApiLongpollServer> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .getLongpollServer(if (needPts) 1 else 0, lpVersion)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchConversations(
        query: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<ConversationsResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .searchConversations(query, count, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }
}