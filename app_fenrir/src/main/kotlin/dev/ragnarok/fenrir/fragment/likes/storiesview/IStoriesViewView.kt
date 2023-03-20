package dev.ragnarok.fenrir.fragment.likes.storiesview

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner

interface IStoriesViewView : IMvpView, IErrorView {
    fun displayOwnerList(owners: List<Pair<Owner, Boolean>>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataRemoved(position: Int, count: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun showOwnerWall(accountId: Long, owner: Owner)
    fun updateTitle(@StringRes res: Int)
}
