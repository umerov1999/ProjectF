package dev.ragnarok.filegallery.fragment.theme

import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.settings.theme.ThemeValue

interface IThemeView : IMvpView {
    fun displayData(data: Array<ThemeValue>)
}