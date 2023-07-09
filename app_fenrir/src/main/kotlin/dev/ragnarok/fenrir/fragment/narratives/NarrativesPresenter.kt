package dev.ragnarok.fenrir.fragment.narratives

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.nonNullNoEmpty
import io.reactivex.rxjava3.disposables.CompositeDisposable

class NarrativesPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<INarrativesView>(accountId, savedInstanceState) {
    private val storiesInteractor: IStoriesShortVideosInteractor =
        InteractorFactory.createStoriesInteractor()
    private val mNarratives: ArrayList<Narratives> = ArrayList()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(storiesInteractor.getNarratives(
            accountId,
            owner_id,
            null,
            null
        )
            .fromIOToMain()
            .subscribe({ products ->
                onNetDataReceived(
                    offset,
                    products
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(offset: Int, stories: List<Narratives>) {
        mEndOfContent = true
        netLoadingNow = false
        if (offset == 0) {
            mNarratives.clear()
            mNarratives.addAll(stories)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mNarratives.size
            mNarratives.addAll(stories)
            view?.notifyDataAdded(
                startSize,
                stories.size
            )
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mNarratives.size)
    }

    override fun onGuiCreated(viewHost: INarrativesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mNarratives)
        resolveRefreshingView()
    }

    private fun canLoadMore(): Boolean {
        return mNarratives.isNotEmpty() && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireNarrativesOpen(narrative: Narratives) {
        netLoadingNow = true
        resolveRefreshingView()

        appendDisposable(
            storiesInteractor.getStoryById(accountId, narrative.getStoriesIds())
                .fromIOToMain()
                .subscribe({
                    netLoadingNow = false
                    resolveRefreshingView()
                    if (it.nonNullNoEmpty()) {
                        view?.onNarrativesOpen(accountId, ArrayList(it))
                    }
                }, {
                    onNetDataGetError(it)
                })
        )
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    init {
        requestAtLast()
    }
}
