package de.maxr1998.modernpreferences.preferences

import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID
import de.maxr1998.modernpreferences.helpers.readTypedObjectCompat
import de.maxr1998.modernpreferences.helpers.writeTypedObjectCompat

class Badge : Parcelable {
    internal constructor() {
        textRes = DISABLED_RESOURCE_ID
        text = null
        badgeColor = null
    }

    constructor(@StringRes textRes: Int, text: CharSequence?, badgeColor: ColorStateList?) {
        this.textRes = textRes
        this.text = text
        this.badgeColor = badgeColor
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readTypedObjectCompat(ColorStateList.CREATOR)
    )

    constructor(text: CharSequence?, badgeColor: ColorStateList? = null) {
        this.textRes = DISABLED_RESOURCE_ID
        this.text = text
        this.badgeColor = badgeColor
    }

    constructor(@StringRes textRes: Int, badgeColor: ColorStateList? = null) {
        this.textRes = textRes
        this.text = null
        this.badgeColor = badgeColor
    }

    fun copy() = Badge(textRes, text, badgeColor)

    val isVisible: Boolean
        get() = textRes != DISABLED_RESOURCE_ID || text != null

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(textRes)
        parcel.writeString(text?.toString())
        parcel.writeTypedObjectCompat(badgeColor, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Badge> {
        override fun createFromParcel(parcel: Parcel): Badge {
            return Badge(parcel)
        }

        override fun newArray(size: Int): Array<Badge?> {
            return arrayOfNulls(size)
        }
    }

    @StringRes
    val textRes: Int
    val text: CharSequence?
    val badgeColor: ColorStateList?
}