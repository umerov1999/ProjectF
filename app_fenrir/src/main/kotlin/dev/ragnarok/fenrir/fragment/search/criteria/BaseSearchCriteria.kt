package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.fragment.search.options.*
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

open class BaseSearchCriteria : Parcelable, Cloneable {
    var query: String?
    var options: ArrayList<BaseOption>
        private set

    @JvmOverloads
    constructor(query: String?, optionsCount: Int = 0) {
        this.query = query
        options = ArrayList(optionsCount)
    }

    protected constructor(`in`: Parcel) {
        query = `in`.readString()
        val optionsSize = `in`.readInt()
        options = ArrayList(optionsSize)
        for (i in 0 until optionsSize) {
            when (`in`.readInt()) {
                BaseOption.DATABASE -> `in`.readTypedObjectCompat(DatabaseOption.CREATOR)
                BaseOption.SIMPLE_BOOLEAN -> `in`.readTypedObjectCompat(SimpleBooleanOption.CREATOR)
                BaseOption.SIMPLE_TEXT -> `in`.readTypedObjectCompat(SimpleTextOption.CREATOR)
                BaseOption.SIMPLE_NUMBER -> `in`.readTypedObjectCompat(SimpleNumberOption.CREATOR)
                BaseOption.SPINNER -> `in`.readTypedObjectCompat(SpinnerOption.CREATOR)
                BaseOption.GPS -> `in`.readTypedObjectCompat(SimpleGPSOption.CREATOR)
                BaseOption.DATE_TIME -> `in`.readTypedObjectCompat(SimpleDateOption.CREATOR)
                else -> throw IllegalArgumentException("Unknown option type !!!")
            }?.let { options.add(it) }
        }
    }

    fun appendOption(option: BaseOption) {
        options.add(option)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BaseSearchCriteria
        return (query == that.query
                && options == that.options)
    }

    override fun hashCode(): Int {
        var result = if (query != null) query.hashCode() else 0
        result = 31 * result + options.hashCode()
        return result
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): BaseSearchCriteria {
        val clone = super.clone() as BaseSearchCriteria
        clone.options = ArrayList(options.size)
        for (option in options) {
            clone.options.add(option.clone())
        }
        return clone
    }

    fun safellyClone(): BaseSearchCriteria {
        return try {
            clone()
        } catch (e: CloneNotSupportedException) {
            throw UnsupportedOperationException("Unable to clone criteria")
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(query)
        dest.writeInt(options.size)
        for (option in options) {
            dest.writeInt(option.optionType)
            dest.writeTypedObjectCompat(option, flags)
        }
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T : BaseOption> findOptionByKey(key: Int): T? {
        for (option in options) {
            if (option.key == key) {
                return option as T
            }
        }
        return null
    }

    fun extractBoleanValueFromOption(key: Int): Boolean {
        val simpleBooleanOption = findOptionByKey<SimpleBooleanOption>(key)
        return simpleBooleanOption != null && simpleBooleanOption.checked
    }

    fun extractNumberValueFromOption(key: Int): Int? {
        val simpleNumberOption = findOptionByKey<SimpleNumberOption>(key)
        return simpleNumberOption?.value
    }

    fun extractTextValueFromOption(key: Int): String? {
        val option = findOptionByKey<SimpleTextOption>(key)
        return option?.value
    }

    fun extractDatabaseEntryValueId(key: Int): Int? {
        val databaseOption = findOptionByKey<DatabaseOption>(key)
        return if (databaseOption?.value == null) {
            null
        } else {
            (databaseOption.value ?: return null).id
        }
    }

    companion object CREATOR : Parcelable.Creator<BaseSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): BaseSearchCriteria {
            return BaseSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<BaseSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}