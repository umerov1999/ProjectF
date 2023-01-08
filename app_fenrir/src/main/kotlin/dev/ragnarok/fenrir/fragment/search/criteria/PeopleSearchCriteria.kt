package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.options.*
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger

class PeopleSearchCriteria : BaseSearchCriteria {
    var groupId: Int? = null
        private set

    constructor(query: String?) : super(query) {
        val sort = SpinnerOption(KEY_SORT, R.string.sorting, true)
        sort.available = ArrayList(2)
        sort.available.add(SpinnerOption.Entry(1, R.string.search_option_by_date_registered))
        sort.available.add(SpinnerOption.Entry(0, R.string.search_option_by_rating))
        appendOption(sort)
        val ageFrom = SimpleNumberOption(KEY_AGE_FROM, R.string.age_from, true)
        appendOption(ageFrom)
        val ageTo = SimpleNumberOption(KEY_AGE_TO, R.string.age_to, true)
        appendOption(ageTo)
        val status = SpinnerOption(KEY_RELATIONSHIP, R.string.relationship, true)
        status.available = ArrayList(7)
        status.available.add(SpinnerOption.Entry(1, R.string.search_option_not_married))
        status.available.add(SpinnerOption.Entry(2, R.string.search_option_in_relationship))
        status.available.add(SpinnerOption.Entry(3, R.string.search_option_engaged))
        status.available.add(SpinnerOption.Entry(4, R.string.search_option_married))
        status.available.add(SpinnerOption.Entry(5, R.string.search_option_its_complicated))
        status.available.add(SpinnerOption.Entry(6, R.string.search_option_actively_searching))
        status.available.add(SpinnerOption.Entry(7, R.string.search_option_in_love))
        appendOption(status)
        val sex = SpinnerOption(KEY_SEX, R.string.sex, true)
        sex.available = ArrayList(2)
        sex.available.add(SpinnerOption.Entry(1, R.string.female))
        sex.available.add(SpinnerOption.Entry(2, R.string.male))
        appendOption(sex)
        val onlyOnline = SimpleBooleanOption(KEY_ONLINE_ONLY, R.string.online_only, true)
        appendOption(onlyOnline)
        val withPhoto = SimpleBooleanOption(KEY_WITH_PHOTO_ONLY, R.string.with_photo_only, true)
        appendOption(withPhoto)
        val country =
            DatabaseOption(KEY_COUNTRY, R.string.country, true, DatabaseOption.TYPE_COUNTRY)
        country.makeChildDependencies(KEY_CITY)
        appendOption(country)
        val city = DatabaseOption(KEY_CITY, R.string.city, true, DatabaseOption.TYPE_CITY)
        city.setDependencyOf(KEY_COUNTRY)
        appendOption(city)
        appendOption(SimpleTextOption(KEY_HOMETOWN, R.string.hometown, true))
        appendOption(SimpleNumberOption(KEY_BIRTHDAY_DAY, R.string.birthday_day, true))
        val birthdayMonth = SpinnerOption(KEY_BIRTHDAY_MONTH, R.string.birthday_month, true)
        birthdayMonth.available = ArrayList(12)
        birthdayMonth.available.add(SpinnerOption.Entry(1, R.string.january))
        birthdayMonth.available.add(SpinnerOption.Entry(2, R.string.february))
        birthdayMonth.available.add(SpinnerOption.Entry(3, R.string.march))
        birthdayMonth.available.add(SpinnerOption.Entry(4, R.string.april))
        birthdayMonth.available.add(SpinnerOption.Entry(5, R.string.may))
        birthdayMonth.available.add(SpinnerOption.Entry(6, R.string.june))
        birthdayMonth.available.add(SpinnerOption.Entry(7, R.string.july))
        birthdayMonth.available.add(SpinnerOption.Entry(8, R.string.august))
        birthdayMonth.available.add(SpinnerOption.Entry(9, R.string.september))
        birthdayMonth.available.add(SpinnerOption.Entry(10, R.string.october))
        birthdayMonth.available.add(SpinnerOption.Entry(11, R.string.november))
        birthdayMonth.available.add(SpinnerOption.Entry(12, R.string.december))
        appendOption(birthdayMonth)
        appendOption(SimpleNumberOption(KEY_BIRTHDAY_YEAR, R.string.birthday_year, true))
        val universityCountry = DatabaseOption(
            KEY_UNIVERSITY_COUNTRY,
            R.string.university_country,
            true,
            DatabaseOption.TYPE_COUNTRY
        )
        universityCountry.makeChildDependencies(
            KEY_UNIVERSITY,
            KEY_UNIVERSITY_FACULTY,
            KEY_UNIVERSITY_CHAIR
        )
        appendOption(universityCountry)
        val university = DatabaseOption(
            KEY_UNIVERSITY,
            R.string.college_or_university,
            true,
            DatabaseOption.TYPE_UNIVERSITY
        )
        university.setDependencyOf(KEY_UNIVERSITY_COUNTRY)
        university.makeChildDependencies(KEY_UNIVERSITY_FACULTY, KEY_UNIVERSITY_CHAIR)
        appendOption(university)
        appendOption(SimpleNumberOption(KEY_UNIVERSITY_YEAR, R.string.year_of_graduation, true))
        val faculty = DatabaseOption(
            KEY_UNIVERSITY_FACULTY,
            R.string.faculty,
            true,
            DatabaseOption.TYPE_FACULTY
        )
        faculty.setDependencyOf(KEY_UNIVERSITY)
        faculty.makeChildDependencies(KEY_UNIVERSITY_CHAIR)
        appendOption(faculty)
        val chair =
            DatabaseOption(KEY_UNIVERSITY_CHAIR, R.string.chair, true, DatabaseOption.TYPE_CHAIR)
        chair.setDependencyOf(KEY_UNIVERSITY_FACULTY)
        appendOption(chair)
        val schoolCountry = DatabaseOption(
            KEY_SCHOOL_COUNTRY,
            R.string.school_country,
            true,
            DatabaseOption.TYPE_COUNTRY
        )
        schoolCountry.makeChildDependencies(KEY_SCHOOL_CITY, KEY_SCHOOL, KEY_SCHOOL_CLASS)
        appendOption(schoolCountry)
        val shoolCity =
            DatabaseOption(KEY_SCHOOL_CITY, R.string.school_city, true, DatabaseOption.TYPE_CITY)
        shoolCity.makeChildDependencies(KEY_SCHOOL)
        shoolCity.setDependencyOf(KEY_SCHOOL_COUNTRY)
        appendOption(shoolCity)
        val school = DatabaseOption(KEY_SCHOOL, R.string.school, true, DatabaseOption.TYPE_SCHOOL)
        school.setDependencyOf(KEY_SCHOOL_CITY)
        appendOption(school)
        appendOption(SimpleNumberOption(KEY_SCHOOL_YEAR, R.string.year_of_graduation, true))
        val schoolClass = DatabaseOption(
            KEY_SCHOOL_CLASS,
            R.string.school_class,
            true,
            DatabaseOption.TYPE_SCHOOL_CLASS
        )
        schoolClass.setDependencyOf(KEY_SCHOOL_COUNTRY)
        appendOption(schoolClass)
        appendOption(SimpleTextOption(KEY_RELIGION, R.string.religious_affiliation, true))
        appendOption(SimpleTextOption(KEY_INTERESTS, R.string.interests, true))
        appendOption(SimpleTextOption(KEY_COMPANY, R.string.company, true))
        appendOption(SimpleTextOption(KEY_POSITION, R.string.position, true))
        val fromListOption = SpinnerOption(KEY_FROM_LIST, R.string.from_list, true)
        fromListOption.available = ArrayList(2)
        fromListOption.available.add(SpinnerOption.Entry(FromList.FRIENDS, R.string.friends))
        fromListOption.available.add(
            SpinnerOption.Entry(
                FromList.SUBSCRIPTIONS,
                R.string.subscriptions
            )
        )
        appendOption(fromListOption)
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        groupId = readObjectInteger(parcel)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeObjectInteger(dest, groupId)
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): PeopleSearchCriteria {
        return super.clone() as PeopleSearchCriteria
    }

    fun setGroupId(groupId: Int?): PeopleSearchCriteria {
        this.groupId = groupId
        return this
    }

    object FromList {
        const val FRIENDS = 1
        const val SUBSCRIPTIONS = 2
    }

    companion object {
        const val KEY_SORT = 1
        const val KEY_AGE_FROM = 2
        const val KEY_AGE_TO = 3
        const val KEY_RELATIONSHIP = 4
        const val KEY_SEX = 5
        const val KEY_ONLINE_ONLY = 6
        const val KEY_WITH_PHOTO_ONLY = 7
        const val KEY_COUNTRY = 8
        const val KEY_CITY = 9
        const val KEY_HOMETOWN = 10
        const val KEY_BIRTHDAY_DAY = 13
        const val KEY_BIRTHDAY_MONTH = 15
        const val KEY_BIRTHDAY_YEAR = 16
        const val KEY_UNIVERSITY_COUNTRY = 17
        const val KEY_UNIVERSITY = 18
        const val KEY_UNIVERSITY_YEAR = 19
        const val KEY_UNIVERSITY_FACULTY = 20
        const val KEY_UNIVERSITY_CHAIR = 21
        const val KEY_SCHOOL_COUNTRY = 22
        const val KEY_SCHOOL_CITY = 23
        const val KEY_SCHOOL = 24
        const val KEY_SCHOOL_CLASS = 25
        const val KEY_SCHOOL_YEAR = 26
        const val KEY_RELIGION = 27
        const val KEY_INTERESTS = 28
        const val KEY_COMPANY = 11
        const val KEY_POSITION = 12
        const val KEY_FROM_LIST = 29

        @JvmField
        val CREATOR: Parcelable.Creator<PeopleSearchCriteria> =
            object : Parcelable.Creator<PeopleSearchCriteria> {
                override fun createFromParcel(parcel: Parcel): PeopleSearchCriteria {
                    return PeopleSearchCriteria(parcel)
                }

                override fun newArray(size: Int): Array<PeopleSearchCriteria?> {
                    return arrayOfNulls(size)
                }
            }
    }
}