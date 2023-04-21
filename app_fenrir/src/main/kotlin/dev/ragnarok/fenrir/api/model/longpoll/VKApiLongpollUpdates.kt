package dev.ragnarok.fenrir.api.model.longpoll

import dev.ragnarok.fenrir.api.adapters.LongpollUpdatesDtoAdapter
import dev.ragnarok.fenrir.util.Utils.safeCountOfMultiple
import kotlinx.serialization.Serializable

@Serializable(with = LongpollUpdatesDtoAdapter::class)
class VKApiLongpollUpdates {
    // TODO Message edit update
    //{"ts":1841741106,"updates":[[5,1200880,51,26632922,1528116889,"и тд",{"title":""},{},1000771599]]}
    var ts: Long = 0
    var failed = 0
    var write_text_in_dialog_updates: MutableList<WriteTextInDialogUpdate>? = null
    var add_message_updates: MutableList<AddMessageUpdate>? = null
    var user_is_online_updates: MutableList<UserIsOnlineUpdate>? = null
    var user_is_offline_updates: MutableList<UserIsOfflineUpdate>? = null
    var message_flags_reset_updates: MutableList<MessageFlagsResetUpdate>? = null
    var message_flags_set_updates: MutableList<MessageFlagsSetUpdate>? = null
    var input_messages_set_read_updates: MutableList<InputMessagesSetReadUpdate>? = null
    var output_messages_set_read_updates: MutableList<OutputMessagesSetReadUpdate>? = null
    var badge_count_change_updates: MutableList<BadgeCountChangeUpdate>? = null

    //public VkApiLongpollUpdates(int account_id) {
    //    this.account_id = account_id;
    //}
    fun putUpdate(update: AbsLongpollEvent) {
        when (update.action) {
            AbsLongpollEvent.ACTION_MESSAGES_FLAGS_SET -> message_flags_set_updates =
                addAndReturn(message_flags_set_updates, update as MessageFlagsSetUpdate)

            AbsLongpollEvent.ACTION_MESSAGES_FLAGS_RESET -> message_flags_reset_updates =
                addAndReturn(message_flags_reset_updates, update as MessageFlagsResetUpdate)

            AbsLongpollEvent.ACTION_MESSAGE_EDITED, AbsLongpollEvent.ACTION_MESSAGE_CHANGED, AbsLongpollEvent.ACTION_MESSAGE_ADDED -> add_message_updates =
                addAndReturn(add_message_updates, update as AddMessageUpdate)

            AbsLongpollEvent.ACTION_USER_IS_ONLINE -> user_is_online_updates =
                addAndReturn(user_is_online_updates, update as UserIsOnlineUpdate)

            AbsLongpollEvent.ACTION_USER_IS_OFFLINE -> user_is_offline_updates =
                addAndReturn(user_is_offline_updates, update as UserIsOfflineUpdate)

            AbsLongpollEvent.ACTION_USER_WRITE_TEXT_IN_DIALOG, AbsLongpollEvent.ACTION_USER_WRITE_VOICE_IN_DIALOG -> write_text_in_dialog_updates =
                addAndReturn(write_text_in_dialog_updates, update as WriteTextInDialogUpdate)

            AbsLongpollEvent.ACTION_SET_INPUT_MESSAGES_AS_READ -> input_messages_set_read_updates =
                addAndReturn(input_messages_set_read_updates, update as InputMessagesSetReadUpdate)

            AbsLongpollEvent.ACTION_SET_OUTPUT_MESSAGES_AS_READ -> output_messages_set_read_updates =
                addAndReturn(
                    output_messages_set_read_updates,
                    update as OutputMessagesSetReadUpdate
                )

            AbsLongpollEvent.ACTION_COUNTER_UNREAD_WAS_CHANGED -> badge_count_change_updates =
                addAndReturn(badge_count_change_updates, update as BadgeCountChangeUpdate)
        }
    }

    val updatesCount: Int
        get() = safeCountOfMultiple(
            write_text_in_dialog_updates,
            add_message_updates,
            user_is_online_updates,
            user_is_offline_updates,
            message_flags_reset_updates,
            message_flags_set_updates,
            input_messages_set_read_updates,
            output_messages_set_read_updates,
            badge_count_change_updates
        )
    val isEmpty: Boolean
        get() = updatesCount == 0

    override fun toString(): String {
        return "Longpolling updates, count: $updatesCount, failed: $failed"
    }

    companion object {
        private fun <T : AbsLongpollEvent> crateAndAppend(item: T): MutableList<T> {
            val data: MutableList<T> = ArrayList(1)
            data.add(item)
            return data
        }

        internal fun <T : AbsLongpollEvent> addAndReturn(
            data: MutableList<T>?,
            item: T
        ): MutableList<T> {
            return if (data == null) {
                crateAndAppend(item)
            } else {
                data.add(item)
                data
            }
        }
    }
}