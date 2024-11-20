package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.interfaces.IdentificableOwner
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class AppChatUser : Parcelable, IdentificableOwner {
    val member: Owner?
    val invitedBy: Long
    var join_date: Long = 0
        private set
    var inviter: Owner? = null
        private set
    var canRemove = false
        private set
    var isAdmin = false
        private set
    var isOwner = false
        private set

    constructor(member: Owner?, invitedBy: Long) {
        this.member = member
        this.invitedBy = invitedBy
    }

    internal constructor(parcel: Parcel) {
        inviter =
            ParcelableOwnerWrapper.readOwner(parcel)
        member =
            ParcelableOwnerWrapper.readOwner(parcel)
        invitedBy = parcel.readLong()
        canRemove = parcel.getBoolean()
        join_date = parcel.readLong()
        isAdmin = parcel.getBoolean()
        isOwner = parcel.getBoolean()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        ParcelableOwnerWrapper.writeOwner(dest, flags, inviter)
        ParcelableOwnerWrapper.writeOwner(dest, flags, member)
        dest.writeLong(invitedBy)
        dest.putBoolean(canRemove)
        dest.writeLong(join_date)
        dest.putBoolean(isAdmin)
        dest.putBoolean(isOwner)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setCanRemove(canRemove: Boolean): AppChatUser {
        this.canRemove = canRemove
        return this
    }

    fun setInviter(inviter: Owner?): AppChatUser {
        this.inviter = inviter
        return this
    }

    override fun getOwnerObjectId(): Long {
        return member?.ownerId ?: 0
    }

    fun setJoin_date(join_date: Long): AppChatUser {
        this.join_date = join_date
        return this
    }

    fun setAdmin(admin: Boolean): AppChatUser {
        isAdmin = admin
        return this
    }

    fun setOwner(owner: Boolean): AppChatUser {
        isOwner = owner
        return this
    }

    companion object CREATOR : Parcelable.Creator<AppChatUser> {
        override fun createFromParcel(parcel: Parcel): AppChatUser {
            return AppChatUser(parcel)
        }

        override fun newArray(size: Int): Array<AppChatUser?> {
            return arrayOfNulls(size)
        }
    }
}