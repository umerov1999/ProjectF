package dev.ragnarok.filegallery.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiAudioUploadServer : Parcelable, UploadServer {
    @SerialName("upload_url")
    override var url: String? = null

    @Suppress("UNUSED")
    constructor()
    constructor(url: String?) {
        this.url = url
    }

    internal constructor(`in`: Parcel) {
        url = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
    }

    companion object CREATOR : Parcelable.Creator<VKApiAudioUploadServer> {
        override fun createFromParcel(parcel: Parcel): VKApiAudioUploadServer {
            return VKApiAudioUploadServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiAudioUploadServer?> {
            return arrayOfNulls(size)
        }
    }
}