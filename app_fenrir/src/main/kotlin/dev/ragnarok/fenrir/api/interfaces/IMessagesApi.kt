package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IMessagesApi {
    @CheckResult
    fun edit(
        peerId: Long,
        messageId: Int,
        message: String?,
        attachments: List<IAttachmentToken>?,
        keepFwd: Boolean,
        keepSnippets: Boolean?
    ): Completable

    @CheckResult
    fun removeChatMember(chatId: Long, memberId: Long): Single<Boolean>

    @CheckResult
    fun deleteChatPhoto(chatId: Long): Single<Boolean>

    @CheckResult
    fun addChatUser(chatId: Long, userId: Long): Single<Boolean>

    @CheckResult
    fun getChat(
        chatId: Long?,
        chatIds: Collection<Long>?,
        fields: String?,
        name_case: String?
    ): Single<List<VKApiChat>>

    @CheckResult
    fun getConversationMembers(
        peer_id: Long?,
        fields: String?
    ): Single<ConversationMembersResponse>

    @CheckResult
    fun editChat(chatId: Long, title: String?): Single<Boolean>

    @CheckResult
    fun createChat(userIds: Collection<Long>, title: String?): Single<Long>

    @CheckResult
    fun deleteDialog(peerId: Long): Single<ConversationDeleteResult>

    @CheckResult
    fun restore(messageId: Int): Single<Boolean>

    @CheckResult
    fun delete(
        messageIds: Collection<Int>,
        deleteForAll: Boolean?,
        spam: Boolean?
    ): Single<Map<String, Int>>

    @CheckResult
    fun markAsRead(peerId: Long?, startMessageId: Int?): Single<Boolean>

    @CheckResult
    fun setActivity(peerId: Long, typing: Boolean): Single<Boolean>

    @CheckResult
    fun search(
        query: String?, peerId: Long?, date: Long?, previewLength: Int?,
        offset: Int?, count: Int?
    ): Single<Items<VKApiMessage>>

    @CheckResult
    fun markAsImportant(messageIds: Collection<Int>, important: Int?): Single<List<Int>>

    @CheckResult
    fun getLongPollHistory(
        ts: Long?, pts: Long?, previewLength: Int?,
        onlines: Boolean?, fields: String?,
        eventsLimit: Int?, msgsLimit: Int?,
        max_msg_id: Int?
    ): Single<LongpollHistoryResponse>

    @CheckResult
    fun getHistoryAttachments(
        peerId: Long, mediaType: String?, startFrom: String?, photoSizes: Int?,
        count: Int?, fields: String?
    ): Single<AttachmentsHistoryResponse>

    @CheckResult
    fun send(
        randomId: Long?, peerId: Long?, domain: String?, message: String?,
        latitude: Double?, longitude: Double?, attachments: Collection<IAttachmentToken>?,
        forwardMessages: Collection<Int>?, stickerId: Int?, payload: String?, reply_to: Int?
    ): Single<Int>

    @CheckResult
    fun getDialogs(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Single<DialogsResponse>

    @CheckResult
    fun getConversations(
        peers: List<Long>,
        extended: Boolean?,
        fields: String?
    ): Single<ItemsProfilesGroupsResponse<VKApiConversation>>

    @CheckResult
    fun getById(identifiers: Collection<Int>?): Single<List<VKApiMessage>>

    @CheckResult
    fun getHistory(
        offset: Int?,
        count: Int?,
        peerId: Long,
        startMessageId: Int?,
        rev: Boolean?,
        extended: Boolean?,
        fields: String?
    ): Single<MessageHistoryResponse>

    @CheckResult
    fun getJsonHistory(offset: Int?, count: Int?, peerId: Long): Single<Items<VKApiJsonString>>

    @CheckResult
    fun getImportantMessages(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Boolean?,
        fields: String?
    ): Single<MessageImportantResponse>

    @CheckResult
    fun getLongpollServer(needPts: Boolean, lpVersion: Int): Single<VKApiLongpollServer>

    @CheckResult
    fun searchConversations(
        query: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<ConversationsResponse>

    @CheckResult
    fun pin(peerId: Long, messageId: Int): Completable

    @CheckResult
    fun unpin(peerId: Long): Completable

    @CheckResult
    fun pinUnPinConversation(peerId: Long, peen: Boolean): Completable

    @CheckResult
    fun markAsListened(message_id: Int): Completable

    @CheckResult
    fun recogniseAudioMessage(message_id: Int?, audio_message_id: String?): Single<Int>

    @CheckResult
    fun setMemberRole(peer_id: Long?, member_id: Long?, role: String?): Single<Int>
}