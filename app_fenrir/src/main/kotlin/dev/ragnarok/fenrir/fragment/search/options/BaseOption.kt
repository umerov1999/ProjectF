package dev.ragnarok.fenrir.fragment.search.options

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

open class BaseOption : Parcelable, Cloneable {
    val key: Int
    val optionType: Int
    val active: Boolean
    val title: Int
    var parentDependencyKey: Int
    var childDependencies: IntArray? = null

    constructor(optionType: Int, key: Int, title: Int, active: Boolean) {
        this.optionType = optionType
        this.key = key
        this.title = title
        this.active = active
        parentDependencyKey = NO_DEPENDENCY
    }

    protected constructor(`in`: Parcel) {
        optionType = `in`.readInt()
        key = `in`.readInt()
        active = `in`.getBoolean()
        title = `in`.readInt()
        parentDependencyKey = `in`.readInt()
        childDependencies = `in`.createIntArray()
    }

    fun makeChildDependencies(vararg childs: Int) {
        childDependencies = childs
    }

    fun setDependencyOf(key: Int) {
        parentDependencyKey = key
    }

    open fun reset() {
        // must be implemented in child class
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(optionType)
        dest.writeInt(key)
        dest.putBoolean(active)
        dest.writeInt(title)
        dest.writeInt(parentDependencyKey)
        dest.writeIntArray(childDependencies)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val option = other as BaseOption
        if (optionType != option.optionType) return false
        return if (key != option.key) false else active == option.active
    }

    override fun hashCode(): Int {
        var result = optionType
        result = 31 * result + key
        result = 31 * result + if (active) 1 else 0
        return result
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): BaseOption {
        val option = super.clone() as BaseOption
        option.childDependencies = childDependencies?.clone()
        return option
    }

    companion object {
        const val DATABASE = 44
        const val SIMPLE_BOOLEAN = 45
        const val SIMPLE_TEXT = 46
        const val SIMPLE_NUMBER = 47
        const val SPINNER = 48
        const val GPS = 49
        const val DATE_TIME = 50
        const val NO_DEPENDENCY = -1

        @JvmField
        val CREATOR: Parcelable.Creator<BaseOption> = object : Parcelable.Creator<BaseOption> {
            override fun createFromParcel(`in`: Parcel): BaseOption {
                return BaseOption(`in`)
            }

            override fun newArray(size: Int): Array<BaseOption?> {
                return arrayOfNulls(size)
            }
        }
    }
}