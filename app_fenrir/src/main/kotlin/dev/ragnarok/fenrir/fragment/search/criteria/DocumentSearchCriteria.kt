package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class DocumentSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): DocumentSearchCriteria {
        return super.clone() as DocumentSearchCriteria
    }

    companion object CREATOR : Parcelable.Creator<DocumentSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): DocumentSearchCriteria {
            return DocumentSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<DocumentSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}