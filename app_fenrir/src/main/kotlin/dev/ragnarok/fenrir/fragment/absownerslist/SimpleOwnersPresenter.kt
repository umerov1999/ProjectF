package dev.ragnarok.fenrir.fragment.absownerslist

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Owner

abstract class SimpleOwnersPresenter<V : ISimpleOwnersView>(
    accountId: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<V>(accountId, savedInstanceState) {
    val data: MutableList<Owner>
    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayOwnerList(data)
    }

    fun fireRefresh() {
        onUserRefreshed()
    }

    protected open fun onUserRefreshed() {}
    fun fireScrollToEnd() {
        onUserScrolledToEnd()
    }

    protected open fun onUserScrolledToEnd() {}

    fun fireOwnerClick(owner: Owner) {
        view?.showOwnerWall(accountId, owner)
    }

    init {
        data = ArrayList()
    }
}