package dev.ragnarok.fenrir.model

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class Conversation(private val id: Long) {
    private var title: String? = null
    private var unreadCount = 0
    private var photo50: String? = null
    private var photo100: String? = null
    private var photo200: String? = null

    /**
     * ID of the last read incoming message.
     */
    private var inRead = 0

    /**
     * ID of the last read outcoming message.
     */
    private var outRead = 0
    private var interlocutor: Owner? = null
    private var currentKeyboard: Keyboard? = null
    private var pinned: Message? = null
    private var isGroupChannel = false
    private var acl = 0
    private var major_id = 0
    private var minor_id = 0
    fun getAcl(): Int {
        return acl
    }

    fun setAcl(acl: Int): Conversation {
        this.acl = acl
        return this
    }

    fun getPinned(): Message? {
        return pinned
    }

    fun setPinned(pinned: Message?): Conversation {
        this.pinned = pinned
        return this
    }

    fun getInterlocutor(): Owner? {
        return interlocutor
    }

    fun setInterlocutor(interlocutor: Owner?): Conversation {
        this.interlocutor = interlocutor
        return this
    }

    fun getId(): Long {
        return id
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
                val custom = get().other().getUserNameChanges(id)
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
                val custom = get().other().getUserNameChanges(id)
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

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): Conversation {
        this.title = title
        return this
    }

    fun getCurrentKeyboard(): Keyboard? {
        return currentKeyboard
    }

    fun setCurrentKeyboard(currentKeyboard: Keyboard?): Conversation {
        this.currentKeyboard = currentKeyboard
        return this
    }

    fun getUnreadCount(): Int {
        return unreadCount
    }

    fun setUnreadCount(unreadCount: Int): Conversation {
        this.unreadCount = unreadCount
        return this
    }

    fun getPhoto50(): String? {
        return photo50
    }

    fun setPhoto50(photo50: String?): Conversation {
        this.photo50 = photo50
        return this
    }

    fun getPhoto100(): String? {
        return photo100
    }

    fun setPhoto100(photo100: String?): Conversation {
        this.photo100 = photo100
        return this
    }

    fun getPhoto200(): String? {
        return photo200
    }

    fun setPhoto200(photo200: String?): Conversation {
        this.photo200 = photo200
        return this
    }

    fun getInRead(): Int {
        return inRead
    }

    fun setInRead(inRead: Int): Conversation {
        this.inRead = inRead
        return this
    }

    fun getOutRead(): Int {
        return outRead
    }

    fun setOutRead(outRead: Int): Conversation {
        this.outRead = outRead
        return this
    }

    fun isGroupChannel(): Boolean {
        return isGroupChannel
    }

    fun setGroupChannel(isGroupChannel: Boolean): Conversation {
        this.isGroupChannel = isGroupChannel
        return this
    }

    fun getMajor_id(): Int {
        return major_id
    }

    fun setMajor_id(major_id: Int): Conversation {
        this.major_id = major_id
        return this
    }

    fun getMinor_id(): Int {
        return minor_id
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