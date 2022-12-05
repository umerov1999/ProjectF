package dev.ragnarok.fenrir.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiOwnerPhotoUploadServer : Parcelable, UploadServer {
    @SerialName("upload_url")
    override var url: String? = null

    @Suppress("UNUSED")
    constructor()
    internal constructor(`in`: Parcel) {
        url = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
    }

    companion object CREATOR : Parcelable.Creator<VKApiOwnerPhotoUploadServer> {
        override fun createFromParcel(parcel: Parcel): VKApiOwnerPhotoUploadServer {
            return VKApiOwnerPhotoUploadServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiOwnerPhotoUploadServer?> {
            return arrayOfNulls(size)
        }
    }
}