package dev.ragnarok.fenrir.model.menu

import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.model.Text

class Section(val title: Text?) {

    @DrawableRes
    var icon: Int? = null
        private set

    fun setIcon(icon: Int?): Section {
        this.icon = icon
        return this
    }
}