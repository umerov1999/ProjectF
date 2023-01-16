package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class VkPhotosSelectableSource : AbsSelectableSource {
    val accountId: Long
    val ownerId: Long

    /**
     * @param accountId Кто будет загружать список фото
     * @param ownerId   Чьи фото будут загружатся
     */
    constructor(accountId: Long, ownerId: Long) : super(Types.VK_PHOTOS) {
        this.accountId = accountId
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        accountId = parcel.readLong()
        ownerId = parcel.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(accountId)
        dest.writeLong(ownerId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VkPhotosSelectableSource> {
        override fun createFromParcel(parcel: Parcel): VkPhotosSelectableSource {
            return VkPhotosSelectableSource(parcel)
        }

        override fun newArray(size: Int): Array<VkPhotosSelectableSource?> {
            return arrayOfNulls(size)
        }
    }
}