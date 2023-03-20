package dev.ragnarok.fenrir.fragment.likes.storiesview

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.disposables.CompositeDisposable

class StoriesViewPresenter(
    accountId: Long,
    private val ownerId: Long,
    private val itemId: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IStoriesViewView>(accountId, savedInstanceState) {
    val data: MutableList<Pair<Owner, Boolean>> = ArrayList()
    private val storiesInteractor: IStoriesShortVideosInteractor =
        InteractorFactory.createStoriesInteractor()
    private val netDisposable = CompositeDisposable()
    private var endOfContent = false
    private var loadingNow = false

    override fun onGuiCreated(viewHost: IStoriesViewView) {
        super.onGuiCreated(viewHost)
        viewHost.displayOwnerList(data)
    }

    fun fireRefresh() {
        onUserRefreshed()
    }

    fun fireScrollToEnd() {
        onUserScrolledToEnd()
    }

    fun fireOwnerClick(owner: Owner) {
        view?.showOwnerWall(accountId, owner)
    }

    //private int loadingOffset;
    private fun requestData(offset: Int) {
        loadingNow = true
        //this.loadingOffset = offset;
        resolveRefreshingView()
        netDisposable.add(storiesInteractor.getStoriesViewers(
            accountId,
            ownerId,
            itemId,
            50,
            offset
        )
            .fromIOToMain()
            .subscribe({ owners ->
                onDataReceived(
                    offset,
                    owners
                )
            }) { t -> onDataGetError(t) })
    }

    private fun onDataGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
        loadingNow = false
        resolveRefreshingView()
    }

    private fun onDataReceived(offset: Int, owners: List<Pair<Owner, Boolean>>) {
        loadingNow = false
        endOfContent = owners.isEmpty()
        if (offset == 0) {
            data.clear()
            data.addAll(owners)
            view?.notifyDataSetChanged()
        } else {
            val sizeBefore = data.size
            data.addAll(owners)
            view?.notifyDataAdded(
                sizeBefore,
                owners.size
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun onUserRefreshed() {
        netDisposable.clear()
        requestData(0)
    }

    private fun onUserScrolledToEnd() {
        if (!loadingNow && !endOfContent && data.nonNullNoEmpty()) {
            requestData(data.size)
        }
    }

    init {
        requestData(0)
    }
}