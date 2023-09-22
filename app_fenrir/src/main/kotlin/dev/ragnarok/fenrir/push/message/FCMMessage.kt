package dev.ragnarok.fenrir.push.message

import com.google.firebase.messaging.RemoteMessage
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.realtime.Processors.realtimeMessages
import dev.ragnarok.fenrir.realtime.QueueContainsException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FCMMessage {
    var message_id = 0
    var peerId = 0L
    var conversation_message_id = 0
    fun notify(accountId: Long) {
        try {
            realtimeMessages.process(accountId, message_id, peerId, conversation_message_id, true)
        } catch (e: QueueContainsException) {
            e.printStackTrace()
        }
    }

    @Serializable
    private class MessageContext {
        @SerialName("msg_id")
        var msg_id = 0

        @SerialName("sender_id")
        var sender_id = 0L

        @SerialName("chat_id")
        var chat_id = 0

        @SerialName("conversation_message_id")
        var conversation_message_id = 0
    }

    companion object {
        fun fromRemoteMessage(remote: RemoteMessage): FCMMessage {
            val message = FCMMessage()
            val data = remote.data
            val context: MessageContext =
                kJson.decodeFromString(MessageContext.serializer(), data["context"]!!)
            message.message_id = context.msg_id
            message.peerId =
                if (context.chat_id == 0) context.sender_id else Peer.fromChatId(context.chat_id.toLong())
            message.conversation_message_id = context.conversation_message_id
            return message
        }
    }
}