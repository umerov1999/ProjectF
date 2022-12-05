package dev.ragnarok.fenrir.fragment.theme

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.settings.theme.ThemesController

class ThemePresenter(savedInstanceState: Bundle?) : AbsPresenter<IThemeView>(savedInstanceState) {

    override fun onGuiCreated(viewHost: IThemeView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(ThemesController.themes)
    }
}