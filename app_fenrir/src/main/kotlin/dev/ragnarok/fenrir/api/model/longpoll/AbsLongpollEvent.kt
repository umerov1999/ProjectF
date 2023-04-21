package dev.ragnarok.fenrir.api.model.longpoll

import dev.ragnarok.fenrir.api.adapters.LongpollUpdateDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = LongpollUpdateDtoAdapter::class)
open class AbsLongpollEvent(val action: Int) {
    companion object {
        const val ACTION_MESSAGES_FLAGS_SET = 2
        const val ACTION_MESSAGES_FLAGS_RESET = 3
        const val ACTION_MESSAGE_ADDED = 4
        const val ACTION_MESSAGE_EDITED = 5
        const val ACTION_SET_INPUT_MESSAGES_AS_READ = 6
        const val ACTION_SET_OUTPUT_MESSAGES_AS_READ = 7
        const val ACTION_USER_IS_ONLINE = 8
        const val ACTION_USER_IS_OFFLINE = 9
        const val ACTION_MESSAGE_CHANGED = 18
        const val ACTION_USER_WRITE_TEXT_IN_DIALOG = 63
        const val ACTION_USER_WRITE_VOICE_IN_DIALOG = 64
        const val ACTION_COUNTER_UNREAD_WAS_CHANGED = 80
    }
}