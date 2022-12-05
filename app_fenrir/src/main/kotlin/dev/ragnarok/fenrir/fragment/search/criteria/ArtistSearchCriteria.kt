package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class ArtistSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ArtistSearchCriteria> {
        override fun createFromParcel(`in`: Parcel): ArtistSearchCriteria {
            return ArtistSearchCriteria(`in`)
        }

        override fun newArray(size: Int): Array<ArtistSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}