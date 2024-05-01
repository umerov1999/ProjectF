package dev.ragnarok.fenrir.fragment.theme

import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.settings.theme.ThemesController

class ThemePresenter : AbsPresenter<IThemeView>() {
    override fun onGuiCreated(viewHost: IThemeView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(ThemesController.themes)
    }
}