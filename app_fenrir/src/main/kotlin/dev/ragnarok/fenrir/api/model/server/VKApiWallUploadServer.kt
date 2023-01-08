package dev.ragnarok.fenrir.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiWallUploadServer : Parcelable, UploadServer {
    /**
     * адрес для загрузки фотографий
     */
    @SerialName("upload_url")
    override var url: String? = null

    /**
     * идентификатор альбома, в который будет загружена фотография
     */
    var album_id = 0

    /**
     * идентификатор пользователя, от чьего имени будет загружено фото
     */
    var user_id = 0

    @Suppress("UNUSED")
    constructor()
    internal constructor(parcel: Parcel) {
        url = parcel.readString()
        album_id = parcel.readInt()
        user_id = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
        dest.writeInt(album_id)
        dest.writeInt(user_id)
    }

    companion object CREATOR : Parcelable.Creator<VKApiWallUploadServer> {
        override fun createFromParcel(parcel: Parcel): VKApiWallUploadServer {
            return VKApiWallUploadServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiWallUploadServer?> {
            return arrayOfNulls(size)
        }
    }
}