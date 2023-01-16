package dev.ragnarok.fenrir.api.model.longpoll

import dev.ragnarok.fenrir.api.model.VKApiConversation.CurrentKeyboard
import dev.ragnarok.fenrir.nonNullNoEmpty

class AddMessageUpdate : AbsLongpollEvent(ACTION_MESSAGE_ADDED) {
    var messageId = 0
    var timestamp: Long = 0
    var text: String? = null
    var from = 0L
    var isOut = false
    var unread = false
    var important = false
    var deleted = false
    var hasMedia = false
    var sourceText: String? = null
    var sourceAct: String? = null
    var sourceMid = 0L
    var fwds: ArrayList<String>? = null
    var keyboard: CurrentKeyboard? = null
    var payload: String? = null
    var reply: String? = null
    var peerId = 0L
    var random_id: String? = null
    var edit_time: Long = 0

    fun hasMedia(): Boolean {
        return hasMedia
    }

    fun hasFwds(): Boolean {
        return fwds.nonNullNoEmpty()
    }

    fun hasReply(): Boolean {
        return reply.nonNullNoEmpty()
    }

    val isServiceMessage: Boolean
        get() = sourceAct.nonNullNoEmpty()
    val isFull: Boolean
        get() = !hasMedia() && !hasFwds() && !hasReply() && !isServiceMessage
}