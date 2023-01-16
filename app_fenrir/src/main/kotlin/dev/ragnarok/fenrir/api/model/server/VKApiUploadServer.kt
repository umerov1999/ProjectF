package dev.ragnarok.fenrir.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiUploadServer : Parcelable, UploadServer {
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
    var user_id = 0L

    internal constructor(parcel: Parcel) {
        url = parcel.readString()
        album_id = parcel.readInt()
        user_id = parcel.readLong()
    }

    @Suppress("UNUSED")
    constructor()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
        dest.writeInt(album_id)
        dest.writeLong(user_id)
    }

    override fun toString(): String {
        return "VKApiUploadServer{" +
                "upload_url='" + url + '\'' +
                ", album_id=" + album_id +
                ", user_id=" + user_id +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<VKApiUploadServer> {
        override fun createFromParcel(parcel: Parcel): VKApiUploadServer {
            return VKApiUploadServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiUploadServer?> {
            return arrayOfNulls(size)
        }
    }
}