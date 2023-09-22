package dev.ragnarok.fenrir.materialpopupmenu.builder

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.GravityInt
import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenuBuilder

sealed class NormalItemBuilder<T : NormalItemBuilder<T>> : AbstractItemBuilder<T>() {
    abstract override val data: MaterialPopupMenuBuilder.NormalItem.Data
    fun setLabelColor(@ColorInt labelColor: Int) = self().apply { data.labelColor = labelColor }
    fun setLabelTypeface(labelTypeface: Typeface) =
        self().apply { data.labelTypeface = labelTypeface }

    fun setLabelAlignment(@GravityInt labelAlignment: Int) =
        self().apply { data.labelAlignment = labelAlignment }

    fun setIcon(@DrawableRes iconRes: Int) = self().apply { data.icon = iconRes }
    fun setIcon(drawable: Drawable) = self().apply { data.iconDrawable = drawable }
    fun setIconColor(@ColorInt color: Int) = self().apply { data.iconColor = color }
}
