package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.Identificable

class IdOption : Parcelable, Identificable {
    private val id: Int
    val title: String?
    val childs: List<IdOption>?

    @JvmOverloads
    constructor(id: Int, title: String?, childs: List<IdOption>? = emptyList()) {
        this.id = id
        this.title = title
        this.childs = childs
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        title = parcel.readString()
        childs = parcel.createTypedArrayList(CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(title)
        dest.writeTypedList(childs)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun getObjectId(): Int {
        return id
    }

    companion object CREATOR : Parcelable.Creator<IdOption> {
        override fun createFromParcel(parcel: Parcel): IdOption {
            return IdOption(parcel)
        }

        override fun newArray(size: Int): Array<IdOption?> {
            return arrayOfNulls(size)
        }
    }
}