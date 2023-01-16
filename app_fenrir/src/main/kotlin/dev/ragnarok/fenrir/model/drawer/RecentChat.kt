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
    var aid = 0L
        private set

    @SerialName("peerId")
    var peerId = 0L

    @SerialName("title")
    var title: String? = null

    @SerialName("iconUrl")
    var iconUrl: String? = null

    constructor() : super(TYPE_RECENT_CHAT)
    constructor(
        aid: Long,
        peerId: Long,
        title: String?,
        iconUrl: String?
    ) : super(TYPE_RECENT_CHAT) {
        this.aid = aid
        this.peerId = peerId
        this.title = title
        this.iconUrl = iconUrl
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        aid = parcel.readLong()
        peerId = parcel.readLong()
        title = parcel.readString()
        iconUrl = parcel.readString()
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
        result = 31 * result + aid.hashCode()
        result = 31 * result + peerId.hashCode()
        return result
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(aid)
        dest.writeLong(peerId)
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