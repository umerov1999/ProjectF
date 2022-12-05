package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.model.Conversation
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils

object MapUtil {
    fun calculateConversationAcl(conversation: VKApiConversation?): Int {
        conversation ?: return 0
        var result = 0
        conversation.settings?.acl.requireNonNull {
            val acl = it
            result =
                Utils.addFlagIf(result, Conversation.AclFlags.CAN_CHANGE_INFO, acl.can_change_info)
            result = Utils.addFlagIf(
                result,
                Conversation.AclFlags.CAN_CHANGE_INVITE_LINK,
                acl.can_change_invite_link
            )
            result =
                Utils.addFlagIf(result, Conversation.AclFlags.CAN_CHANGE_PIN, acl.can_change_pin)
            result = Utils.addFlagIf(result, Conversation.AclFlags.CAN_INVITE, acl.can_invite)
            result = Utils.addFlagIf(
                result,
                Conversation.AclFlags.CAN_PROMOTE_USERS,
                acl.can_promote_users
            )
            result = Utils.addFlagIf(
                result,
                Conversation.AclFlags.CAN_SEE_INVITE_LINK,
                acl.can_see_invite_link
            )
        }
        return result
    }

    inline fun <reified O, R> mapAndAdd(
        orig: Collection<O>?,
        function: (O) -> R,
        target: MutableCollection<R>
    ) {
        if (orig != null) {
            for (o in orig) {
                target.add(function.invoke(o))
            }
        }
    }

    inline fun <reified O, reified R> mapAllMutable(
        orig: Collection<O>?,
        function: (O) -> R
    ): MutableList<R> {
        return if (orig.nonNullNoEmpty()) {
            if (orig.size == 1) {
                return mutableListOf(function.invoke(orig.iterator().next()))
            }
            val list: MutableList<R> = ArrayList(orig.size)
            for (o in orig) {
                list.add(function.invoke(o))
            }
            return list
        } else {
            ArrayList(0)
        }
    }

    inline fun <reified O, reified R> mapAll(orig: Collection<O>?, function: (O) -> R?): List<R> {
        return if (orig.nonNullNoEmpty()) {
            if (orig.size == 1) {
                function.invoke(orig.iterator().next())?.let {
                    listOf(it)
                } ?: emptyList()
            } else {
                val list: MutableList<R> = ArrayList(orig.size)
                for (o in orig) {
                    function.invoke(o)?.let { list.add(it) }
                }
                return list
            }
        } else {
            emptyList()
        }
    }
}