package dev.ragnarok.fenrir.model

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class Conversation(val id: Long) {
    var title: String? = null
        private set
    var unreadCount = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set

    /**
     * ID of the last read incoming message.
     */
    var inRead = 0
        private set

    /**
     * ID of the last read outcoming message.
     */
    var outRead = 0
        private set
    var interlocutor: Owner? = null
        private set
    var currentKeyboard: Keyboard? = null
        private set
    var pinned: Message? = null
        private set
    var isGroupChannel = false
        private set
    var acl = 0
        private set
    var major_id = 0
        private set
    var minor_id = 0
        private set

    fun setAcl(acl: Int): Conversation {
        this.acl = acl
        return this
    }

    fun setPinned(pinned: Message?): Conversation {
        this.pinned = pinned
        return this
    }

    fun setInterlocutor(interlocutor: Owner?): Conversation {
        this.interlocutor = interlocutor
        return this
    }

    val imageUrl: String?
        get() {
            if (Peer.getType(id) == Peer.CHAT || Peer.getType(id) == Peer.CONTACT) {

                //if (isEmpty(img) && interlocutor != null) {
                //img = interlocutor.getMaxSquareAvatar();
                // }
                return firstNonEmptyString(photo200, photo100, photo50)
            }
            return interlocutor?.maxSquareAvatar
        }

    fun getDisplayTitle(context: Context): String? {
        return when (Peer.getType(id)) {
            Peer.USER, Peer.GROUP -> {
                val custom = get().main().getUserNameChanges(id)
                if (custom.nonNullNoEmpty()) {
                    return custom
                }
                if (interlocutor == null && title.isNullOrEmpty()) context.getString(R.string.unknown_first_name) + " " + context.getString(
                    R.string.unknown_last_name
                )
                else if (title.nonNullNoEmpty()) {
                    title
                } else interlocutor?.fullName
            }

            Peer.CHAT, Peer.CONTACT -> title
            else -> throw IllegalStateException("Unknown peer id: $id")
        }
    }

    fun getDisplayTitle(): String? {
        return when (Peer.getType(id)) {
            Peer.USER, Peer.GROUP -> {
                val custom = get().main().getUserNameChanges(id)
                if (custom.nonNullNoEmpty()) {
                    return custom
                }
                if (interlocutor == null && title.isNullOrEmpty()) null
                else if (title.nonNullNoEmpty()) {
                    title
                } else interlocutor?.fullName
            }

            Peer.CHAT, Peer.CONTACT -> title
            else -> throw IllegalStateException("Unknown peer id: $id")
        }
    }

    fun setTitle(title: String?): Conversation {
        this.title = title
        return this
    }

    fun setCurrentKeyboard(currentKeyboard: Keyboard?): Conversation {
        this.currentKeyboard = currentKeyboard
        return this
    }

    fun setUnreadCount(unreadCount: Int): Conversation {
        this.unreadCount = unreadCount
        return this
    }

    fun setPhoto50(photo50: String?): Conversation {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): Conversation {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): Conversation {
        this.photo200 = photo200
        return this
    }

    fun setInRead(inRead: Int): Conversation {
        this.inRead = inRead
        return this
    }

    fun setOutRead(outRead: Int): Conversation {
        this.outRead = outRead
        return this
    }

    fun setGroupChannel(isGroupChannel: Boolean): Conversation {
        this.isGroupChannel = isGroupChannel
        return this
    }

    fun setMajor_id(major_id: Int): Conversation {
        this.major_id = major_id
        return this
    }

    fun setMinor_id(minor_id: Int): Conversation {
        this.minor_id = minor_id
        return this
    }

    object AclFlags {
        const val CAN_INVITE = 1
        const val CAN_CHANGE_INFO = 2
        const val CAN_CHANGE_PIN = 4
        const val CAN_PROMOTE_USERS = 8
        const val CAN_SEE_INVITE_LINK = 16
        const val CAN_CHANGE_INVITE_LINK = 32
    }
}