package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.errorprone.annotations.Keep
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import kotlinx.serialization.Serializable

@Keep
@Serializable
class DrawerCategory : Parcelable {
    @SwitchableCategory
    private val id: String
    private var active = false

    constructor(@SwitchableCategory id: String, active: Boolean = true) {
        this.id = id
        this.active = active
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readString()!!
        active = `in`.getBoolean()
    }

    @StringRes
    fun getTitleResCategory(): Int {
        return when (id) {
            SwitchableCategory.FRIENDS -> R.string.friends
            SwitchableCategory.NEWSFEED_COMMENTS -> R.string.drawer_newsfeed_comments
            SwitchableCategory.GROUPS -> R.string.groups
            SwitchableCategory.PHOTOS -> R.string.photos
            SwitchableCategory.VIDEOS -> R.string.videos
            SwitchableCategory.MUSIC -> R.string.music
            SwitchableCategory.DOCS -> R.string.documents
            SwitchableCategory.FAVES -> R.string.bookmarks
            SwitchableCategory.DIALOGS -> R.string.dialogs
            SwitchableCategory.FEED -> R.string.feed
            SwitchableCategory.FEEDBACK -> R.string.drawer_feedback
            SwitchableCategory.SEARCH -> R.string.search
            else -> {
                throw IllegalArgumentException()
            }
        }
    }

    @SwitchableCategory
    fun getId(): String {
        return id
    }

    fun isActive(): Boolean {
        return active
    }

    fun setActive(checked: Boolean): DrawerCategory {
        this.active = checked
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.putBoolean(active)
    }

    companion object CREATOR : Parcelable.Creator<DrawerCategory> {
        override fun createFromParcel(parcel: Parcel): DrawerCategory {
            return DrawerCategory(parcel)
        }

        override fun newArray(size: Int): Array<DrawerCategory?> {
            return arrayOfNulls(size)
        }
    }
}