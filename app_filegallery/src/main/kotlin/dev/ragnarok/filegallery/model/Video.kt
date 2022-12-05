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
    var ownerId = 0
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
    var duration = 0
        private set

    constructor()
    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        title = `in`.readString()
        description = `in`.readString()
        link = `in`.readString()
        date = `in`.readLong()
        image = `in`.readString()
        isRepeat = `in`.getBoolean()
        duration = `in`.readInt()
    }

    internal constructor(`in`: ParcelNative) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        title = `in`.readString()
        description = `in`.readString()
        link = `in`.readString()
        date = `in`.readLong()
        image = `in`.readString()
        isRepeat = `in`.readBoolean()
        duration = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(link)
        dest.writeLong(date)
        dest.writeString(image)
        dest.putBoolean(isRepeat)
        dest.writeInt(duration)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(link)
        dest.writeLong(date)
        dest.writeString(image)
        dest.writeBoolean(isRepeat)
        dest.writeInt(duration)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setId(id: Int): Video {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): Video {
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

    fun setDuration(duration: Int): Video {
        this.duration = duration
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video> = object : Parcelable.Creator<Video> {
            override fun createFromParcel(`in`: Parcel): Video {
                return Video(`in`)
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