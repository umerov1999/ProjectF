package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.options.SimpleDateOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleNumberOption
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption

class PhotoSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query) {
        val sort = SpinnerOption(KEY_SORT, R.string.sorting, true)
        sort.available = ArrayList(2)
        sort.available.add(SpinnerOption.Entry(0, R.string.likes))
        sort.available.add(SpinnerOption.Entry(1, R.string.by_date_added))
        appendOption(sort)
        appendOption(SimpleNumberOption(KEY_RADIUS, R.string.radius, true, 5000))
        appendOption(SimpleGPSOption(KEY_GPS, R.string.gps, true))
        appendOption(SimpleDateOption(KEY_START_TIME, R.string.date_start, true))
        appendOption(SimpleDateOption(KEY_END_TIME, R.string.date_to, true))
    }

    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): PhotoSearchCriteria {
        return super.clone() as PhotoSearchCriteria
    }

    companion object {
        const val KEY_SORT = 1
        const val KEY_RADIUS = 2
        const val KEY_GPS = 3
        const val KEY_START_TIME = 4
        const val KEY_END_TIME = 5

        @JvmField
        val CREATOR: Parcelable.Creator<PhotoSearchCriteria> =
            object : Parcelable.Creator<PhotoSearchCriteria> {
                override fun createFromParcel(`in`: Parcel): PhotoSearchCriteria {
                    return PhotoSearchCriteria(`in`)
                }

                override fun newArray(size: Int): Array<PhotoSearchCriteria?> {
                    return arrayOfNulls(size)
                }
            }
    }
}