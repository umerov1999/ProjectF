package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ContactConversation : Parcelable {
    val id: Int
    var title: String? = null
        private set
    var photo: String? = null
        private set
    var phone: String? = null
        private set
    var lastSeen: Long = 0
        private set
    var last_seen_status: String? = null
        private set
    var isContact: Boolean = false
        private set

    constructor(id: Int) {
        this.id = id
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        title = `in`.readString()
        isContact = `in`.getBoolean()
        photo = `in`.readString()
        phone = `in`.readString()
        lastSeen = `in`.readLong()
        last_seen_status = `in`.readString()
    }

    fun setIsContact(value: Boolean): ContactConversation {
        isContact = value
        return this
    }

    fun setTitle(value: String?): ContactConversation {
        title = value
        return this
    }

    fun setPhoto(value: String?): ContactConversation {
        photo = value
        return this
    }

    fun setPhone(value: String?): ContactConversation {
        phone = value
        return this
    }

    fun setLastSeen(value: Long): ContactConversation {
        lastSeen = value
        return this
    }

    fun setLast_seen_status(value: String?): ContactConversation {
        last_seen_status = value
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.putBoolean(isContact)
        parcel.writeString(photo)
        parcel.writeString(phone)
        parcel.writeLong(lastSeen)
        parcel.writeString(last_seen_status)
    }

    companion object CREATOR : Parcelable.Creator<ContactConversation> {
        override fun createFromParcel(parcel: Parcel): ContactConversation {
            return ContactConversation(parcel)
        }

        override fun newArray(size: Int): Array<ContactConversation?> {
            return arrayOfNulls(size)
        }
    }
}