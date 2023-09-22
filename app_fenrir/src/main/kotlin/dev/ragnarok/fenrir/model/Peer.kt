package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings.get
import kotlin.math.abs

class Peer : Parcelable {
    val id: Long
    private var title: String? = null
    var avaUrl: String? = null
        private set

    constructor(id: Long) {
        this.id = id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readLong()
        title = parcel.readString()
        avaUrl = parcel.readString()
    }

    fun getTitle(): String? {
        if (isUser(id)) {
            val custom = get().main().getUserNameChanges(id)
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
        dest.writeLong(id)
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
            override fun createFromParcel(parcel: Parcel): Peer {
                return Peer(parcel)
            }

            override fun newArray(size: Int): Array<Peer?> {
                return arrayOfNulls(size)
            }
        }
        const val USER = 1
        const val GROUP = 2
        const val CHAT = 3
        const val CONTACT = 4
        fun getType(peerId: Long): Int {
            if (peerId > VKApiMessage.CHAT_PEER) {
                return CHAT
            } else if (peerId > VKApiMessage.CONTACT_PEER) {
                return CONTACT
            }
            return if (peerId < 0) {
                GROUP
            } else USER
        }

        fun fromChatId(chatId: Long): Long {
            return chatId + VKApiMessage.CHAT_PEER
        }

        fun fromContactId(chatId: Long): Long {
            return chatId + VKApiMessage.CONTACT_PEER
        }

        fun toChatId(peerId: Long): Long {
            return peerId - VKApiMessage.CHAT_PEER
        }

        fun toContactId(peerId: Long): Long {
            return peerId - VKApiMessage.CONTACT_PEER
        }

        fun fromOwnerId(ownerId: Long): Long {
            return ownerId
        }

        fun toOwnerId(peerId: Long): Long {
            return peerId
        }

        fun toUserId(peerId: Long): Long {
            return peerId
        }

        fun isGroupChat(peerId: Long): Boolean {
            return peerId > VKApiMessage.CHAT_PEER
        }

        fun isContactChat(peerId: Long): Boolean {
            return peerId in (VKApiMessage.CONTACT_PEER + 1) until VKApiMessage.CHAT_PEER
        }

        fun fromUserId(userId: Long): Long {
            return userId
        }

        fun isUser(peerId: Long): Boolean {
            return getType(peerId) == USER
        }

        fun isGroup(peerId: Long): Boolean {
            return getType(peerId) == GROUP
        }

        fun fromGroupId(groupId: Long): Long {
            return -abs(groupId)
        }
    }
}