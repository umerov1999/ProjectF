package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.CallSuper

abstract class AbsSelectableSource : Parcelable {
    @Types
    val type: Int

    internal constructor(@Types type: Int) {
        this.type = type
    }

    internal constructor(parcel: Parcel) {
        type = parcel.readInt()
    }

    @CallSuper
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
    }
}