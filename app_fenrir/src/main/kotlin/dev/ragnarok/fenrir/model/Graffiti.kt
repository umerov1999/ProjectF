package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Graffiti : AbsModel {
    var id = 0
        private set
    var owner_id = 0
        private set
    var url: String? = null
        private set
    var width = 0
        private set
    var height = 0
        private set
    var access_key: String? = null
        private set

    constructor()
    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        owner_id = `in`.readInt()
        url = `in`.readString()
        width = `in`.readInt()
        height = `in`.readInt()
        access_key = `in`.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(owner_id)
        parcel.writeString(url)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeString(access_key)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_GRAFFITI
    }

    fun setId(id: Int): Graffiti {
        this.id = id
        return this
    }

    fun setOwner_id(owner_id: Int): Graffiti {
        this.owner_id = owner_id
        return this
    }

    fun setUrl(url: String?): Graffiti {
        this.url = url
        return this
    }

    fun setWidth(width: Int): Graffiti {
        this.width = width
        return this
    }

    fun setHeight(height: Int): Graffiti {
        this.height = height
        return this
    }

    fun setAccess_key(access_key: String?): Graffiti {
        this.access_key = access_key
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Graffiti) return false
        return id == other.id && owner_id == other.owner_id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id
        return result
    }

    companion object CREATOR : Parcelable.Creator<Graffiti> {
        override fun createFromParcel(parcel: Parcel): Graffiti {
            return Graffiti(parcel)
        }

        override fun newArray(size: Int): Array<Graffiti?> {
            return arrayOfNulls(size)
        }
    }
}