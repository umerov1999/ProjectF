package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymembers

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CommunityMembersPresenter(
    accountId: Long,
    private val group_id: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<ICommunityMembersView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val mMembers: ArrayList<Owner> = ArrayList()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var netLoadingNow = false
    private var offset = 0
    private var filter: String? = null
    private val isNotFriendShow: Boolean = Settings.get().main().isOwnerInChangesMonitor(-group_id)
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    fun fireFilter(filter: String?) {
        this.filter = filter
        requestAtLast(false)
    }

    fun fireSearch() {
        view?.onSearch(accountId, group_id)
    }

    fun fireFilter() {
        view?.onOptions(filter)
    }

    private fun request(do_scan: Boolean) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(relationshipInteractor.getGroupMembers(
            accountId,
            group_id,
            offset,
            if (isNotFriendShow) 1000 else COUNT_PER_REQUEST, filter
        )
            .fromIOToMain()
            .subscribe({
                onNetDataReceived(
                    it, do_scan
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(data: List<Owner>, do_scan: Boolean) {
        if (do_scan && isNotFriendShow) {
            val not_members = ArrayList<Owner>()
            for (i in mMembers) {
                if (Utils.indexOf(data, i.ownerId) == -1) {
                    not_members.add(i)
                }
            }
            val add_members = ArrayList<Owner>()
            for (i in data) {
                if (Utils.indexOf(mMembers, i.ownerId) == -1) {
                    add_members.add(i)
                }
            }
            if (add_members.isNotEmpty() || not_members.isNotEmpty()) {
                view?.showModMembers(
                    add_members,
                    not_members,
                    accountId, -group_id
                )
            }
        }
        mEndOfContent = data.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mMembers.clear()
            mMembers.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mMembers.size
            mMembers.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
        offset += if (isNotFriendShow) 1000 else COUNT_PER_REQUEST
        resolveRefreshingView()
    }

    private fun requestAtLast(do_scan: Boolean) {
        offset = 0
        request(do_scan)
    }

    private fun requestNext() {
        request(false)
    }

    override fun onGuiCreated(viewHost: ICommunityMembersView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mMembers)
        resolveRefreshingView()
    }

    private fun canLoadMore(): Boolean {
        return mMembers.isNotEmpty() && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast(false)
    }

    fun fireUserClick(owner: Owner) {
        view?.openUserWall(
            accountId,
            owner
        )
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    private fun loadAllCachedData() {
        netDisposable.add(relationshipInteractor.getCachedGroupMembers(accountId, group_id)
            .fromIOToMain()
            .subscribe({
                mMembers.clear()
                mMembers.addAll(it)
                view?.notifyDataSetChanged()
                requestAtLast(mMembers.isNotEmpty())
            }) {
                requestAtLast(false)
            })
    }

    companion object {
        private const val COUNT_PER_REQUEST = 100
    }

    init {
        loadAllCachedData()
    }
}