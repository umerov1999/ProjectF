package dev.ragnarok.fenrir.fragment.narratives

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class NarrativesPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<INarrativesView>(accountId, savedInstanceState) {
    private val storiesInteractor: IStoriesShortVideosInteractor =
        InteractorFactory.createStoriesInteractor()
    private val mNarratives: ArrayList<Narratives> = ArrayList()
    private val netDisposable = CompositeJob()
    private var mEndOfContent = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.cancel()
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
            .fromIOToMain({ products ->
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

        appendJob(
            storiesInteractor.getStoryById(accountId, narrative.getStoriesIds())
                .fromIOToMain({
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
