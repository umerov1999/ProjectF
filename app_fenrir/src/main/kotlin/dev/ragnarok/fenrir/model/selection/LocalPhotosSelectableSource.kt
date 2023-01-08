package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class LocalPhotosSelectableSource : AbsSelectableSource {
    constructor() : super(Types.LOCAL_PHOTOS)
    internal constructor(parcel: Parcel) : super(parcel)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocalPhotosSelectableSource> {
        override fun createFromParcel(parcel: Parcel): LocalPhotosSelectableSource {
            return LocalPhotosSelectableSource(parcel)
        }

        override fun newArray(size: Int): Array<LocalPhotosSelectableSource?> {
            return arrayOfNulls(size)
        }
    }
}