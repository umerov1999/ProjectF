package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class AudioPlaylistSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query)
    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AudioPlaylistSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): AudioPlaylistSearchCriteria {
            return AudioPlaylistSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<AudioPlaylistSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}