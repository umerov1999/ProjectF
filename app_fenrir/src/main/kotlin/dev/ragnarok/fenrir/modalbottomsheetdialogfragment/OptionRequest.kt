package dev.ragnarok.fenrir.modalbottomsheetdialogfragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

/**
 * Request for an option you can select within the modal
 */
class OptionRequest(
    val id: Int,
    val title: String?,
    @DrawableRes val icon: Int?,
    val singleLine: Boolean
) : Parcelable {

    internal fun toOption(context: Context): Option {
        var drawable: Drawable? = null
        icon?.let {
            drawable = ResourcesCompat.getDrawable(context.resources, icon, context.theme)
        }

        return Option(id, title, drawable, singleLine)
    }

    constructor(source: Parcel) : this(
        source.readInt(),
        source.readString(),
        if (!source.getBoolean()) source.readInt() else null,
        source.getBoolean()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(id)
        writeString(title)
        putBoolean(icon == null)
        icon?.let {
            writeInt(it)
        }
        putBoolean(singleLine)
    }

    companion object CREATOR : Parcelable.Creator<OptionRequest> {
        override fun createFromParcel(parcel: Parcel): OptionRequest {
            return OptionRequest(parcel)
        }

        override fun newArray(size: Int): Array<OptionRequest?> {
            return arrayOfNulls(size)
        }
    }
}
