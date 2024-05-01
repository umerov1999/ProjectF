package dev.ragnarok.fenrir.fragment.navigationedit

import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.swap

class SideDrawerEditPresenter :
    AbsPresenter<IDrawerEditView>() {
    private var data: ArrayList<DrawerCategory> =
        ArrayList(Settings.get().sideDrawerSettings().categoriesOrder)

    override fun onGuiCreated(viewHost: IDrawerEditView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun save() {
        Settings.get().sideDrawerSettings().categoriesOrder = data
    }

    fun fireResetClick() {
        data = ArrayList(Settings.get().sideDrawerSettings().categoriesOrder)
        view?.displayData(data)
    }

    fun fireSaveClick() {
        save()
        view?.goBackAndApplyChanges()
    }

    fun fireItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                data.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                data.swap(i, i - 1)
            }
        }
    }
}