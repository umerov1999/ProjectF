package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class GiftItem : AbsModel {
    val id: Int
    var thumb256: String? = null
        private set
    var thumb96: String? = null
        private set
    var thumb48: String? = null
        private set

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        thumb256 = `in`.readString()
        thumb96 = `in`.readString()
        thumb48 = `in`.readString()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_GIFT_ITEM
    }

    constructor(id: Int) {
        this.id = id
    }

    val thumb: String?
        get() = firstNonEmptyString(thumb256, thumb96, thumb48)

    fun setThumb256(thumb256: String?): GiftItem {
        this.thumb256 = thumb256
        return this
    }

    fun setThumb96(thumb96: String?): GiftItem {
        this.thumb96 = thumb96
        return this
    }

    fun setThumb48(thumb48: String?): GiftItem {
        this.thumb48 = thumb48
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(thumb256)
        parcel.writeString(thumb96)
        parcel.writeString(thumb48)
    }

    companion object CREATOR : Parcelable.Creator<GiftItem> {
        override fun createFromParcel(parcel: Parcel): GiftItem {
            return GiftItem(parcel)
        }

        override fun newArray(size: Int): Array<GiftItem?> {
            return arrayOfNulls(size)
        }
    }
}