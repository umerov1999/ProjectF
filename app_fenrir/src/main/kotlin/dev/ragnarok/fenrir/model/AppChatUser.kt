package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class AppChatUser : Parcelable, Identificable {
    private val member: Owner?
    private val invitedBy: Int
    private var join_date: Long = 0
    private var inviter: Owner? = null
    private var canRemove = false
    private var isAdmin = false
    private var isOwner = false

    constructor(member: Owner?, invitedBy: Int) {
        this.member = member
        this.invitedBy = invitedBy
    }

    internal constructor(parcel: Parcel) {
        inviter =
            parcel.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)!!
                .get()
        member =
            parcel.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)!!
                .get()
        invitedBy = parcel.readInt()
        canRemove = parcel.getBoolean()
        join_date = parcel.readLong()
        isAdmin = parcel.getBoolean()
        isOwner = parcel.getBoolean()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedObjectCompat(ParcelableOwnerWrapper(inviter), flags)
        dest.writeTypedObjectCompat(ParcelableOwnerWrapper(member), flags)
        dest.writeInt(invitedBy)
        dest.putBoolean(canRemove)
        dest.writeLong(join_date)
        dest.putBoolean(isAdmin)
        dest.putBoolean(isOwner)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isCanRemove(): Boolean {
        return canRemove
    }

    fun setCanRemove(canRemove: Boolean): AppChatUser {
        this.canRemove = canRemove
        return this
    }

    fun getInviter(): Owner? {
        return inviter
    }

    fun setInviter(inviter: Owner?): AppChatUser {
        this.inviter = inviter
        return this
    }

    fun getInvitedBy(): Int {
        return invitedBy
    }

    fun getMember(): Owner? {
        return member
    }

    override fun getObjectId(): Int {
        return member?.ownerId ?: 0
    }

    fun getJoin_date(): Long {
        return join_date
    }

    fun setJoin_date(join_date: Long): AppChatUser {
        this.join_date = join_date
        return this
    }

    fun isAdmin(): Boolean {
        return isAdmin
    }

    fun setAdmin(admin: Boolean): AppChatUser {
        isAdmin = admin
        return this
    }

    fun isOwner(): Boolean {
        return isOwner
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