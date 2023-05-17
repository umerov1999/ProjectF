package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityblacklist

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.GroupSettingsInteractor
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.findIndexByPredicate
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime

class CommunityBlacklistPresenter(
    accountId: Long,
    private val groupId: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<ICommunityBlacklistView>(accountId, savedInstanceState) {
    private val data: MutableList<Banned>
    private val groupSettingsInteractor: IGroupSettingsInteractor
    private var loadingNow = false
    private var moreStartFrom: IntNextFrom
    private var endOfContent = false
    private fun onBanActionReceived(action: BanAction) {
        if (action.isBan) {
            //refresh data
            requestDataAtStart()
        } else {
            val index = findIndexByPredicate(
                data
            ) { it.banned.ownerId == action.ownerId }
            if (index != -1) {
                data.removeAt(index)
                view?.notifyItemRemoved(
                    index
                )
            }
        }
    }

    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        view?.displayRefreshing(
            loadingNow
        )
    }

    private fun requestDataAtStart() {
        request(IntNextFrom(0))
    }

    private fun request(startFrom: IntNextFrom) {
        if (loadingNow) return
        setLoadingNow(true)
        appendDisposable(groupSettingsInteractor.getBanned(accountId, groupId, startFrom, COUNT)
            .fromIOToMain()
            .subscribe(
                {
                    onBannedUsersReceived(
                        startFrom,
                        it.second,
                        it.first
                    )
                }
            ) { throwable -> onRequqestError(getCauseIfRuntime(throwable)) })
    }

    override fun onGuiCreated(viewHost: ICommunityBlacklistView) {
        super.onGuiCreated(viewHost)
        viewHost.diplayData(data)
    }

    private fun onRequqestError(throwable: Throwable) {
        setLoadingNow(false)
        throwable.printStackTrace()
        showError(throwable)
    }

    private fun onBannedUsersReceived(
        startFrom: IntNextFrom,
        nextFrom: IntNextFrom,
        users: List<Banned>
    ) {
        endOfContent = users.isEmpty()
        moreStartFrom = nextFrom
        if (startFrom.offset != 0) {
            val startSize = data.size
            data.addAll(users)
            view?.notifyItemsAdded(
                startSize,
                users.size
            )
        } else {
            data.clear()
            data.addAll(users)
            view?.notifyDataSetChanged()
        }
        setLoadingNow(false)
    }

    fun fireRefresh() {
        requestDataAtStart()
    }

    fun fireBannedClick(banned: Banned) {
        view?.openBanEditor(
            accountId,
            groupId,
            banned
        )
    }

    fun fireAddClick() {
        view?.startSelectProfilesActivity(
            accountId,
            groupId
        )
    }

    fun fireAddToBanUsersSelected(owners: ArrayList<Owner>) {
        val users = ArrayList<User>()
        for (i in owners) {
            if (i is User) {
                users.add(i)
            }
        }
        if (users.nonNullNoEmpty()) {
            view?.addUsersToBan(
                accountId,
                groupId,
                users
            )
        }
    }

    fun fireBannedRemoveClick(banned: Banned) {
        appendDisposable(groupSettingsInteractor
            .unban(accountId, groupId, banned.banned.ownerId)
            .fromIOToMain()
            .subscribe({ onUnbanComplete() }) { throwable ->
                onUnbanError(
                    throwable
                )
            })
    }

    private fun onUnbanComplete() {
        view?.customToast?.showToastSuccessBottom(
            R.string.deleted
        )
    }

    private fun onUnbanError(throwable: Throwable) {
        showError(
            throwable
        )
    }

    private fun canLoadMore(): Boolean {
        return !endOfContent && !loadingNow && data.nonNullNoEmpty() && moreStartFrom.offset > 0
    }

    fun fireScrollToBottom() {
        if (canLoadMore()) {
            request(moreStartFrom)
        }
    }

    companion object {
        private const val COUNT = 20
    }

    init {
        data = ArrayList()
        moreStartFrom = IntNextFrom(0)
        val networker = networkInterfaces
        val repository = stores.owners()
        groupSettingsInteractor = GroupSettingsInteractor(networker, repository, owners)
        appendDisposable(repository.observeBanActions()
            .filter { it.groupId == groupId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onBanActionReceived(it) })
        requestDataAtStart()
    }
}