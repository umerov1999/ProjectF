package dev.ragnarok.fenrir.fragment.base.horizontal

import android.content.Context

interface Entry {
    fun getTitle(context: Context): String?
    val isActive: Boolean
    val isCustom: Boolean
}