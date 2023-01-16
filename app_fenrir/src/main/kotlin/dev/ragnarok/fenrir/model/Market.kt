package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class Market : AbsModel {
    val id: Int
    val owner_id: Long
    var access_key: String? = null
        private set
    var isIs_favorite = false
        private set
    var weight = 0
        private set
    var availability = 0
        private set
    var date: Long = 0
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var price: String? = null
        private set
    var dimensions: String? = null
        private set
    var thumb_photo: String? = null
        private set
    var sku: String? = null
        private set
    var photos: List<Photo>? = null
        private set

    constructor(id: Int, owner_id: Long) {
        this.id = id
        this.owner_id = owner_id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readLong()
        access_key = parcel.readString()
        isIs_favorite = parcel.getBoolean()
        weight = parcel.readInt()
        availability = parcel.readInt()
        date = parcel.readLong()
        title = parcel.readString()
        description = parcel.readString()
        price = parcel.readString()
        dimensions = parcel.readString()
        thumb_photo = parcel.readString()
        sku = parcel.readString()
        photos = parcel.createTypedArrayList(Photo.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(owner_id)
        parcel.writeString(access_key)
        parcel.putBoolean(isIs_favorite)
        parcel.writeInt(weight)
        parcel.writeInt(availability)
        parcel.writeLong(date)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(price)
        parcel.writeString(dimensions)
        parcel.writeString(thumb_photo)
        parcel.writeString(sku)
        parcel.writeTypedList(photos)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_MARKET
    }

    fun setAccess_key(access_key: String?): Market {
        this.access_key = access_key
        return this
    }

    fun setPhotos(photos: List<Photo>?): Market {
        this.photos = photos
        return this
    }

    fun setIs_favorite(is_favorite: Boolean): Market {
        isIs_favorite = is_favorite
        return this
    }

    fun setWeight(weight: Int): Market {
        this.weight = weight
        return this
    }

    fun setAvailability(availability: Int): Market {
        this.availability = availability
        return this
    }

    fun setDate(date: Long): Market {
        this.date = date
        return this
    }

    fun setTitle(title: String?): Market {
        this.title = title
        return this
    }

    fun setDescription(description: String?): Market {
        this.description = description
        return this
    }

    fun setPrice(price: String?): Market {
        this.price = price
        return this
    }

    fun setDimensions(dimensions: String?): Market {
        this.dimensions = dimensions
        return this
    }

    fun setThumb_photo(thumb_photo: String?): Market {
        this.thumb_photo = thumb_photo
        return this
    }

    fun setSku(sku: String?): Market {
        this.sku = sku
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Market> {
        override fun createFromParcel(parcel: Parcel): Market {
            return Market(parcel)
        }

        override fun newArray(size: Int): Array<Market?> {
            return arrayOfNulls(size)
        }
    }
}