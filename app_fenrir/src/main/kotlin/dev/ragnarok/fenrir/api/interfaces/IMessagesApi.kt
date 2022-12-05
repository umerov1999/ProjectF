package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IMessagesApi {
    @CheckResult
    fun edit(
        peerId: Int,
        messageId: Int,
        message: String?,
        attachments: List<IAttachmentToken>?,
        keepFwd: Boolean,
        keepSnippets: Boolean?
    ): Completable

    @CheckResult
    fun removeChatMember(chatId: Int, memberId: Int): Single<Boolean>

    @CheckResult
    fun deleteChatPhoto(chatId: Int): Single<Boolean>

    @CheckResult
    fun addChatUser(chatId: Int, userId: Int): Single<Boolean>

    @CheckResult
    fun getChat(
        chatId: Int?,
        chatIds: Collection<Int>?,
        fields: String?,
        name_case: String?
    ): Single<List<VKApiChat>>

    @CheckResult
    fun getConversationMembers(
        peer_id: Int?,
        fields: String?
    ): Single<ConversationMembersResponse>

    @CheckResult
    fun editChat(chatId: Int, title: String?): Single<Boolean>

    @CheckResult
    fun createChat(userIds: Collection<Int?>?, title: String?): Single<Int>

    @CheckResult
    fun deleteDialog(peerId: Int): Single<ConversationDeleteResult>

    @CheckResult
    fun restore(messageId: Int): Single<Boolean>

    @CheckResult
    fun delete(
        messageIds: Collection<Int>,
        deleteForAll: Boolean?,
        spam: Boolean?
    ): Single<Map<String, Int>>

    @CheckResult
    fun markAsRead(peerId: Int?, startMessageId: Int?): Single<Boolean>

    @CheckResult
    fun setActivity(peerId: Int, typing: Boolean): Single<Boolean>

    @CheckResult
    fun search(
        query: String?, peerId: Int?, date: Long?, previewLength: Int?,
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
        peerId: Int, mediaType: String?, startFrom: String?, photoSizes: Int?,
        count: Int?, fields: String?
    ): Single<AttachmentsHistoryResponse>

    @CheckResult
    fun send(
        randomId: Long?, peerId: Int?, domain: String?, message: String?,
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
        peers: List<Int>,
        extended: Boolean?,
        fields: String?
    ): Single<ItemsProfilesGroupsResponse<VKApiConversation>>

    @CheckResult
    fun getById(identifiers: Collection<Int>?): Single<List<VKApiMessage>>

    @CheckResult
    fun getHistory(
        offset: Int?,
        count: Int?,
        peerId: Int,
        startMessageId: Int?,
        rev: Boolean?,
        extended: Boolean?,
        fields: String?
    ): Single<MessageHistoryResponse>

    @CheckResult
    fun getJsonHistory(offset: Int?, count: Int?, peerId: Int): Single<Items<VKApiJsonString>>

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
    fun pin(peerId: Int, messageId: Int): Completable

    @CheckResult
    fun unpin(peerId: Int): Completable

    @CheckResult
    fun pinUnPinConversation(peerId: Int, peen: Boolean): Completable

    @CheckResult
    fun markAsListened(message_id: Int): Completable

    @CheckResult
    fun recogniseAudioMessage(message_id: Int?, audio_message_id: String?): Single<Int>

    @CheckResult
    fun setMemberRole(peer_id: Int?, member_id: Int?, role: String?): Single<Int>
}