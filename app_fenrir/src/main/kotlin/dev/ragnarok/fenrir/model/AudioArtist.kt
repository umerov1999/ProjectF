package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class AudioArtist : AbsModel {
    private val id: String?
    private var name: String? = null
    private var photo: List<AudioArtistImage>? = null

    constructor(id: String?) {
        this.id = id
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readString()
        name = `in`.readString()
        photo = `in`.createTypedArrayList(AudioArtistImage.CREATOR)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_AUDIO_ARTIST
    }

    fun getId(): String? {
        return id
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?): AudioArtist {
        this.name = name
        return this
    }

    fun getPhoto(): List<AudioArtistImage>? {
        return photo
    }

    fun setPhoto(photo: List<AudioArtistImage>?): AudioArtist {
        this.photo = photo
        return this
    }

    fun getMaxPhoto(): String? {
        if (photo.isNullOrEmpty()) {
            return null
        }
        var size = 0
        var url = photo?.get(0)?.url
        for (i in photo.orEmpty()) {
            if (i.width * i.height > size) {
                size = i.width * i.height
                url = i.url
            }
        }
        return url
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeTypedList(photo)
    }

    class AudioArtistImage : Parcelable {
        val url: String?
        val width: Int
        val height: Int

        constructor(url: String?, width: Int, height: Int) {
            this.url = url
            this.width = width
            this.height = height
        }

        internal constructor(`in`: Parcel) {
            url = `in`.readString()
            width = `in`.readInt()
            height = `in`.readInt()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(url)
            dest.writeInt(width)
            dest.writeInt(height)
        }

        companion object CREATOR : Parcelable.Creator<AudioArtistImage> {
            override fun createFromParcel(parcel: Parcel): AudioArtistImage {
                return AudioArtistImage(parcel)
            }

            override fun newArray(size: Int): Array<AudioArtistImage?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<AudioArtist> {
        override fun createFromParcel(parcel: Parcel): AudioArtist {
            return AudioArtist(parcel)
        }

        override fun newArray(size: Int): Array<AudioArtist?> {
            return arrayOfNulls(size)
        }
    }
}