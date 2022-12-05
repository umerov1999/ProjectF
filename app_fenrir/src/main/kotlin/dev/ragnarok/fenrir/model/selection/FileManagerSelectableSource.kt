package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class FileManagerSelectableSource : AbsSelectableSource {
    constructor() : super(Types.FILES)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileManagerSelectableSource> {
        override fun createFromParcel(parcel: Parcel): FileManagerSelectableSource {
            return FileManagerSelectableSource(parcel)
        }

        override fun newArray(size: Int): Array<FileManagerSelectableSource?> {
            return arrayOfNulls(size)
        }
    }
}