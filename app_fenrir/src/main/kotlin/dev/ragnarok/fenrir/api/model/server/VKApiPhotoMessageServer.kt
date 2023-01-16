package dev.ragnarok.fenrir.api.model.server

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Атрибуты сервера для аплоада фотографий в личное сообщение
 */
@Serializable
class VKApiPhotoMessageServer : Parcelable, UploadServer {
    /**
     * Адрес сервера
     */
    @SerialName("upload_url")
    override var url: String? = null

    /**
     * id альбома
     */
    @SerialName("album_id")
    var album_id = 0

    /**
     * id текущего пользователя
     */
    @SerialName("user_id")
    var user_id = 0L

    @Suppress("UNUSED")
    constructor()
    internal constructor(parcel: Parcel) {
        url = parcel.readString()
        album_id = parcel.readInt()
        user_id = parcel.readLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
        dest.writeInt(album_id)
        dest.writeLong(user_id)
    }

    override fun toString(): String {
        return "VKApiPhotoMessageServer{" +
                "upload_url='" + url + '\'' +
                ", album_id=" + album_id +
                ", user_id=" + user_id +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<VKApiPhotoMessageServer> {
        override fun createFromParcel(parcel: Parcel): VKApiPhotoMessageServer {
            return VKApiPhotoMessageServer(parcel)
        }

        override fun newArray(size: Int): Array<VKApiPhotoMessageServer?> {
            return arrayOfNulls(size)
        }
    }
}