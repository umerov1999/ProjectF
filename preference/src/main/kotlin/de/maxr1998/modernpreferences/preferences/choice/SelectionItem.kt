package de.maxr1998.modernpreferences.preferences.choice

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID

/**
 * Represents a selectable item in a selection dialog preference,
 * e.g. the [SingleChoiceDialogPreference]
 *
 * @param key The key of this item, will be committed to preferences if selected
 */
data class SelectionItem(
    val key: String,
    @StringRes
    val titleRes: Int,
    val title: CharSequence,
    @StringRes
    val summaryRes: Int,
    val summary: CharSequence?,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()
    )

    /**
     * @see SelectionItem
     */
    constructor(
        key: String,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int = DISABLED_RESOURCE_ID
    ) :
            this(key, titleRes, "", summaryRes, null)

    /**
     * @see SelectionItem
     */
    constructor(key: String, title: CharSequence, summary: CharSequence? = null) :
            this(key, DISABLED_RESOURCE_ID, title, DISABLED_RESOURCE_ID, summary)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(key)
        dest.writeInt(titleRes)
        dest.writeString(title.toString())
        dest.writeInt(summaryRes)
        dest.writeString(summary?.toString())
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