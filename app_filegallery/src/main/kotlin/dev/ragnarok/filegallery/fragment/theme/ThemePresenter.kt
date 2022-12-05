package dev.ragnarok.filegallery.fragment.theme

import android.os.Bundle
import dev.ragnarok.filegallery.fragment.base.core.AbsPresenter
import dev.ragnarok.filegallery.settings.theme.ThemesController

class ThemePresenter(savedInstanceState: Bundle?) : AbsPresenter<IThemeView>(savedInstanceState) {

    override fun onGuiCreated(viewHost: IThemeView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(ThemesController.themes)
    }
}