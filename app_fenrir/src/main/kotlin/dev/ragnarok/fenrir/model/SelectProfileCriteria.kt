package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef

class SelectProfileCriteria : Parcelable {
    @OwnerType
    private var ownerType: Int

    internal constructor(parcel: Parcel) {
        ownerType = parcel.readInt()
    }

    constructor() {
        ownerType = OwnerType.ALL_PEOPLE
    }

    @OwnerType
    fun getOwnerType(): Int {
        return ownerType
    }

    fun setOwnerType(@OwnerType ownerType: Int): SelectProfileCriteria {
        this.ownerType = ownerType
        return this
    }

    fun getIsPeopleOnly(): Boolean {
        return ownerType == OwnerType.ALL_PEOPLE || ownerType == OwnerType.ONLY_FRIENDS
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ownerType)
    }

    @IntDef(OwnerType.ALL_PEOPLE, OwnerType.ONLY_FRIENDS, OwnerType.OWNERS)
    @Retention(
        AnnotationRetention.SOURCE
    )
    annotation class OwnerType {
        companion object {
            const val ALL_PEOPLE = 1
            const val ONLY_FRIENDS = 2
            const val OWNERS = 3
        }
    }

    companion object CREATOR : Parcelable.Creator<SelectProfileCriteria> {
        override fun createFromParcel(parcel: Parcel): SelectProfileCriteria {
            return SelectProfileCriteria(parcel)
        }

        override fun newArray(size: Int): Array<SelectProfileCriteria?> {
            return arrayOfNulls(size)
        }
    }
}