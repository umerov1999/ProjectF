package dev.ragnarok.fenrir.model.drawer

import android.os.Parcel
import android.os.Parcelable

class DividerMenuItem : AbsMenuItem {
    constructor() : super(TYPE_DIVIDER)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DividerMenuItem> {
        override fun createFromParcel(parcel: Parcel): DividerMenuItem {
            return DividerMenuItem(parcel)
        }

        override fun newArray(size: Int): Array<DividerMenuItem?> {
            return arrayOfNulls(size)
        }
    }
}