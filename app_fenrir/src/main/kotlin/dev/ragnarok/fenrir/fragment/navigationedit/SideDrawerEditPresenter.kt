package dev.ragnarok.fenrir.fragment.navigationedit

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.settings.Settings
import java.util.Collections

class SideDrawerEditPresenter(savedInstanceState: Bundle?) :
    AbsPresenter<IDrawerEditView>(savedInstanceState) {
    private var data: List<DrawerCategory> = Settings.get().sideDrawerSettings().categoriesOrder

    override fun onGuiCreated(viewHost: IDrawerEditView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun save() {
        Settings.get().sideDrawerSettings().categoriesOrder = data
    }

    fun fireResetClick() {
        data = Settings.get().sideDrawerSettings().categoriesOrder
        view?.displayData(data)
    }

    fun fireSaveClick() {
        save()
        view?.goBackAndApplyChanges()
    }

    fun fireItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(data, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(data, i, i - 1)
            }
        }
    }
}