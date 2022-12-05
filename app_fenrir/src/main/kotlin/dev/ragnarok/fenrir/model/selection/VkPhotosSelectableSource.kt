package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class VkPhotosSelectableSource : AbsSelectableSource {
    val accountId: Int
    val ownerId: Int

    /**
     * @param accountId Кто будет загружать список фото
     * @param ownerId   Чьи фото будут загружатся
     */
    constructor(accountId: Int, ownerId: Int) : super(Types.VK_PHOTOS) {
        this.accountId = accountId
        this.ownerId = ownerId
    }

    internal constructor(`in`: Parcel) : super(`in`) {
        accountId = `in`.readInt()
        ownerId = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(accountId)
        dest.writeInt(ownerId)
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