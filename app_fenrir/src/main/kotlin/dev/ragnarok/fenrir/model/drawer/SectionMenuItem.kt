package dev.ragnarok.fenrir.model.drawer

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes

open class SectionMenuItem : AbsMenuItem {
    var section: Int

    @StringRes
    var title: Int
    var count = 0

    constructor(type: Int, section: Int, @StringRes title: Int) : super(type) {
        this.section = section
        this.title = title
    }

    protected constructor(parcel: Parcel) : super(parcel) {
        section = parcel.readInt()
        title = parcel.readInt()
        count = parcel.readInt()
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + section
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(section)
        dest.writeInt(title)
        dest.writeInt(count)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as SectionMenuItem
        return section == that.section
    }

    companion object CREATOR : Parcelable.Creator<SectionMenuItem> {
        override fun createFromParcel(parcel: Parcel): SectionMenuItem {
            return SectionMenuItem(parcel)
        }

        override fun newArray(size: Int): Array<SectionMenuItem?> {
            return arrayOfNulls(size)
        }
    }
}