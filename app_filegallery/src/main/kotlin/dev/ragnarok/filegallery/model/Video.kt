package dev.ragnarok.filegallery.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.api.adapters.VideoDtoAdapter
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.putBoolean
import kotlinx.serialization.Serializable

@Serializable(with = VideoDtoAdapter::class)
class Video : Parcelable, ParcelNative.ParcelableNative {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var link: String? = null
        private set
    var date: Long = 0
        private set
    var image: String? = null
        private set
    var isRepeat = false
        private set
    var duration = 0L
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        title = parcel.readString()
        description = parcel.readString()
        link = parcel.readString()
        date = parcel.readLong()
        image = parcel.readString()
        isRepeat = parcel.getBoolean()
        duration = parcel.readLong()
    }

    internal constructor(parcel: ParcelNative) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        title = parcel.readString()
        description = parcel.readString()
        link = parcel.readString()
        date = parcel.readLong()
        image = parcel.readString()
        isRepeat = parcel.readBoolean()
        duration = parcel.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(ownerId)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(link)
        dest.writeLong(date)
        dest.writeString(image)
        dest.putBoolean(isRepeat)
        dest.writeLong(duration)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeLong(ownerId)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(link)
        dest.writeLong(date)
        dest.writeString(image)
        dest.writeBoolean(isRepeat)
        dest.writeLong(duration)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setId(id: Int): Video {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Long): Video {
        this.ownerId = ownerId
        return this
    }

    fun setTitle(title: String?): Video {
        this.title = title
        return this
    }

    fun setDescription(description: String?): Video {
        this.description = description
        return this
    }

    fun setLink(link: String?): Video {
        this.link = link
        return this
    }

    fun setDate(date: Long): Video {
        this.date = date
        return this
    }

    fun setImage(image: String?): Video {
        this.image = image
        return this
    }

    fun setRepeat(repeat: Boolean): Video {
        isRepeat = repeat
        return this
    }

    fun setDuration(duration: Long): Video {
        this.duration = duration
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video> = object : Parcelable.Creator<Video> {
            override fun createFromParcel(parcel: Parcel): Video {
                return Video(parcel)
            }

            override fun newArray(size: Int): Array<Video?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Video> =
            object : ParcelNative.Creator<Video> {
                override fun readFromParcelNative(dest: ParcelNative): Video {
                    return Video(dest)
                }
            }
    }
}