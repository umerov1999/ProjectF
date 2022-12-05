package dev.ragnarok.fenrir.model

import android.content.Context
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.horizontal.Entry

class DocFilter(val type: Int, @field:StringRes @param:StringRes val title: Int) : Entry {

    override var isActive = false
        private set

    override fun getTitle(context: Context): String {
        return context.getString(title)
    }

    fun setActive(active: Boolean): DocFilter {
        isActive = active
        return this
    }

    override val isCustom: Boolean
        get() = false

    object Type {
        const val ALL = 0
        const val TEXT = 1
        const val ARCHIVE = 2
        const val GIF = 3
        const val IMAGE = 4
        const val AUDIO = 5
        const val VIDEO = 6
        const val BOOKS = 7
        const val OTHER = 8
    }
}