package dev.ragnarok.fenrir.fragment.friends.birthday

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.BirthDay
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import java.util.Calendar

class BirthDayPresenter(accountId: Long, private val ownerId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IBirthDayView>(accountId, savedInstanceState) {
    private val users: MutableList<BirthDay> = ArrayList()
    private val cacheInteractor: IOwnersRepository = Repository.owners
    private val cacheDisposable = CompositeJob()
    private var cacheLoadingNow = false
    private fun loadCachedData() {
        cacheLoadingNow = true
        resolveRefreshingView()
        cacheDisposable.add(
            cacheInteractor.findFriendBirtday(ownerId)
                .fromIOToMain({ onCachedDataReceived(it) }, {
                    cacheLoadingNow = false
                    resolveRefreshingView()
                })
        )
    }

    private fun onCachedDataReceived(users: List<User>) {
        cacheLoadingNow = false
        resolveRefreshingView()
        this.users.clear()
        val ks = ArrayList<BirthDay>(users.size)
        for (i in users) {
            ks.add(BirthDay(i))
        }
        ks.sortBy {
            it.sortVt
        }
        this.users.addAll(ks)
        view?.notifyDataSetChanged()
        val ps = Calendar.getInstance().get(Calendar.MONTH) + 1
        var pos = 0
        for ((tmpPos, i) in this.users.withIndex()) {
            if (i.month == ps) {
                pos = tmpPos
                break
            }
        }
        view?.moveTo(pos)
    }

    override fun onGuiCreated(viewHost: IBirthDayView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(users)
        resolveRefreshingView()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        super.onDestroyed()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(cacheLoadingNow)
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        cacheLoadingNow = false
        loadCachedData()
    }

    fun fireUserClick(user: User) {
        view?.goToWall(accountId, user)
    }

    init {
        loadCachedData()
    }
}
