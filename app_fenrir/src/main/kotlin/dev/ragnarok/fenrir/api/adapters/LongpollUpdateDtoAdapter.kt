package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.VKApiReaction
import dev.ragnarok.fenrir.api.model.longpoll.AbsLongpollEvent
import dev.ragnarok.fenrir.api.model.longpoll.AddMessageUpdate
import dev.ragnarok.fenrir.api.model.longpoll.BadgeCountChangeUpdate
import dev.ragnarok.fenrir.api.model.longpoll.InputMessagesSetReadUpdate
import dev.ragnarok.fenrir.api.model.longpoll.MessageFlagsResetUpdate
import dev.ragnarok.fenrir.api.model.longpoll.MessageFlagsSetUpdate
import dev.ragnarok.fenrir.api.model.longpoll.OutputMessagesSetReadUpdate
import dev.ragnarok.fenrir.api.model.longpoll.ReactionEventType
import dev.ragnarok.fenrir.api.model.longpoll.ReactionMessageChangeUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOfflineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.UserIsOnlineUpdate
import dev.ragnarok.fenrir.api.model.longpoll.WriteTextInDialogUpdate
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Utils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections

class LongpollUpdateDtoAdapter : AbsDtoAdapter<AbsLongpollEvent?>("AbsLongpollEvent?") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): AbsLongpollEvent? {
        val array = json.jsonArray
        val action = array[0].jsonPrimitive.int
        return deserialize(action, array)
    }

    private fun deserialize(
        action: Int,
        array: JsonArray
    ): AbsLongpollEvent? {
        when (action) {
            AbsLongpollEvent.ACTION_MESSAGE_EDITED, AbsLongpollEvent.ACTION_MESSAGE_CHANGED, AbsLongpollEvent.ACTION_MESSAGE_ADDED -> return deserializeAddMessageUpdate(
                array
            )

            AbsLongpollEvent.ACTION_MESSAGE_REACTION_CHANGE -> {
                @ReactionEventType val event_type_of_reaction: Int =
                    ReactionEventType.toReactionEventType(optInt(array, 1))
                var myReaction: Int? = null
                if (event_type_of_reaction == ReactionEventType.UNKNOW) {
                    return null
                }
                val peer_id = optLong(array, 2)
                val conversation_message_id = optInt(array, 3)
                var reactionCountIndex = 4
                if (event_type_of_reaction == ReactionEventType.I_ADDED_REACTION) {
                    myReaction = optInt(array, 4)
                    reactionCountIndex = 5
                }
                val myReactionChanged =
                    event_type_of_reaction == ReactionEventType.I_ADDED_REACTION || event_type_of_reaction == ReactionEventType.I_DELETED_REACTION
                val reactionCount = optInt(array, reactionCountIndex)
                var offset = reactionCountIndex + 1
                val arrayReactionList = ArrayList<VKApiReaction>()
                for (s in 0..<reactionCount) {
                    val reaction = VKApiReaction()
                    reaction.reaction_id = optInt(array, offset + 1)
                    reaction.count = optInt(array, offset + 2)
                    offset += optInt(array, offset) + 1
                    arrayReactionList.add(reaction)
                }
                return ReactionMessageChangeUpdate(
                    event_type_of_reaction,
                    peer_id,
                    conversation_message_id,
                    myReaction.orZero(),
                    myReactionChanged,
                    arrayReactionList
                )
            }

            AbsLongpollEvent.ACTION_USER_WRITE_TEXT_IN_DIALOG -> {
                val w = WriteTextInDialogUpdate(true)
                w.peer_id = optLong(array, 1)
                w.from_ids = optLongArray(array, 2, longArrayOf())
                w.from_ids_count = optInt(array, 3)
                return w
            }

            AbsLongpollEvent.ACTION_USER_WRITE_VOICE_IN_DIALOG -> {
                val v = WriteTextInDialogUpdate(false)
                v.peer_id = optLong(array, 1)
                v.from_ids = optLongArray(array, 2, longArrayOf())
                v.from_ids_count = optInt(array, 3)
                return v
            }

            AbsLongpollEvent.ACTION_USER_IS_ONLINE -> {
                val u = UserIsOnlineUpdate()
                u.userId = -optLong(array, 1)
                u.platform = optInt(array, 2)
                u.timestamp = optLong(array, 3)
                u.app_id = optInt(array, 4)
                return u
            }

            AbsLongpollEvent.ACTION_USER_IS_OFFLINE -> {
                val u1 = UserIsOfflineUpdate()
                u1.userId = -optLong(array, 1)
                u1.isTimeout = optInt(array, 2) != 0
                u1.timestamp = optLong(array, 3)
                u1.app_id = optInt(array, 4)
                return u1
            }

            AbsLongpollEvent.ACTION_MESSAGES_FLAGS_RESET -> {
                val update = MessageFlagsResetUpdate()
                update.messageId = optInt(array, 1)
                update.mask = optInt(array, 2)
                update.peerId = optLong(array, 3)
                return if (update.peerId != 0L && update.messageId != 0) update else null
            }

            AbsLongpollEvent.ACTION_MESSAGES_FLAGS_SET -> {
                val update = MessageFlagsSetUpdate()
                update.messageId = optInt(array, 1)
                update.mask = optInt(array, 2)
                update.peerId = optLong(array, 3)
                return if (update.peerId != 0L && update.messageId != 0) update else null
            }

            AbsLongpollEvent.ACTION_COUNTER_UNREAD_WAS_CHANGED -> {
                val c = BadgeCountChangeUpdate()
                c.count = optInt(array, 1)
                return c
            }

            AbsLongpollEvent.ACTION_SET_INPUT_MESSAGES_AS_READ -> {
                val update = InputMessagesSetReadUpdate()
                update.peerId = optLong(array, 1)
                update.localId = optInt(array, 2)
                update.unreadCount = optInt(array, 3) // undocumented
                return if (update.peerId != 0L) update else null
            }

            AbsLongpollEvent.ACTION_SET_OUTPUT_MESSAGES_AS_READ -> {
                val update = OutputMessagesSetReadUpdate()
                update.peerId = optLong(array, 1)
                update.localId = optInt(array, 2)
                update.unreadCount = optInt(array, 3) // undocumented
                return if (update.peerId != 0L) update else null
            }
        }
        return null
    }

    private fun deserializeAddMessageUpdate(
        array: JsonArray
    ): AddMessageUpdate? {
        val update = AddMessageUpdate()
        val flags = optInt(array, 2)
        update.messageId = optInt(array, 1)
        update.peerId = optLong(array, 3)
        update.timestamp = optLong(array, 4)
        update.text = VKStringUtils.unescape(optString(array, 5))
        update.isOut = Utils.hasFlag(flags, VKApiMessage.FLAG_OUTBOX)
        update.unread = Utils.hasFlag(flags, VKApiMessage.FLAG_UNREAD)
        update.important = Utils.hasFlag(flags, VKApiMessage.FLAG_IMPORTANT)
        update.deleted = Utils.hasFlag(flags, VKApiMessage.FLAG_DELETED)
        val extra = opt(array, 6) as JsonObject?
        if (extra != null) {
            update.from = optLong(extra, "from")
            update.sourceText = optString(extra, "source_text")
            update.sourceAct = optString(extra, "source_act")
            update.sourceMid = optLong(extra, "source_mid")
            update.payload = optString(extra, "payload")
            if (hasObject(extra, "keyboard")) {
                update.keyboard =
                    extra["keyboard"]?.let {
                        kJson.decodeFromJsonElement(
                            VKApiConversation.CurrentKeyboard.serializer(),
                            it
                        )
                    }
            }
        }
        val attachments = opt(array, 7) as JsonObject?
        if (attachments != null) {
            update.hasMedia = attachments.has("attach1_type")
            val fwd = optString(attachments, "fwd")
            val reply = optString(attachments, "reply")
            if (fwd.nonNullNoEmpty()) {
                update.fwds = parseLineWithSeparators(fwd, ",")
            }
            if (reply.nonNullNoEmpty()) {
                update.reply = reply
            }
        }
        update.random_id = optString(array, 8) // ok
        update.conversationMessageId = optInt(array, 9)
        update.edit_time = optLong(array, 10)
        if (update.from == 0L && !Peer.isGroupChat(update.peerId) && !Peer.isContactChat(update.peerId) && !update.isOut) {
            update.from = update.peerId
        }
        return if (update.messageId != 0) update else null
    }

    companion object {
        internal fun parseLineWithSeparators(line: String?, separator: String): ArrayList<String>? {
            if (line.isNullOrEmpty()) {
                return null
            }
            val tokens = line.split(separator.toRegex()).toTypedArray()
            val ids = ArrayList<String>()
            Collections.addAll(ids, *tokens)
            return ids
        }
    }
}
