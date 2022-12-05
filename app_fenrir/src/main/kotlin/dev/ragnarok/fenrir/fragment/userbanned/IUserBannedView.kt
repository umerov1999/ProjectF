package dev.ragnarok.fenrir.fragment.userbanned

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner

interface IUserBannedView : IMvpView, IErrorView {
    fun displayOwnerList(owners: List<Owner>)
    fun notifyItemsAdded(position: Int, count: Int)
    fun notifyDataSetChanged()
    fun notifyItemRemoved(position: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun startUserSelection(accountId: Int)
    fun showSuccessToast()
    fun scrollToPosition(position: Int)
    fun showOwnerProfile(accountId: Int, owner: Owner)
}