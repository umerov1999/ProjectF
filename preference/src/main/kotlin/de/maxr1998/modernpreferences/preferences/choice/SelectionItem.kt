package de.maxr1998.modernpreferences.preferences.choice

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID
import de.maxr1998.modernpreferences.helpers.readTypedObjectCompat
import de.maxr1998.modernpreferences.helpers.writeTypedObjectCompat
import de.maxr1998.modernpreferences.preferences.Badge

/**
 * Represents a selectable item in a selection dialog preference,
 * e.g. the [SingleChoiceDialogPreference]
 *
 * @param key The key of this item, will be committed to preferences if selected
 */
data class SelectionItem(
    val key: String,
    @param:StringRes
    val titleRes: Int,
    val title: CharSequence,
    @param:StringRes
    val summaryRes: Int,
    val summary: CharSequence?,
    val badgeInfo: Badge?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString(),
        parcel.readTypedObjectCompat(Badge.CREATOR)
    )

    /**
     * @see SelectionItem
     */
    constructor(
        key: String,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int = DISABLED_RESOURCE_ID,
        badgeInfo: Badge? = null
    ) : this(key, titleRes, "", summaryRes, null, badgeInfo)

    /**
     * @see SelectionItem
     */
    constructor(
        key: String,
        title: CharSequence,
        summary: CharSequence? = null,
        badgeInfo: Badge? = null
    ) : this(
        key,
        DISABLED_RESOURCE_ID,
        title,
        DISABLED_RESOURCE_ID,
        summary,
        badgeInfo
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(key)
        dest.writeInt(titleRes)
        dest.writeString(title.toString())
        dest.writeInt(summaryRes)
        dest.writeString(summary?.toString())
        dest.writeTypedObjectCompat(badgeInfo, flags)
    }

    companion object CREATOR : Parcelable.Creator<SelectionItem> {
        override fun createFromParcel(parcel: Parcel): SelectionItem {
            return SelectionItem(parcel)
        }

        override fun newArray(size: Int): Array<SelectionItem?> {
            return arrayOfNulls(size)
        }
    }
}