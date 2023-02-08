package dev.ragnarok.fenrir.fragment.likes

import android.os.Bundle
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.disposables.CompositeDisposable

class LikesListPresenter(
    accountId: Long,
    private val type: String,
    private val ownerId: Long,
    private val itemId: Int,
    private val filter: String,
    savedInstanceState: Bundle?
) : SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val likesInteractor: ILikesInteractor = InteractorFactory.createLikesInteractor()
    private val netDisposable = CompositeDisposable()
    private var endOfContent = false
    private var loadingNow = false

    //private int loadingOffset;
    private fun requestData(offset: Int) {
        loadingNow = true
        //this.loadingOffset = offset;
        resolveRefreshingView()
        netDisposable.add(likesInteractor.getLikes(
            accountId,
            type,
            ownerId,
            itemId,
            filter,
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

    private fun onDataReceived(offset: Int, owners: List<Owner>) {
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

    override fun onUserRefreshed() {
        netDisposable.clear()
        requestData(0)
    }

    override fun onUserScrolledToEnd() {
        if (!loadingNow && !endOfContent && data.nonNullNoEmpty()) {
            requestData(data.size)
        }
    }

    init {
        requestData(0)
    }
}