package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.options.SimpleBooleanOption
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption

class AudioSearchCriteria : BaseSearchCriteria {
    constructor(query: String?, by_artist: Boolean, in_main_page: Boolean) : super(query) {
        val sort = SpinnerOption(KEY_SORT, R.string.sorting, true)
        sort.available = ArrayList(3)
        sort.available.add(SpinnerOption.Entry(0, R.string.by_date_added))
        sort.available.add(SpinnerOption.Entry(1, R.string.by_relevance))
        sort.available.add(SpinnerOption.Entry(2, R.string.by_duration))
        appendOption(sort)
        appendOption(SimpleBooleanOption(KEY_SEARCH_ADDED, R.string.my_saved, true, in_main_page))
        appendOption(SimpleBooleanOption(KEY_SEARCH_BY_ARTIST, R.string.by_artist, true, by_artist))
        appendOption(SimpleBooleanOption(KEY_SEARCH_AUTOCOMPLETE, R.string.auto_compete, true))
        appendOption(SimpleBooleanOption(KEY_SEARCH_WITH_LYRICS, R.string.with_lyrics, true))
    }

    internal constructor(`in`: Parcel) : super(`in`)

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): AudioSearchCriteria {
        return super.clone() as AudioSearchCriteria
    }

    companion object {
        const val KEY_SEARCH_ADDED = 1
        const val KEY_SEARCH_BY_ARTIST = 2
        const val KEY_SEARCH_AUTOCOMPLETE = 3
        const val KEY_SEARCH_WITH_LYRICS = 4
        const val KEY_SORT = 5

        @JvmField
        val CREATOR: Parcelable.Creator<AudioSearchCriteria> =
            object : Parcelable.Creator<AudioSearchCriteria> {
                override fun createFromParcel(`in`: Parcel): AudioSearchCriteria {
                    return AudioSearchCriteria(`in`)
                }

                override fun newArray(size: Int): Array<AudioSearchCriteria?> {
                    return arrayOfNulls(size)
                }
            }
    }
}