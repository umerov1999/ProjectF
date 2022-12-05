package dev.ragnarok.fenrir.fragment.theme

import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.settings.theme.ThemeValue

interface IThemeView : IMvpView {
    fun displayData(data: Array<ThemeValue>)
}