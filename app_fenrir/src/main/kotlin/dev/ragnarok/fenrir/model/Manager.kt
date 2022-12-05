package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Manager : Parcelable {
    val user: User?
    val role: String?
    var isDisplayAsContact = false
        private set
    var contactInfo: ContactInfo? = null
        private set

    constructor(user: User?, role: String?) {
        this.user = user
        this.role = role
    }

    internal constructor(`in`: Parcel) {
        user = `in`.readTypedObjectCompat(User.CREATOR)
        isDisplayAsContact = `in`.getBoolean()
        role = `in`.readString()
    }

    fun setContactInfo(contactInfo: ContactInfo?): Manager {
        this.contactInfo = contactInfo
        return this
    }

    fun setDisplayAsContact(displayAsContact: Boolean): Manager {
        isDisplayAsContact = displayAsContact
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedObjectCompat(user, flags)
        dest.putBoolean(isDisplayAsContact)
        dest.writeString(role)
    }

    companion object CREATOR : Parcelable.Creator<Manager> {
        override fun createFromParcel(parcel: Parcel): Manager {
            return Manager(parcel)
        }

        override fun newArray(size: Int): Array<Manager?> {
            return arrayOfNulls(size)
        }
    }
}