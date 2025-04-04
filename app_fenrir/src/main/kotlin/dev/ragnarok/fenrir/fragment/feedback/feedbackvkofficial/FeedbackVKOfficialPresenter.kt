package dev.ragnarok.fenrir.fragment.feedback.feedbackvkofficial

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFeedbackInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FeedbackVKOfficialPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFeedbackVKOfficialView>(accountId, savedInstanceState) {
    private val pages: FeedbackVKOfficialList = FeedbackVKOfficialList()
    private val fInteractor: IFeedbackInteractor
    private val actualDataDisposable = CompositeJob()
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    override fun onGuiCreated(viewHost: IFeedbackVKOfficialView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(pages)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(
            fInteractor.getActualFeedbacksOfficial(accountId, COUNT, offset)
                .fromIOToMain({ data ->
                    onActualDataReceived(
                        offset,
                        data
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun loadCachedData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(
            fInteractor.getCachedFeedbacksOfficial(accountId)
                .fromIOToMain({ data ->
                    onCachedDataReceived(
                        data
                    )
                    loadActualData(0)
                }) {
                    actualDataLoading = false
                    resolveRefreshingView()
                    it.printStackTrace()
                    loadActualData(0)
                })
    }

    fun hideNotification(position: Int, query: String?) {
        if (Utils.isHiddenAccount(accountId)) {
            return
        }
        actualDataDisposable.add(
            fInteractor.hide(accountId, query)
                .fromIOToMain({
                    pages.items?.removeAt(position)
                    view?.notifyItemRemoved(
                        position
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: FeedbackVKOfficialList) {
        actualDataLoading = false
        endOfContent = (data.items?.size ?: 0) < COUNT
        actualDataReceived = true
        if (offset == 0) {
            pages.items?.clear()
            data.items?.let { pages.items?.addAll(it) }
            view?.notifyFirstListReceived()
        } else {
            val startSize = pages.items?.size.orZero()
            data.items?.let { pages.items?.addAll(it) }
            view?.notifyDataAdded(
                startSize,
                data.items?.size.orZero()
            )
        }
        resolveRefreshingView()
    }

    private fun onCachedDataReceived(data: FeedbackVKOfficialList) {
        actualDataLoading = false
        actualDataReceived = true
        pages.items?.clear()
        data.items?.let { pages.items?.addAll(it) }
        view?.notifyFirstListReceived()
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && pages.items.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            loadActualData(pages.items?.size.orZero())
            return false
        }
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    companion object {
        private const val COUNT = 100
    }

    init {
        pages.items = ArrayList()
        fInteractor = InteractorFactory.createFeedbackInteractor()
        loadCachedData()
    }
}