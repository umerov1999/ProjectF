package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings.get
import kotlin.math.abs

class Peer : Parcelable {
    val id: Int
    private var title: String? = null
    var avaUrl: String? = null
        private set

    constructor(id: Int) {
        this.id = id
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        title = `in`.readString()
        avaUrl = `in`.readString()
    }

    fun getTitle(): String? {
        if (isUser(id)) {
            val custom = get().other().getUserNameChanges(id)
            if (custom.nonNullNoEmpty()) {
                return custom
            }
        }
        return title
    }

    fun setTitle(title: String?): Peer {
        this.title = title
        return this
    }

    fun setAvaUrl(avaUrl: String?): Peer {
        this.avaUrl = avaUrl
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(title)
        dest.writeString(avaUrl)
    }

    companion object {
        // VKAPi
        // Для пользователя: id пользователя.
        // Для групповой беседы: 2000000000 + id беседы.
        // Для сообщества: -id сообщества.
        @JvmField
        val CREATOR: Parcelable.Creator<Peer> = object : Parcelable.Creator<Peer> {
            override fun createFromParcel(`in`: Parcel): Peer {
                return Peer(`in`)
            }

            override fun newArray(size: Int): Array<Peer?> {
                return arrayOfNulls(size)
            }
        }
        const val USER = 1
        const val GROUP = 2
        const val CHAT = 3
        const val CONTACT = 4
        fun getType(peerId: Int): Int {
            if (peerId > VKApiMessage.CHAT_PEER) {
                return CHAT
            } else if (peerId > VKApiMessage.CONTACT_PEER) {
                return CONTACT
            }
            return if (peerId < 0) {
                GROUP
            } else USER
        }

        fun fromChatId(chatId: Int): Int {
            return chatId + VKApiMessage.CHAT_PEER
        }

        fun fromContactId(chatId: Int): Int {
            return chatId + VKApiMessage.CONTACT_PEER
        }

        fun toChatId(peerId: Int): Int {
            return peerId - VKApiMessage.CHAT_PEER
        }

        fun toContactId(peerId: Int): Int {
            return peerId - VKApiMessage.CONTACT_PEER
        }

        fun fromOwnerId(ownerId: Int): Int {
            return ownerId
        }

        fun toOwnerId(peerId: Int): Int {
            return peerId
        }

        fun toUserId(peerId: Int): Int {
            return peerId
        }

        fun isGroupChat(peerId: Int): Boolean {
            return peerId > VKApiMessage.CHAT_PEER
        }

        fun isContactChat(peerId: Int): Boolean {
            return peerId in (VKApiMessage.CONTACT_PEER + 1) until VKApiMessage.CHAT_PEER
        }

        fun fromUserId(userId: Int): Int {
            return userId
        }

        fun isUser(peerId: Int): Boolean {
            return getType(peerId) == USER
        }

        fun isGroup(peerId: Int): Boolean {
            return getType(peerId) == GROUP
        }

        fun fromGroupId(groupId: Int): Int {
            return -abs(groupId)
        }
    }
}