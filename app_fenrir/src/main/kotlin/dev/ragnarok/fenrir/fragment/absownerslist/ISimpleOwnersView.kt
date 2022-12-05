package dev.ragnarok.fenrir.fragment.absownerslist

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner

interface ISimpleOwnersView : IMvpView, IErrorView {
    fun displayOwnerList(owners: List<Owner>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataRemoved(position: Int, count: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun showOwnerWall(accountId: Int, owner: Owner)
    fun updateTitle(@StringRes res: Int)
}