package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.horizontal.Entry
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class FeedSource : Entry, Parcelable {
    val value: String?
    private val title: Text?
    private var active = false
    private var custom: Boolean = false
    var customId: Long? = null
        private set

    constructor(value: String?, title: String?) {
        this.value = value
        this.title = Text(title)
    }

    constructor(value: String?, @StringRes title: Int) {
        this.value = value
        this.title = Text(title)
    }

    internal constructor(parcel: Parcel) {
        value = parcel.readString()
        title = parcel.readTypedObjectCompat(Text.CREATOR)
        active = parcel.getBoolean()
        custom = parcel.getBoolean()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(value)
        dest.writeTypedObjectCompat(title, flags)
        dest.putBoolean(active)
        dest.putBoolean(custom)
    }

    override fun getTitle(context: Context): String? {
        return title?.getText(context)
    }

    override val isActive: Boolean
        get() = active

    fun setActive(active: Boolean): FeedSource {
        this.active = active
        return this
    }

    override val isCustom: Boolean
        get() = custom

    fun setCustom(custom: Boolean): FeedSource {
        this.custom = custom
        return this
    }

    fun setCustomId(customId: Long): FeedSource {
        this.customId = customId
        return this
    }

    companion object CREATOR : Parcelable.Creator<FeedSource> {
        override fun createFromParcel(parcel: Parcel): FeedSource {
            return FeedSource(parcel)
        }

        override fun newArray(size: Int): Array<FeedSource?> {
            return arrayOfNulls(size)
        }
    }
}