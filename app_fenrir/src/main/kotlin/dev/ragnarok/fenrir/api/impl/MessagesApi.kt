package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.exceptions.ApiException
import dev.ragnarok.fenrir.api.interfaces.IMessagesApi
import dev.ragnarok.fenrir.api.model.Assets
import dev.ragnarok.fenrir.api.model.Error
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiJsonString
import dev.ragnarok.fenrir.api.model.VKApiLongpollServer
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.VKApiReactionAsset
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.AttachmentsHistoryResponse
import dev.ragnarok.fenrir.api.model.response.ConversationDeleteResult
import dev.ragnarok.fenrir.api.model.response.ConversationMembersResponse
import dev.ragnarok.fenrir.api.model.response.ConversationsResponse
import dev.ragnarok.fenrir.api.model.response.DialogsResponse
import dev.ragnarok.fenrir.api.model.response.ItemsProfilesGroupsResponse
import dev.ragnarok.fenrir.api.model.response.LongpollHistoryResponse
import dev.ragnarok.fenrir.api.model.response.MessageDeleteResponse
import dev.ragnarok.fenrir.api.model.response.MessageHistoryResponse
import dev.ragnarok.fenrir.api.model.response.MessageImportantResponse
import dev.ragnarok.fenrir.api.model.response.SendMessageResponse
import dev.ragnarok.fenrir.api.services.IMessageService
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class MessagesApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IMessagesApi {
    private fun serviceRx(vararg tokenTypes: Int): Flow<IMessageService> {
        return provideService(IMessageService(), *tokenTypes)
    }

    override fun edit(
        peerId: Long,
        messageId: Int,
        message: String?,
        attachments: List<IAttachmentToken>?,
        keepFwd: Boolean,
        keepSnippets: Boolean?
    ): Flow<Boolean> {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.editMessage(
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

    override fun removeChatMember(chatId: Long, memberId: Long): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.removeChatUser(chatId, memberId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun deleteChatPhoto(chatId: Long): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat { s ->
                s.deleteChatPhoto(chatId)
                    .map(extractResponseWithErrorHandling())
                    .map { it.message_id != 0 }
            }
    }

    override fun addChatUser(chatId: Long, userId: Long): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.addChatUser(chatId, userId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun getChat(
        chatId: Long?,
        chatIds: Collection<Long>?,
        fields: String?,
        name_case: String?
    ): Flow<List<VKApiChat>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat { s ->
                s.getChat(chatId, join(chatIds, ","), fields, name_case)
                    .map(extractResponseWithErrorHandling())
                    .map { listEmptyIfNull(it.chats) }
            }
    }

    override fun getConversationMembers(
        peer_id: Long?,
        fields: String?
    ): Flow<ConversationMembersResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getConversationMembers(peer_id, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun editChat(chatId: Long, title: String?): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.editChat(chatId, title)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun createChat(userIds: Collection<Long>, title: String?): Flow<Long> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.createChat(join(userIds, ","), title)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun recogniseAudioMessage(message_id: Int?, audio_message_id: String?): Flow<Int> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.recogniseAudioMessage(message_id, audio_message_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setMemberRole(peer_id: Long?, member_id: Long?, role: String?): Flow<Int> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.setMemberRole(peer_id, member_id, role)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteDialog(peerId: Long): Flow<ConversationDeleteResult> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.deleteDialog(peerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun restore(messageId: Int): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.restore(messageId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun delete(
        messageIds: Collection<Int>,
        deleteForAll: Boolean?,
        spam: Boolean?
    ): Flow<List<MessageDeleteResponse>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.delete(
                    join(messageIds, ","),
                    integerFromBoolean(deleteForAll),
                    integerFromBoolean(spam)
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun markAsRead(peerId: Long?, startMessageId: Int?): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.markAsRead(peerId, startMessageId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun markAsImportant(messageIds: Collection<Int>, important: Int?): Flow<List<Int>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.markAsImportant(join(messageIds, ","), important)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setActivity(peerId: Long, typing: Boolean): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.setActivity(peerId, if (typing) "typing" else null)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun search(
        query: String?,
        peerId: Long?,
        date: Long?,
        previewLength: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiMessage>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.search(query, peerId, date, previewLength, offset, count)
                    .map(extractResponseWithErrorHandling())
            }

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
    ): Flow<LongpollHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getLongPollHistory(
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
        preserve_order: Int?,
        max_forwards_level: Int?,
        count: Int?,
        fields: String?
    ): Flow<AttachmentsHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getHistoryAttachments(
                    peerId,
                    mediaType,
                    startFrom,
                    count,
                    photoSizes,
                    preserve_order,
                    max_forwards_level,
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun send(
        randomId: Long?, peerId: Long?, domain: String?, message: String?,
        latitude: Double?, longitude: Double?, attachments: Collection<IAttachmentToken>?,
        forwardMessages: Collection<Int>?, stickerId: Int?, payload: String?, reply_to: Int?
    ): Flow<SendMessageResponse> {
        val atts = join(attachments, ",") {
            formatAttachmentToken(it)
        }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat { s ->
                s.send(
                    randomId,
                    join(listOf(peerId), ","),
                    domain,
                    message,
                    latitude,
                    longitude,
                    atts,
                    join(forwardMessages, ","),
                    stickerId,
                    payload,
                    reply_to
                )
                    .map(extractResponseWithErrorHandling())
                    .map {
                        if (it.isEmpty()) {
                            throw NullPointerException("VK return null response")
                        }
                        it[0].error?.let { err ->
                            val error = Error()
                            error.errorCode = err.errorCode
                            error.errorMsg = err.description
                            throw ApiException(error)
                        }
                        it[0]
                    }
            }
    }

    override fun getConversations(
        peers: List<Long>,
        extended: Boolean?,
        fields: String?
    ): Flow<ItemsProfilesGroupsResponse<VKApiConversation>> {
        val ids = join(peers, ",") { it.toString() }
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getConversationsById(ids, integerFromBoolean(extended), fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getDialogs(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Flow<DialogsResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getDialogs(offset, count, startMessageId, integerFromBoolean(extended), fields)
                    .map(extractResponseWithErrorHandling())
            }

    }

    override fun unpin(peerId: Long): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.unpin(peerId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun pin(peerId: Long, messageId: Int): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.pin(peerId, messageId)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun pinUnPinConversation(peerId: Long, peen: Boolean): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                (if (peen) it.pinConversation(peerId) else it.unpinConversation(peerId))
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun markAsListened(message_id: Int): Flow<Boolean> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.markAsListened(message_id)
                    .map(extractResponseWithErrorHandling())
                    .ignoreElement()
            }
    }

    override fun getById(identifiers: Collection<Int>?): Flow<List<VKApiMessage>> {
        val ids = join(identifiers, ",")
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat { s ->
                s.getById(ids, null)
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
    ): Flow<MessageHistoryResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getHistory(
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
    ): Flow<Items<VKApiJsonString>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getJsonHistory(offset, count, peerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getImportantMessages(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Flow<MessageImportantResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getImportantMessages(
                    offset,
                    count,
                    startMessageId,
                    integerFromBoolean(extended),
                    fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLongpollServer(needPts: Boolean, lpVersion: Int): Flow<VKApiLongpollServer> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getLongpollServer(if (needPts) 1 else 0, lpVersion)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchConversations(
        query: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<ConversationsResponse> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.searchConversations(query, count, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getReactionsAssets(): Flow<Assets<VKApiReactionAsset>> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getReactionsAssets(null)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun sendOrDeleteReaction(peer_id: Long, cmid: Int, reaction_id: Int?): Flow<Int> {
        return serviceRx(TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                if (reaction_id != null) it.sendReaction(peer_id, cmid, reaction_id)
                    .map(extractResponseWithErrorHandling()) else it.deleteReaction(
                    peer_id,
                    cmid
                ).map(extractResponseWithErrorHandling())
            }
    }
}