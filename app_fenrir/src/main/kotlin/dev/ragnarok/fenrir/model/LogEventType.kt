package dev.ragnarok.fenrir.model

import android.content.Context
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.horizontal.Entry

class LogEventType(private val type: Int, @field:StringRes private val title: Int) : Entry {
    private var active = false
    fun getType(): Int {
        return type
    }

    override fun getTitle(context: Context): String {
        return context.getString(title)
    }

    override val isActive: Boolean
        get() = active

    fun setActive(active: Boolean): LogEventType {
        this.active = active
        return this
    }

    override val isCustom: Boolean
        get() = false
}