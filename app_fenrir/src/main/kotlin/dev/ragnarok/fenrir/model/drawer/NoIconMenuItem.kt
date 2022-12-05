package dev.ragnarok.fenrir.model.drawer

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes

class NoIconMenuItem : SectionMenuItem {
    constructor(
        section: Int,
        @StringRes title: Int
    ) : super(TYPE_WITHOUT_ICON, section, title)

    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NoIconMenuItem> {
        override fun createFromParcel(parcel: Parcel): NoIconMenuItem {
            return NoIconMenuItem(parcel)
        }

        override fun newArray(size: Int): Array<NoIconMenuItem?> {
            return arrayOfNulls(size)
        }
    }
}