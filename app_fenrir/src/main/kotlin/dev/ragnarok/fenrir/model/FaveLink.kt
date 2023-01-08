package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class FaveLink : AbsModel {
    val id: String?
    var url: String? = null
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var photo: Photo? = null
        private set

    constructor(id: String?) {
        this.id = id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readString()
        url = parcel.readString()
        title = parcel.readString()
        description = parcel.readString()
        photo = parcel.readTypedObjectCompat(Photo.CREATOR)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_FAVE_LINK
    }

    fun setUrl(url: String?): FaveLink {
        this.url = url
        return this
    }

    fun setTitle(title: String?): FaveLink {
        this.title = title
        return this
    }

    fun setDescription(description: String?): FaveLink {
        this.description = description
        return this
    }

    fun setPhoto(photo: Photo?): FaveLink {
        this.photo = photo
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeTypedObjectCompat(photo, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FaveLink> {
        override fun createFromParcel(parcel: Parcel): FaveLink {
            return FaveLink(parcel)
        }

        override fun newArray(size: Int): Array<FaveLink?> {
            return arrayOfNulls(size)
        }
    }
}