package dev.ragnarok.fenrir.model.menu

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Text

class Item(val key: Int, val title: Text?) {
    var icon: Icon? = null
        private set
    var section: Section? = null
        private set

    @ColorInt
    var color: Int? = null
        private set
    var extra = 0
        private set

    fun setExtra(extra: Int): Item {
        this.extra = extra
        return this
    }

    fun setColor(@ColorInt color: Int): Item {
        this.color = color
        return this
    }

    fun setSection(section: Section?): Item {
        this.section = section
        return this
    }

    fun setIcon(@DrawableRes res: Int): Item {
        icon = Icon.fromResources(res)
        return this
    }

    fun setIcon(remoteUrl: String?): Item {
        icon = Icon.fromUrl(remoteUrl)
        return this
    }

    fun setIcon(icon: Icon?): Item {
        this.icon = icon
        return this
    }
}