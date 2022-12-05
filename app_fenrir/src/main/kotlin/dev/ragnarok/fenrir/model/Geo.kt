package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Geo : AbsModel {
    var latitude: String? = null
        private set
    var longitude: String? = null
        private set
    var title: String? = null
        private set
    var address: String? = null
        private set
    var country: Int = 0
        private set
    var id: Int = 0
        private set

    constructor()
    internal constructor(`in`: Parcel) {
        latitude = `in`.readString()
        longitude = `in`.readString()
        title = `in`.readString()
        address = `in`.readString()
        country = `in`.readInt()
        id = `in`.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeString(title)
        parcel.writeString(address)
        parcel.writeInt(country)
        parcel.writeInt(id)
    }

    fun setLatitude(latitude: String?): Geo {
        this.latitude = latitude
        return this
    }

    fun setLongitude(longitude: String?): Geo {
        this.longitude = longitude
        return this
    }

    fun setTitle(title: String?): Geo {
        this.title = title
        return this
    }

    fun setAddress(address: String?): Geo {
        this.address = address
        return this
    }

    fun setCountry(country: Int): Geo {
        this.country = country
        return this
    }

    fun setId(id: Int): Geo {
        this.id = id
        return this
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_GEO
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Geo> {
        override fun createFromParcel(parcel: Parcel): Geo {
            return Geo(parcel)
        }

        override fun newArray(size: Int): Array<Geo?> {
            return arrayOfNulls(size)
        }
    }
}
