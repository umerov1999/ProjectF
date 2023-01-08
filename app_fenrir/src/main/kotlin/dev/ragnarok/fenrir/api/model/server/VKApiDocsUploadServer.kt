package dev.ragnarok.fenrir.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiDocsUploadServer : UploadServer, Parcelable {
    @SerialName("upload_url")
    override var url: String? = null

    @Suppress("UNUSED")
    constructor()
    internal constructor(parcel: Parcel) {
        url = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
    }

    companion object CREATOR : Parcelable.Creator<VKApiDocsUploadServer> {
        override fun createFromParcel(parcel: Parcel): VKApiDocsUploadServer {
            return VKApiDocsUploadServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiDocsUploadServer?> {
            return arrayOfNulls(size)
        }
    }
}