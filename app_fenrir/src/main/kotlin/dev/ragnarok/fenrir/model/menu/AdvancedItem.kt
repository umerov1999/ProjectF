package dev.ragnarok.fenrir.model.menu

import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Text

class AdvancedItem(
    val key: Long,
    val title: Text?,
    val type: Int = TYPE_DEFAULT,
    val autolink: Boolean = true
) {
    var urlPrefix: String? = null
        private set
    var icon: Icon? = null
        private set
    var subtitle: Text? = null
        private set
    var section: Section? = null
        private set
    var tag: Any? = null
        private set

    fun setUrlPrefix(urlPrefix: String?): AdvancedItem {
        this.urlPrefix = urlPrefix
        return this
    }

    fun setTag(tag: Any?): AdvancedItem {
        this.tag = tag
        return this
    }

    fun setSubtitle(subtitle: Text?): AdvancedItem {
        this.subtitle = subtitle
        return this
    }

    fun setSection(section: Section?): AdvancedItem {
        this.section = section
        return this
    }

    fun setIcon(icon: Icon?): AdvancedItem {
        this.icon = icon
        return this
    }

    fun setIcon(@DrawableRes iconRes: Int): AdvancedItem {
        icon = Icon.fromResources(iconRes)
        return this
    }

    fun setIcon(remoteUrl: String?): AdvancedItem {
        icon = Icon.fromUrl(remoteUrl)
        return this
    }

    companion object {
        const val TYPE_DEFAULT = 0
        const val TYPE_COPY_DETAILS_ONLY = 1
        const val TYPE_OPEN_URL = 2
    }
}