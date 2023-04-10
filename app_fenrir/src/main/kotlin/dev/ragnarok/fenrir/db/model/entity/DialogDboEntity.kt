package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class DialogDboEntity(val peerId: Long) : DboEntity() {
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
    var inRead = 0
        private set
    var outRead = 0
        private set
    var message: MessageDboEntity? = null
        private set
    var currentKeyboard: KeyboardEntity? = null
        private set
    var pinned: MessageDboEntity? = null
        private set
    var lastMessageId = 0
        private set
    var acl = 0
        private set
    var isGroupChannel = false
        private set
    var major_id = 0
        private set
    var minor_id = 0
        private set

    fun setAcl(acl: Int): DialogDboEntity {
        this.acl = acl
        return this
    }

    fun setPinned(pinned: MessageDboEntity?): DialogDboEntity {
        this.pinned = pinned
        return this
    }

    fun setInRead(inRead: Int): DialogDboEntity {
        this.inRead = inRead
        return this
    }

    fun setOutRead(outRead: Int): DialogDboEntity {
        this.outRead = outRead
        return this
    }

    fun setTitle(title: String?): DialogDboEntity {
        this.title = title
        return this
    }

    fun setCurrentKeyboard(currentKeyboard: KeyboardEntity?): DialogDboEntity {
        this.currentKeyboard = currentKeyboard
        return this
    }

    fun setUnreadCount(unreadCount: Int): DialogDboEntity {
        this.unreadCount = unreadCount
        return this
    }

    fun setPhoto50(photo50: String?): DialogDboEntity {
        this.photo50 = photo50
        return this
    }

    fun simplify(): SimpleDialogEntity {
        return SimpleDialogEntity(peerId)
            .setTitle(title)
            .setPhoto200(photo200)
            .setPhoto100(photo100)
            .setPhoto50(photo50)
            .setOutRead(outRead)
            .setInRead(inRead)
            .setUnreadCount(unreadCount)
            .setPinned(pinned)
            .setLastMessageId(lastMessageId)
            .setAcl(acl)
            .setCurrentKeyboard(currentKeyboard)
            .setGroupChannel(isGroupChannel)
            .setMajor_id(major_id)
            .setMinor_id(minor_id)
    }

    fun setPhoto100(photo100: String?): DialogDboEntity {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): DialogDboEntity {
        this.photo200 = photo200
        return this
    }

    fun setMessage(message: MessageDboEntity?): DialogDboEntity {
        this.message = message
        return this
    }

    fun setLastMessageId(lastMessageId: Int): DialogDboEntity {
        this.lastMessageId = lastMessageId
        return this
    }

    fun setGroupChannel(groupChannel: Boolean): DialogDboEntity {
        isGroupChannel = groupChannel
        return this
    }

    fun setMajor_id(major_id: Int): DialogDboEntity {
        this.major_id = major_id
        return this
    }

    fun setMinor_id(minor_id: Int): DialogDboEntity {
        this.minor_id = minor_id
        return this
    }
}