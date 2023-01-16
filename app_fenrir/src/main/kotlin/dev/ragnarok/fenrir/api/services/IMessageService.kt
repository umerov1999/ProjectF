package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class IMessageService : IServiceRest() {
    fun editMessage(
        peedId: Long,
        messageId: Int,
        message: String?,
        attachment: String?,
        keepForwardMessages: Int?,
        keepSnippets: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.edit", form(
                "peer_id" to peedId,
                "message_id" to messageId,
                "message" to message,
                "attachment" to attachment,
                "keep_forward_messages" to keepForwardMessages,
                "keep_snippets" to keepSnippets
            ), baseInt
        )
    }

    fun pin(
        peerId: Long,
        messageId: Int
    ): Single<BaseResponse<VKApiMessage>> {
        return rest.request(
            "messages.pin", form(
                "peer_id" to peerId,
                "message_id" to messageId
            ), base(VKApiMessage.serializer())
        )
    }

    fun pinConversation(peerId: Long): Single<BaseResponse<Int>> {
        return rest.request("messages.pinConversation", form("peer_id" to peerId), baseInt)
    }

    fun unpinConversation(peerId: Long): Single<BaseResponse<Int>> {
        return rest.request("messages.unpinConversation", form("peer_id" to peerId), baseInt)
    }

    fun markAsListened(message_id: Int): Single<BaseResponse<Int>> {
        return rest.request("messages.markAsListened", form("message_id" to message_id), baseInt)
    }

    fun unpin(peerId: Long): Single<BaseResponse<Int>> {
        return rest.request("messages.unpin", form("peer_id" to peerId), baseInt)
    }

    /**
     * Allows the current user to leave a chat or, if the current user started the chat,
     * allows the user to remove another user from the chat.
     *
     * @param chatId   Chat ID
     * @param memberId ID of the member to be removed from the chat
     * @return 1
     */
    fun removeChatUser(
        chatId: Long,
        memberId: Long
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.removeChatUser", form(
                "chat_id" to chatId,
                "member_id" to memberId
            ), baseInt
        )
    }

    fun deleteChatPhoto(chatId: Long): Single<BaseResponse<UploadChatPhotoResponse>> {
        return rest.request(
            "messages.deleteChatPhoto",
            form("chat_id" to chatId),
            base(UploadChatPhotoResponse.serializer())
        )
    }

    /**
     * Adds a new user to a chat.
     *
     * @param chatId Chat ID
     * @param userId ID of the user to be added to the chat.
     * @return 1
     */
    fun addChatUser(
        chatId: Long,
        userId: Long
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.addChatUser", form(
                "chat_id" to chatId,
                "user_id" to userId
            ), baseInt
        )
    }

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
    fun getChat(
        chatId: Long?,
        chatIds: String?,
        fields: String?,
        nameCase: String?
    ): Single<BaseResponse<ChatsInfoResponse>> {
        return rest.request(
            "messages.getChat", form(
                "chat_id" to chatId,
                "chat_ids" to chatIds,
                "fields" to fields,
                "name_case" to nameCase
            ), base(ChatsInfoResponse.serializer())
        )
    }

    fun getConversationMembers(
        peer_id: Long?,
        fields: String?
    ): Single<BaseResponse<ConversationMembersResponse>> {
        return rest.request(
            "messages.getConversationMembers",
            form(
                "peer_id" to peer_id,
                "fields" to fields
            ),
            base(ConversationMembersResponse.serializer())
        )
    }

    /**
     * Edits the title of a chat.
     *
     * @param chatId Chat ID.
     * @param title  New title of the chat.
     * @return 1
     */
    fun editChat(
        chatId: Long,
        title: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.editChat", form(
                "chat_id" to chatId,
                "title" to title
            ), baseInt
        )
    }

    /**
     * Creates a chat with several participants.
     *
     * @param userIds IDs of the users to be added to the chat. List of comma-separated positive numbers
     * @param title   Chat title
     * @return the ID of the created chat (chat_id).
     */
    fun createChat(
        userIds: String?,
        title: String?
    ): Single<BaseResponse<Long>> {
        return rest.request(
            "messages.createChat", form(
                "user_ids" to userIds,
                "title" to title
            ), baseLong
        )
    }

    /**
     * Deletes all private messages in a conversation.
     *
     * @param peerId Destination ID.
     * @return 1
     */
    fun deleteDialog(peerId: Long): Single<BaseResponse<ConversationDeleteResult>> {
        return rest.request(
            "messages.deleteConversation",
            form("peer_id" to peerId),
            base(ConversationDeleteResult.serializer())
        )
    }

    /**
     * Restores a deleted message.
     *
     * @param messageId ID of a previously-deleted message to restore
     * @return 1
     */
    fun restore(messageId: Int): Single<BaseResponse<Int>> {
        return rest.request("messages.restore", form("message_id" to messageId), baseInt)
    }

    /**
     * Deletes one or more messages.
     *
     * @param messageIds Message IDs. List of comma-separated positive numbers
     * @param spam       1 — to mark message as spam.
     * @return 1
     */
    fun delete(
        messageIds: String?,
        deleteForAll: Int?,
        spam: Int?
    ): Single<BaseResponse<Map<String, Int>>> {
        return rest.request(
            "messages.delete",
            form(
                "message_ids" to messageIds,
                "delete_for_all" to deleteForAll,
                "spam" to spam
            ),
            base(MapSerializer(String.serializer(), Int.serializer()))
        )
    }

    /**
     * Marks messages as read.
     *
     * @param peerId         Destination ID.
     * @param startMessageId Message ID to start from
     * @return 1
     */
    fun markAsRead(
        peerId: Long?,
        startMessageId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.markAsRead", form(
                "peer_id" to peerId,
                "start_message_id" to startMessageId
            ), baseInt
        )
    }

    fun markAsImportant(
        messageIds: String?,
        important: Int?
    ): Single<BaseResponse<List<Int>>> {
        return rest.request(
            "messages.markAsImportant", form(
                "message_ids" to messageIds,
                "important" to important
            ), baseList(Int.serializer())
        )
    }

    /**
     * Changes the status of a user as typing in a conversation.
     *
     * @param peerId Destination ID
     * @param type   typing — user has started to type.
     * @return 1
     */
    fun setActivity(
        peerId: Long,
        type: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.setActivity", form(
                "peer_id" to peerId,
                "type" to type
            ), baseInt
        )
    }

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
    fun search(
        query: String?,
        peerId: Long?,
        date: Long?,
        previewLength: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiMessage>>> {
        return rest.request(
            "messages.search", form(
                "q" to query,
                "peer_id" to peerId,
                "date" to date,
                "preview_length" to previewLength,
                "offset" to offset,
                "count" to count
            ), items(VKApiMessage.serializer())
        )
    }

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
    fun getLongPollHistory(
        ts: Long?,
        pts: Long?,
        previewLength: Int?,
        onlines: Int?,
        fields: String?,
        eventsLimit: Int?,
        msgsLimit: Int?,
        maxMsgId: Int?
    ): Single<BaseResponse<LongpollHistoryResponse>> {
        return rest.request(
            "messages.getLongPollHistory",
            form(
                "ts" to ts,
                "pts" to pts,
                "preview_length" to previewLength,
                "onlines" to onlines,
                "fields" to fields,
                "events_limit" to eventsLimit,
                "msgs_limit" to msgsLimit,
                "max_msg_id" to maxMsgId
            ),
            base(LongpollHistoryResponse.serializer())
        )
    }

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
    fun getHistoryAttachments(
        peerId: Long,
        mediaType: String?,
        startFrom: String?,
        count: Int?,
        photoSizes: Int?,
        fields: String?
    ): Single<BaseResponse<AttachmentsHistoryResponse>> {
        return rest.request(
            "messages.getHistoryAttachments",
            form(
                "peer_id" to peerId,
                "media_type" to mediaType,
                "start_from" to startFrom,
                "count" to count,
                "photo_sizes" to photoSizes,
                "fields" to fields
            ),
            base(AttachmentsHistoryResponse.serializer())
        )
    }

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
    fun send(
        randomId: Long?,
        peerId: Long?,
        domain: String?,
        message: String?,
        latitude: Double?,
        longitude: Double?,
        attachment: String?,
        forwardMessages: String?,
        stickerId: Int?,
        payload: String?,
        reply_to: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.send", form(
                "random_id" to randomId,
                "peer_id" to peerId,
                "domain" to domain,
                "message" to message,
                "lat" to latitude,
                "long" to longitude,
                "attachment" to attachment,
                "forward_messages" to forwardMessages,
                "sticker_id" to stickerId,
                "payload" to payload,
                "reply_to" to reply_to
            ), baseInt
        )
    }

    /**
     * Returns messages by their IDs.
     *
     * @param messageIds    Message IDs. List of comma-separated positive numbers
     * @param previewLength Number of characters after which to truncate a previewed message.
     * To preview the full message, specify 0.
     * NOTE: Messages are not truncated by default. Messages are truncated by words.
     * @return a list of message objects.
     */
    fun getById(
        messageIds: String?,
        previewLength: Int?
    ): Single<BaseResponse<Items<VKApiMessage>>> {
        return rest.request(
            "messages.getById", form(
                "message_ids" to messageIds,
                "preview_length" to previewLength
            ), items(VKApiMessage.serializer())
        )
    }

    //https://vk.com/dev/messages.getConversations
    fun getDialogs(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<DialogsResponse>> {
        return rest.request(
            "messages.getConversations", form(
                "offset" to offset,
                "count" to count,
                "start_message_id" to startMessageId,
                "extended" to extended,
                "fields" to fields
            ), base(DialogsResponse.serializer())
        )
    }

    //https://vk.com/dev/messages.getConversationsById
    fun getConversationsById(
        peerIds: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ItemsProfilesGroupsResponse<VKApiConversation>>> {
        return rest.request(
            "messages.getConversationsById",
            form(
                "peer_ids" to peerIds,
                "extended" to extended,
                "fields" to fields
            ),
            base(ItemsProfilesGroupsResponse.serializer(VKApiConversation.serializer()))
        )
    }

    fun getLongpollServer(
        needPts: Int,
        lpVersion: Int
    ): Single<BaseResponse<VKApiLongpollServer>> {
        return rest.request(
            "messages.getLongPollServer",
            form(
                "need_pts" to needPts,
                "lp_version" to lpVersion
            ),
            base(VKApiLongpollServer.serializer())
        )
    }

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
    fun getHistory(
        offset: Int?,
        count: Int?,
        peerId: Long,
        startMessageId: Int?,
        rev: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<MessageHistoryResponse>> {
        return rest.request(
            "messages.getHistory",
            form(
                "offset" to offset,
                "count" to count,
                "peer_id" to peerId,
                "start_message_id" to startMessageId,
                "rev" to rev,
                "extended" to extended,
                "fields" to fields
            ),
            base(MessageHistoryResponse.serializer())
        )
    }

    fun getJsonHistory(
        offset: Int?,
        count: Int?,
        peerId: Long
    ): Single<BaseResponse<Items<VKApiJsonString>>> {
        return rest.request(
            "messages.getHistory", form(
                "offset" to offset,
                "count" to count,
                "peer_id" to peerId
            ), items(VKApiJsonString.serializer())
        )
    }

    fun getImportantMessages(
        offset: Int?,
        count: Int?,
        startMessageId: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<MessageImportantResponse>> {
        return rest.request(
            "messages.getImportantMessages",
            form(
                "offset" to offset,
                "count" to count,
                "start_message_id" to startMessageId,
                "extended" to extended,
                "fields" to fields
            ),
            base(MessageImportantResponse.serializer())
        )
    }

    //https://vk.com/dev/messages.searchDialogs
    fun searchConversations(
        q: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ConversationsResponse>> {
        return rest.request(
            "messages.searchConversations",
            form(
                "q" to q,
                "count" to count,
                "extended" to extended,
                "fields" to fields
            ),
            base(ConversationsResponse.serializer())
        )
    }

    fun recogniseAudioMessage(
        message_id: Int?,
        audio_message_id: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.recogniseAudioMessage", form(
                "message_id" to message_id,
                "audio_message_id" to audio_message_id
            ), baseInt
        )
    }

    fun setMemberRole(
        peer_id: Long?,
        member_id: Long?,
        role: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "messages.setMemberRole", form(
                "peer_id" to peer_id,
                "member_id" to member_id,
                "role" to role
            ), baseInt
        )
    }
}