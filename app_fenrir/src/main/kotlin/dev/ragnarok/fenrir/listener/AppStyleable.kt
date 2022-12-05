package dev.ragnarok.fenrir.listener

interface AppStyleable {
    fun hideMenu(hide: Boolean)
    fun openMenu(open: Boolean)
    fun setStatusbarColored(colored: Boolean, invertIcons: Boolean)
}