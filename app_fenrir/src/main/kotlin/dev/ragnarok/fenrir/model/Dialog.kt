package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class Dialog : Identificable, Parcelable {
    var peerId = 0
        private set
    private var title: String? = null
    var unreadCount = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var message: Message? = null
        private set
    var interlocutor: Owner? = null
        private set
    var lastMessageId = 0
        private set
    var inRead = 0
        private set
    var outRead = 0
        private set
    var isGroupChannel = false
        private set
    var major_id = 0
        private set
    private var minor_id = 0

    constructor()
    internal constructor(`in`: Parcel) {
        peerId = `in`.readInt()
        title = `in`.readString()
        unreadCount = `in`.readInt()
        photo50 = `in`.readString()
        photo100 = `in`.readString()
        photo200 = `in`.readString()
        message = `in`.readTypedObjectCompat(Message.CREATOR)
        val interlocutorIsNull = `in`.getBoolean()
        if (!interlocutorIsNull) {
            interlocutor = Owner.readOwnerFromParcel(`in`)
        }
        lastMessageId = `in`.readInt()
        inRead = `in`.readInt()
        outRead = `in`.readInt()
        major_id = `in`.readInt()
        minor_id = `in`.readInt()
        isGroupChannel = `in`.getBoolean()
    }

    fun setPeerId(peerId: Int): Dialog {
        this.peerId = peerId
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): Dialog {
        this.title = title
        return this
    }

    fun setUnreadCount(unreadCount: Int): Dialog {
        this.unreadCount = unreadCount
        return this
    }

    fun setPhoto50(photo50: String?): Dialog {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): Dialog {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): Dialog {
        this.photo200 = photo200
        return this
    }

    val isChat: Boolean
        get() = Peer.isGroupChat(peerId)
    val isUser: Boolean
        get() = Peer.isUser(peerId)
    val isGroup: Boolean
        get() = Peer.isGroup(peerId)

    val isContact: Boolean
        get() = Peer.isContactChat(peerId)

    fun setMessage(message: Message?): Dialog {
        this.message = message
        return this
    }

    fun setInterlocutor(interlocutor: Owner?): Dialog {
        this.interlocutor = interlocutor
        return this
    }

    fun setLastMessageId(lastMessageId: Int): Dialog {
        this.lastMessageId = lastMessageId
        return this
    }

    val isLastMessageOut: Boolean
        get() = message?.isOut == true

    @ChatAction
    val lastMessageAction: Int
        get() = message?.action ?: ChatAction.NO_ACTION

    fun hasForwardMessages(): Boolean {
        return forwardMessagesCount > 0
    }

    val forwardMessagesCount: Int
        get() = message?.forwardMessagesCount.orZero()

    fun hasAttachments(): Boolean {
        return message?.isHasAttachments == true
    }

    val lastMessageBody: String?
        get() = if (message == null) "..." else if (message?.cryptStatus == CryptStatus.DECRYPTED) message?.decryptedBody else message?.body

    fun getSenderShortName(context: Context): String {
        var targerText: String? = null
        if (interlocutor is User) {
            targerText = (interlocutor as User).firstName
        } else if (interlocutor is Community) {
            targerText = (interlocutor as Community).fullName
        }
        return targerText ?: context.getString(R.string.unknown_first_name)
    }

    fun getDisplayTitle(context: Context): String? {
        return when (Peer.getType(peerId)) {
            Peer.USER, Peer.GROUP -> {
                val custom = get().other().getUserNameChanges(peerId)
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
            else -> throw IllegalStateException("Unknown peer id: $peerId")
        }
    }

    fun getDisplayTitle(): String? {
        return when (Peer.getType(peerId)) {
            Peer.USER, Peer.GROUP -> {
                val custom = get().other().getUserNameChanges(peerId)
                if (custom.nonNullNoEmpty()) {
                    return custom
                }
                if (interlocutor == null && title.isNullOrEmpty()) null
                else if (title.nonNullNoEmpty()) {
                    title
                } else interlocutor?.fullName
            }
            Peer.CHAT, Peer.CONTACT -> title
            else -> throw IllegalStateException("Unknown peer id: $peerId")
        }
    }

    val lastMessageDate: Long
        get() = message?.date.orZero()

    //if (isEmpty(img) && interlocutor != null) {
    //img = interlocutor.getMaxSquareAvatar();
    // }
    val imageUrl: String?
        get() {
            if (Peer.getType(peerId) == Peer.CHAT || Peer.getType(peerId) == Peer.CONTACT) {

                //if (isEmpty(img) && interlocutor != null) {
                //img = interlocutor.getMaxSquareAvatar();
                // }
                return firstNonEmptyString(photo200, photo100, photo50)
            }
            return interlocutor?.maxSquareAvatar
        }

    fun setInRead(inRead: Int): Dialog {
        this.inRead = inRead
        return this
    }

    val isLastMessageRead: Boolean
        get() = if (isLastMessageOut) lastMessageId <= outRead else lastMessageId <= inRead

    fun setOutRead(outRead: Int): Dialog {
        this.outRead = outRead
        return this
    }

    fun setGroupChannel(groupChannel: Boolean): Dialog {
        isGroupChannel = groupChannel
        return this
    }

    fun setMajor_id(major_id: Int): Dialog {
        this.major_id = major_id
        return this
    }

    fun getMinor_id(): Int {
        return if (minor_id == 0) {
            lastMessageId
        } else minor_id
    }

    fun setMinor_id(minor_id: Int): Dialog {
        this.minor_id = minor_id
        return this
    }

    override fun getObjectId(): Int {
        return peerId
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(peerId)
        dest.writeString(title)
        dest.writeInt(unreadCount)
        dest.writeString(photo50)
        dest.writeString(photo100)
        dest.writeString(photo200)
        dest.writeTypedObjectCompat(message, flags)
        dest.writeInt(if (interlocutor == null) 1 else 0)
        interlocutor.requireNonNull {
            Owner.writeOwnerToParcel(it, dest, flags)
        }
        dest.writeInt(lastMessageId)
        dest.writeInt(inRead)
        dest.writeInt(outRead)
        dest.writeInt(major_id)
        dest.writeInt(minor_id)
        dest.putBoolean(isGroupChannel)
    }

    companion object CREATOR : Parcelable.Creator<Dialog> {
        override fun createFromParcel(parcel: Parcel): Dialog {
            return Dialog(parcel)
        }

        override fun newArray(size: Int): Array<Dialog?> {
            return arrayOfNulls(size)
        }
    }
}