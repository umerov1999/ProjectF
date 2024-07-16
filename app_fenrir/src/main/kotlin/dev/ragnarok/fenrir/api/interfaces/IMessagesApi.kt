package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Assets
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
import kotlinx.coroutines.flow.Flow

interface IMessagesApi {
    @CheckResult
    fun edit(
        peerId: Long,
        messageId: Int,
        message: String?,
        attachments: List<IAttachmentToken>?,
        keepFwd: Boolean,
        keepSnippets: Boolean?
    ): Flow<Boolean>

    @CheckResult
    fun removeChatMember(chatId: Long, memberId: Long): Flow<Boolean>

    @CheckResult
    fun deleteChatPhoto(chatId: Long): Flow<Boolean>

    @CheckResult
    fun addChatUser(chatId: Long, userId: Long): Flow<Boolean>

    @CheckResult
    fun getChat(
        chatId: Long?,
        chatIds: Collection<Long>?,
        fields: String?,
        name_case: String?
    ): Flow<List<VKApiChat>>

    @CheckResult
    fun getConversationMembers(
        peer_id: Long?,
        fields: String?
    ): Flow<ConversationMembersResponse>

    @CheckResult
    fun editChat(chatId: Long, title: String?): Flow<Boolean>

    @CheckResult
    fun createChat(userIds: Collection<Long>, title: String?): Flow<Long>

    @CheckResult
    fun deleteDialog(peerId: Long): Flow<ConversationDeleteResult>

    @CheckResult
    fun restore(messageId: Int): Flow<Boolean>

    @CheckResult
    fun delete(
        messageIds: Collection<Int>,
        deleteForAll: Boolean?,
        spam: Boolean?
    ): Flow<List<MessageDeleteResponse>>

    @CheckResult
    fun markAsRead(peerId: Long?, startMessageId: Int?): Flow<Boolean>

    @CheckResult
    fun setActivity(peerId: Long, typing: Boolean): Flow<Boolean>

    @CheckResult
    fun search(
        query: String?, peerId: Long?, date: Long?, previewLength: Int?,
        offset: Int?, count: Int?
    ): Flow<Items<VKApiMessage>>

    @CheckResult
    fun markAsImportant(messageIds: Collection<Int>, important: Int?): Flow<List<Int>>

    @CheckResult
    fun getLongPollHistory(
        ts: Long?, pts: Long?, previewLength: Int?,
        onlines: Boolean?, fields: String?,
        eventsLimit: Int?, msgsLimit: Int?,
        max_msg_id: Int?
    ): Flow<LongpollHistoryResponse>

    @CheckResult
    fun getHistoryAttachments(
        peerId: Long,
        mediaType: String?,
        startFrom: String?,
        photoSizes: Int?,
        preserve_order: Int?,
        max_forwards_level: Int?,
        count: Int?,
        fields: String?
    ): Flow<AttachmentsHistoryResponse>

    @CheckResult
    fun send(
        randomId: Long?, peerId: Long?, domain: String?, message: String?,
        latitude: Double?, longitude: Double?, attachments: Collection<IAttachmentToken>?,
        forwardMessages: Collection<Int>?, stickerId: Int?, payload: String?, reply_to: Int?
    ): Flow<SendMessageResponse>

    @CheckResult
    fun getDialogs(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Flow<DialogsResponse>

    @CheckResult
    fun getConversations(
        peers: List<Long>,
        extended: Boolean?,
        fields: String?
    ): Flow<ItemsProfilesGroupsResponse<VKApiConversation>>

    @CheckResult
    fun getById(identifiers: Collection<Int>?): Flow<List<VKApiMessage>>

    @CheckResult
    fun getHistory(
        offset: Int?,
        count: Int?,
        peerId: Long,
        startMessageId: Int?,
        rev: Boolean?,
        extended: Boolean?,
        fields: String?
    ): Flow<MessageHistoryResponse>

    @CheckResult
    fun getJsonHistory(offset: Int?, count: Int?, peerId: Long): Flow<Items<VKApiJsonString>>

    @CheckResult
    fun getImportantMessages(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Flow<MessageImportantResponse>

    @CheckResult
    fun getLongpollServer(needPts: Boolean, lpVersion: Int): Flow<VKApiLongpollServer>

    @CheckResult
    fun searchConversations(
        query: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<ConversationsResponse>

    @CheckResult
    fun pin(peerId: Long, messageId: Int): Flow<Boolean>

    @CheckResult
    fun unpin(peerId: Long): Flow<Boolean>

    @CheckResult
    fun pinUnPinConversation(peerId: Long, peen: Boolean): Flow<Boolean>

    @CheckResult
    fun markAsListened(message_id: Int): Flow<Boolean>

    @CheckResult
    fun recogniseAudioMessage(message_id: Int?, audio_message_id: String?): Flow<Int>

    @CheckResult
    fun setMemberRole(peer_id: Long?, member_id: Long?, role: String?): Flow<Int>

    @CheckResult
    fun sendOrDeleteReaction(peer_id: Long, cmid: Int, reaction_id: Int?): Flow<Int>

    @CheckResult
    fun getReactionsAssets(): Flow<Assets<VKApiReactionAsset>>
}