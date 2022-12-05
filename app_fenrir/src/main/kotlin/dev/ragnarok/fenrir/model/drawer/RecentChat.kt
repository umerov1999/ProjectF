package dev.ragnarok.fenrir.model.drawer

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class RecentChat : AbsMenuItem {
    @SerialName("aid")
    var aid = 0
        private set

    @SerialName("peerId")
    var peerId = 0

    @SerialName("title")
    var title: String? = null

    @SerialName("iconUrl")
    var iconUrl: String? = null

    constructor() : super(TYPE_RECENT_CHAT)
    constructor(
        aid: Int,
        peerId: Int,
        title: String?,
        iconUrl: String?
    ) : super(TYPE_RECENT_CHAT) {
        this.aid = aid
        this.peerId = peerId
        this.title = title
        this.iconUrl = iconUrl
    }

    internal constructor(`in`: Parcel) : super(`in`) {
        aid = `in`.readInt()
        peerId = `in`.readInt()
        title = `in`.readString()
        iconUrl = `in`.readString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as RecentChat
        return aid == that.aid && peerId == that.peerId
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + aid
        result = 31 * result + peerId
        return result
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(aid)
        dest.writeInt(peerId)
        dest.writeString(title)
        dest.writeString(iconUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecentChat> {
        override fun createFromParcel(parcel: Parcel): RecentChat {
            return RecentChat(parcel)
        }

        override fun newArray(size: Int): Array<RecentChat?> {
            return arrayOfNulls(size)
        }
    }
}