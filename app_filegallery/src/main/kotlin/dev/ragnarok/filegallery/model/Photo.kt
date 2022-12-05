package dev.ragnarok.filegallery.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.api.adapters.PhotoDtoAdapter
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.putBoolean
import kotlinx.serialization.Serializable

@Keep
@Serializable(with = PhotoDtoAdapter::class)
class Photo : Parcelable, ParcelNative.ParcelableNative {
    var id = 0
        private set
    var ownerId = 0
        private set
    var photo_url: String? = null
        private set
    var preview_url: String? = null
        private set
    var text: String? = null
        private set
    var date: Long = 0
        private set
    var isGif = false
        private set
    private var isLocal = false

    constructor()
    internal constructor(`in`: ParcelNative) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        photo_url = `in`.readString()
        preview_url = `in`.readString()
        text = `in`.readString()
        date = `in`.readLong()
        isLocal = `in`.readBoolean()
        isGif = `in`.readBoolean()
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        photo_url = `in`.readString()
        preview_url = `in`.readString()
        text = `in`.readString()
        date = `in`.readLong()
        isLocal = `in`.getBoolean()
        isGif = `in`.getBoolean()
    }

    fun setGif(gif: Boolean): Photo {
        isGif = gif
        return this
    }

    fun inLocal(): Boolean {
        return isLocal
    }

    fun setLocal(local: Boolean): Photo {
        isLocal = local
        return this
    }

    fun setId(id: Int): Photo {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): Photo {
        this.ownerId = ownerId
        return this
    }

    fun setText(text: String?): Photo {
        this.text = text
        return this
    }

    fun setDate(date: Long): Photo {
        this.date = date
        return this
    }

    fun setPhoto_url(photo_url: String?): Photo {
        this.photo_url = photo_url
        return this
    }

    fun setPreview_url(preview_url: String?): Photo {
        this.preview_url = preview_url
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(photo_url)
        parcel.writeString(preview_url)
        parcel.writeString(text)
        parcel.writeLong(date)
        parcel.putBoolean(isLocal)
        parcel.putBoolean(isGif)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val photo = other as Photo
        return id == photo.id && ownerId == photo.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        return result
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeString(photo_url)
        dest.writeString(preview_url)
        dest.writeString(text)
        dest.writeLong(date)
        dest.writeBoolean(isLocal)
        dest.writeBoolean(isGif)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Photo> = object : Parcelable.Creator<Photo> {
            override fun createFromParcel(`in`: Parcel): Photo {
                return Photo(`in`)
            }

            override fun newArray(size: Int): Array<Photo?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Photo> =
            object : ParcelNative.Creator<Photo> {
                override fun readFromParcelNative(dest: ParcelNative): Photo {
                    return Photo(dest)
                }
            }
    }
}