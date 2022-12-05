package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class LocalVideosSelectableSource : AbsSelectableSource {
    constructor() : super(Types.VIDEOS)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocalVideosSelectableSource> {
        override fun createFromParcel(parcel: Parcel): LocalVideosSelectableSource {
            return LocalVideosSelectableSource(parcel)
        }

        override fun newArray(size: Int): Array<LocalVideosSelectableSource?> {
            return arrayOfNulls(size)
        }
    }
}