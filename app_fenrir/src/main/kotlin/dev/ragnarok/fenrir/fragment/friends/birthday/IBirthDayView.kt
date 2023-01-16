package dev.ragnarok.fenrir.fragment.friends.birthday

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.BirthDay
import dev.ragnarok.fenrir.model.User

interface IBirthDayView : IMvpView, IErrorView {
    fun displayData(users: List<BirthDay>)
    fun notifyDataSetChanged()
    fun showRefreshing(refreshing: Boolean)
    fun goToWall(accountId: Long, user: User)
    fun moveTo(pos: Int)
}
