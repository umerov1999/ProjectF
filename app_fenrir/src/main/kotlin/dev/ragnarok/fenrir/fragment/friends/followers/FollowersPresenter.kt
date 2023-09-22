package dev.ragnarok.fenrir.fragment.friends.followers

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.Utils.indexOfOwner
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FollowersPresenter(accountId: Long, private val userId: Long, savedInstanceState: Bundle?) :
    SimpleOwnersPresenter<IFollowersView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val accountsInteractor: IAccountsInteractor =
        InteractorFactory.createAccountInteractor()
    private val actualDataDisposable = CompositeDisposable()
    private val cacheDisposable = CompositeDisposable()
    private val isNotFriendShow: Boolean = Settings.get().main().isOwnerInChangesMonitor(userId)
    private var actualDataLoading = false
    private var actualDataReceived = false
    private var endOfContent = false
    private var cacheLoadingNow = false
    private var doLoadTabs = false
    private var offset = 0
    private fun requestActualData(do_scan: Boolean) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(relationshipInteractor.getFollowers(
            accountId,
            userId,
            if (isNotFriendShow) 1000 else 200,
            offset
        )
            .fromIOToMain()
            .subscribe({ users ->
                onActualDataReceived(
                    users,
                    do_scan
                )
            }) { t -> onActualDataGetError(t) })
    }

    fun removeFollower(owner: Owner) {
        appendDisposable(
            accountsInteractor.banOwners(accountId, listOf(owner))
                .fromIOToMain()
                .subscribe({
                    val pos = indexOfOwner(data, owner)
                    if (pos >= 0) {
                        data.removeAt(pos)
                        view?.notifyRemoved(
                            pos
                        )
                    }
                }, ignore())
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        loadAllCacheData()
        if (!isNotFriendShow) {
            offset = 0
            requestActualData(false)
        }
    }

    private fun resolveRefreshingView() {
        view?.displayRefreshing(
            actualDataLoading
        )
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(users: List<User>, do_scan: Boolean) {
        if (do_scan && isNotFriendShow) {
            val not_followers = ArrayList<Owner>()
            for (i in data) {
                if (indexOf(users, i.ownerId) == -1) {
                    not_followers.add(i)
                }
            }
            val add_followers = ArrayList<Owner>()
            for (i in users) {
                if (indexOf(data, i.getOwnerObjectId()) == -1) {
                    add_followers.add(i)
                }
            }
            if (add_followers.isNotEmpty() || not_followers.isNotEmpty()) {
                view?.showModFollowers(
                    add_followers,
                    not_followers,
                    accountId, userId
                )
            }
        }
        actualDataLoading = false
        cacheDisposable.clear()
        actualDataReceived = true
        endOfContent = users.isEmpty()
        if (offset == 0) {
            data.clear()
            data.addAll(users)
            view?.notifyDataSetChanged()
        } else {
            val startSzie = data.size
            data.addAll(users)
            view?.notifyDataAdded(
                startSzie,
                users.size
            )
        }
        offset += if (isNotFriendShow) 1000 else 200
        resolveRefreshingView()
    }

    override fun onUserScrolledToEnd() {
        if (!endOfContent && !cacheLoadingNow && !actualDataLoading && actualDataReceived) {
            requestActualData(false)
        }
    }

    public override fun onUserRefreshed() {
        cacheDisposable.clear()
        cacheLoadingNow = false
        actualDataDisposable.clear()
        offset = 0
        requestActualData(false)
    }

    private fun loadAllCacheData() {
        cacheLoadingNow = true
        cacheDisposable.add(relationshipInteractor.getCachedFollowers(accountId, userId)
            .fromIOToMain()
            .subscribe({ users -> onCachedDataReceived(users) }) { t ->
                onCacheDataGetError(
                    t
                )
            })
    }

    private fun onCacheDataGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(getCauseIfRuntime(t))
        if (isNotFriendShow) {
            offset = 0
            requestActualData(false)
        }
    }

    private fun onCachedDataReceived(users: List<User>) {
        cacheLoadingNow = false
        data.addAll(users)
        view?.notifyDataSetChanged()
        if (isNotFriendShow) {
            offset = 0
            requestActualData(users.isNotEmpty())
        }
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

}