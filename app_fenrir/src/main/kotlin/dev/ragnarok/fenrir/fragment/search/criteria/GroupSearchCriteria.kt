package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.options.DatabaseOption
import dev.ragnarok.fenrir.fragment.search.options.SimpleBooleanOption
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption

class GroupSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query, 5) {
        val type = SpinnerOption(KEY_TYPE, R.string.type, true)
        type.available = ArrayList(3)
        type.available.add(SpinnerOption.Entry(TYPE_PAGE, R.string.page))
        type.available.add(SpinnerOption.Entry(TYPE_GROUP, R.string.group))
        type.available.add(SpinnerOption.Entry(TYPE_EVENT, R.string.event))
        appendOption(type)
        val country =
            DatabaseOption(KEY_COUNTRY, R.string.country, true, DatabaseOption.TYPE_COUNTRY)
        country.makeChildDependencies(KEY_CITY)
        appendOption(country)
        val city = DatabaseOption(KEY_CITY, R.string.city, true, DatabaseOption.TYPE_CITY)
        city.setDependencyOf(KEY_COUNTRY)
        appendOption(city)
        val sort = SpinnerOption(KEY_SORT, R.string.sorting, true)
        sort.available = ArrayList(6)
        sort.available.add(SpinnerOption.Entry(0, R.string.default_sorting))
        sort.available.add(SpinnerOption.Entry(1, R.string.sorting_by_growth_speed))
        sort.available.add(
            SpinnerOption.Entry(
                2,
                R.string.sorting_by_day_attendance_members_number_ratio
            )
        )
        sort.available.add(
            SpinnerOption.Entry(
                3,
                R.string.sorting_by_likes_number_members_number_ratio
            )
        )
        sort.available.add(
            SpinnerOption.Entry(
                4,
                R.string.sorting_by_comments_number_members_number_ratio
            )
        )
        sort.available.add(
            SpinnerOption.Entry(
                5,
                R.string.sorting_by_boards_entries_number_members_number_ratio
            )
        )
        appendOption(sort)
        val futureOnly = SimpleBooleanOption(KEY_FUTURE_ONLY, R.string.future_events_only, true)
        appendOption(futureOnly)
    }

    internal constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): GroupSearchCriteria {
        return super.clone() as GroupSearchCriteria
    }

    companion object {
        const val KEY_TYPE = 1
        const val KEY_COUNTRY = 2
        const val KEY_CITY = 3
        const val KEY_SORT = 4
        const val KEY_FUTURE_ONLY = 5
        const val TYPE_PAGE = 1
        const val TYPE_GROUP = 2
        const val TYPE_EVENT = 3

        @JvmField
        val CREATOR: Parcelable.Creator<GroupSearchCriteria> =
            object : Parcelable.Creator<GroupSearchCriteria> {
                override fun createFromParcel(`in`: Parcel): GroupSearchCriteria {
                    return GroupSearchCriteria(`in`)
                }

                override fun newArray(size: Int): Array<GroupSearchCriteria?> {
                    return arrayOfNulls(size)
                }
            }
    }
}