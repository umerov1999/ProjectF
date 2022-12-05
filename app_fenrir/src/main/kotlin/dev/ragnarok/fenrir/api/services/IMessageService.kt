package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.*
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IMessageService {
    @FormUrlEncoded
    @POST("messages.edit")
    fun editMessage(
        @Field("peer_id") peedId: Int,
        @Field("message_id") messageId: Int,
        @Field("message") message: String?,
        @Field("attachment") attachment: String?,
        @Field("keep_forward_messages") keepForwardMessages: Int?,
        @Field("keep_snippets") keepSnippets: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.pin")
    fun pin(
        @Field("peer_id") peerId: Int,
        @Field("message_id") messageId: Int
    ): Single<BaseResponse<VKApiMessage>>

    @FormUrlEncoded
    @POST("messages.pinConversation")
    fun pinConversation(@Field("peer_id") peerId: Int): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.unpinConversation")
    fun unpinConversation(@Field("peer_id") peerId: Int): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.markAsListened")
    fun markAsListened(@Field("message_id") message_id: Int): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.unpin")
    fun unpin(@Field("peer_id") peerId: Int): Single<BaseResponse<Int>>

    /**
     * Allows the current user to leave a chat or, if the current user started the chat,
     * allows the user to remove another user from the chat.
     *
     * @param chatId   Chat ID
     * @param memberId ID of the member to be removed from the chat
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.removeChatUser")
    fun removeChatUser(
        @Field("chat_id") chatId: Int,
        @Field("member_id") memberId: Int
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.deleteChatPhoto")
    fun deleteChatPhoto(@Field("chat_id") chatId: Int): Single<BaseResponse<UploadChatPhotoResponse>>

    /**
     * Adds a new user to a chat.
     *
     * @param chatId Chat ID
     * @param userId ID of the user to be added to the chat.
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.addChatUser")
    fun addChatUser(
        @Field("chat_id") chatId: Int,
        @Field("user_id") userId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Returns information about a chat.
     *
     * @param chatId   Chat ID.
     * @param chatIds  Chat IDs. List of comma-separated numbers
     * @param fields   Profile fields to return. List of comma-separated words
     * @param nameCase Case for declension of user name and surname:
     * nom — nominative (default)
     * gen — genitive
     * dat — dative
     * acc — accusative
     * ins — instrumental
     * abl — prepositional
     * @return Returns a list of chat objects.
     */
    @FormUrlEncoded
    @POST("messages.getChat")
    fun getChat(
        @Field("chat_id") chatId: Int?,
        @Field("chat_ids") chatIds: String?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<ChatsInfoResponse>>

    @FormUrlEncoded
    @POST("messages.getConversationMembers")
    fun getConversationMembers(
        @Field("peer_id") peer_id: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<ConversationMembersResponse>>

    /**
     * Edits the title of a chat.
     *
     * @param chatId Chat ID.
     * @param title  New title of the chat.
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.editChat")
    fun editChat(
        @Field("chat_id") chatId: Int,
        @Field("title") title: String?
    ): Single<BaseResponse<Int>>

    /**
     * Creates a chat with several participants.
     *
     * @param userIds IDs of the users to be added to the chat. List of comma-separated positive numbers
     * @param title   Chat title
     * @return the ID of the created chat (chat_id).
     */
    @FormUrlEncoded
    @POST("messages.createChat")
    fun createChat(
        @Field("user_ids") userIds: String?,
        @Field("title") title: String?
    ): Single<BaseResponse<Int>>

    /**
     * Deletes all private messages in a conversation.
     *
     * @param peerId Destination ID.
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.deleteConversation")
    fun deleteDialog(@Field("peer_id") peerId: Int): Single<BaseResponse<ConversationDeleteResult>>

    /**
     * Restores a deleted message.
     *
     * @param messageId ID of a previously-deleted message to restore
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.restore")
    fun restore(@Field("message_id") messageId: Int): Single<BaseResponse<Int>>

    /**
     * Deletes one or more messages.
     *
     * @param messageIds Message IDs. List of comma-separated positive numbers
     * @param spam       1 — to mark message as spam.
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.delete")
    fun delete(
        @Field("message_ids") messageIds: String?,
        @Field("delete_for_all") deleteForAll: Int?,
        @Field("spam") spam: Int?
    ): Single<BaseResponse<Map<String, Int>>>

    /**
     * Marks messages as read.
     *
     * @param peerId         Destination ID.
     * @param startMessageId Message ID to start from
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.markAsRead")
    fun markAsRead(
        @Field("peer_id") peerId: Int?,
        @Field("start_message_id") startMessageId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.markAsImportant")
    fun markAsImportant(
        @Field("message_ids") messageIds: String?,
        @Field("important") important: Int?
    ): Single<BaseResponse<List<Int>>>

    /**
     * Changes the status of a user as typing in a conversation.
     *
     * @param peerId Destination ID
     * @param type   typing — user has started to type.
     * @return 1
     */
    @FormUrlEncoded
    @POST("messages.setActivity")
    fun setActivity(
        @Field("peer_id") peerId: Int,
        @Field("type") type: String?
    ): Single<BaseResponse<Int>>

    /**
     * Returns a list of the current user's private messages that match search criteria.
     *
     * @param query         Search query string
     * @param peerId        Destination ID
     * @param date          Date to search message before in Unixtime
     * @param previewLength Number of characters after which to truncate a previewed message.
     * To preview the full message, specify 0
     * NOTE: Messages are not truncated by default. Messages are truncated by words
     * @param offset        Offset needed to return a specific subset of messages.
     * @param count         Number of messages to return
     * @return list of the current user's private messages
     */
    @FormUrlEncoded
    @POST("messages.search")
    fun search(
        @Field("q") query: String?,
        @Field("peer_id") peerId: Int?,
        @Field("date") date: Long?,
        @Field("preview_length") previewLength: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiMessage>>>

    /**
     * Returns updates in user's private messages.
     * To speed up handling of private messages, it can be useful to cache previously loaded messages
     * on a user's mobile device/desktop, to prevent re-receipt at each call.
     * With this method, you can synchronize a local copy of the message list with the actual version.
     *
     * @param ts            Last value of the ts parameter returned from the Long Poll server or by using
     * @param pts           Last value of pts parameter returned from the Long Poll server or by using
     * @param previewLength Number of characters after which to truncate a previewed message.
     * To preview the full message, specify 0.
     * NOTE: Messages are not truncated by default. Messages are truncated by words.
     * @param onlines       1 — to return history with online users only
     * @param fields        Additional profile fileds to return. List of comma-separated words, default
     * @param eventsLimit   Maximum number of events to return.
     * @param msgsLimit     Maximum number of messages to return.
     * @param maxMsgId      Maximum ID of the message among existing ones in the local copy.
     * Both messages received with API methods (for example, messages.getDialogs, messages.getHistory),
     * and data received from a Long Poll server (events with code 4) are taken into account.
     * @return an object that contains the following fields:
     * history — An array similar to updates field returned from the Long Poll server, with these exceptions:
     * For events with code 4 (addition of a new message), there are no fields except the first three.
     * There are no events with codes 8, 9 (friend goes online/offline) or with codes 61, 62 (typing during conversation/chat).
     * messages — An array of private message objects that were found among events with code 4
     * (addition of a new message) from the history field. Each object of message contains a set
     * of fields described here. The first array element is the total number of messages.
     */
    @FormUrlEncoded
    @POST("messages.getLongPollHistory")
    fun getLongPollHistory(
        @Field("ts") ts: Long?,
        @Field("pts") pts: Long?,
        @Field("preview_length") previewLength: Int?,
        @Field("onlines") onlines: Int?,
        @Field("fields") fields: String?,
        @Field("events_limit") eventsLimit: Int?,
        @Field("msgs_limit") msgsLimit: Int?,
        @Field("max_msg_id") maxMsgId: Int?
    ): Single<BaseResponse<LongpollHistoryResponse>>

    /**
     * Returns media files from the dialog or group chat.
     *
     * @param peerId     Peer ID.
     * @param mediaType  Type of media files to return: photo, video, audio, doc, link.
     * @param startFrom  Message ID to start return results from.
     * @param count      Number of objects to return. Maximum value 200, default 30
     * @param photoSizes 1 — to return photo sizes in a special format
     * @param fields     Additional profile fields to return
     * @return a list of photo, video, audio or doc objects depending on media_type parameter value
     * and additional next_from field containing new offset value.
     */
    @FormUrlEncoded
    @POST("messages.getHistoryAttachments")
    fun getHistoryAttachments(
        @Field("peer_id") peerId: Int,
        @Field("media_type") mediaType: String?,
        @Field("start_from") startFrom: String?,
        @Field("count") count: Int?,
        @Field("photo_sizes") photoSizes: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<AttachmentsHistoryResponse>>

    /**
     * Sends a message.
     *
     * @param randomId        Unique identifier to avoid resending the message
     * @param peerId          Destination ID
     * @param domain          User's short address (for example, illarionov).
     * @param message         (Required if attachments is not set.) Text of the message
     * @param latitude        Geographical latitude of a check-in, in degrees (from -90 to 90).
     * @param longitude       Geographical longitude of a check-in, in degrees (from -180 to 180).
     * @param attachment      (Required if message is not set.) List of objects attached to the message,
     * separated by commas, in the following format: {type}{owner_id}_{media_id}_{access_key}
     * @param forwardMessages ID of forwarded messages, separated with a comma.
     * Listed messages of the sender will be shown in the message body at the recipient's.
     * @param stickerId       Sticker id
     * @return sent message ID.
     */
    @FormUrlEncoded
    @POST("messages.send")
    fun send(
        @Field("random_id") randomId: Long?,
        @Field("peer_id") peerId: Int?,
        @Field("domain") domain: String?,
        @Field("message") message: String?,
        @Field("lat") latitude: Double?,
        @Field("long") longitude: Double?,
        @Field("attachment") attachment: String?,
        @Field("forward_messages") forwardMessages: String?,
        @Field("sticker_id") stickerId: Int?,
        @Field("payload") payload: String?,
        @Field("reply_to") reply_to: Int?
    ): Single<BaseResponse<Int>>

    /**
     * Returns messages by their IDs.
     *
     * @param messageIds    Message IDs. List of comma-separated positive numbers
     * @param previewLength Number of characters after which to truncate a previewed message.
     * To preview the full message, specify 0.
     * NOTE: Messages are not truncated by default. Messages are truncated by words.
     * @return a list of message objects.
     */
    @FormUrlEncoded
    @POST("messages.getById")
    fun getById(
        @Field("message_ids") messageIds: String?,
        @Field("preview_length") previewLength: Int?
    ): Single<BaseResponse<Items<VKApiMessage>>>

    //https://vk.com/dev/messages.getConversations
    @FormUrlEncoded
    @POST("messages.getConversations")
    fun getDialogs(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("start_message_id") startMessageId: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<DialogsResponse>>

    //https://vk.com/dev/messages.getConversationsById
    @FormUrlEncoded
    @POST("messages.getConversationsById")
    fun getConversationsById(
        @Field("peer_ids") peerIds: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<ItemsProfilesGroupsResponse<VKApiConversation>>>

    @FormUrlEncoded
    @POST("messages.getLongPollServer")
    fun getLongpollServer(
        @Field("need_pts") needPts: Int,
        @Field("lp_version") lpVersion: Int
    ): Single<BaseResponse<VKApiLongpollServer>>

    /**
     * Returns message history for the specified user or group chat.
     *
     * @param offset         Offset needed to return a specific subset of messages.
     * @param count          Number of messages to return. Default 20, maximum value 200
     * @param peerId         Destination ID
     * @param startMessageId Starting message ID from which to return history.
     * @param rev            Sort order:
     * 1 — return messages in chronological order.
     * 0 — return messages in reverse chronological order.
     * @return Returns a list of message objects.
     */
    @FormUrlEncoded
    @POST("messages.getHistory")
    fun getHistory(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("peer_id") peerId: Int,
        @Field("start_message_id") startMessageId: Int?,
        @Field("rev") rev: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<MessageHistoryResponse>>

    @FormUrlEncoded
    @POST("messages.getHistory")
    fun getJsonHistory(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("peer_id") peerId: Int
    ): Single<BaseResponse<Items<VKApiJsonString>>>

    @FormUrlEncoded
    @POST("messages.getImportantMessages")
    fun getImportantMessages(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("start_message_id") startMessageId: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<MessageImportantResponse>>

    //https://vk.com/dev/messages.searchDialogs
    @FormUrlEncoded
    @POST("messages.searchConversations")
    fun searchConversations(
        @Field("q") q: String?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<ConversationsResponse>>

    @FormUrlEncoded
    @POST("messages.recogniseAudioMessage")
    fun recogniseAudioMessage(
        @Field("message_id") message_id: Int?,
        @Field("audio_message_id") audio_message_id: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("messages.setMemberRole")
    fun setMemberRole(
        @Field("peer_id") peer_id: Int?,
        @Field("member_id") member_id: Int?,
        @Field("role") role: String?
    ): Single<BaseResponse<Int>>
}