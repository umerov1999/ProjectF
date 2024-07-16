package dev.ragnarok.fenrir.fragment.feedback

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFeedbackInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FeedbackPresenter(accountId: Long, savedInstanceState: Bundle?) :
    PlaceSupportPresenter<IFeedbackView>(accountId, savedInstanceState) {
    private val mData: MutableList<Feedback> = ArrayList()
    private val feedbackInteractor: IFeedbackInteractor =
        InteractorFactory.createFeedbackInteractor()
    private val cacheDisposable = CompositeJob()
    private val netDisposable = CompositeJob()
    private var mNextFrom: String? = null
    private var actualDataReceived = false
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var netLoadingStartFrom: String? = null
    private fun resolveLoadMoreFooter() {
        if (mData.isEmpty()) {
            view?.configLoadMore(LoadMoreState.INVISIBLE)
            return
        }
        if (mData.nonNullNoEmpty() && netLoadingNow && netLoadingStartFrom.nonNullNoEmpty()) {
            view?.configLoadMore(LoadMoreState.LOADING)
            return
        }
        if (canLoadMore()) {
            view?.configLoadMore(LoadMoreState.CAN_LOAD_MORE)
            return
        }
        view?.configLoadMore(LoadMoreState.END_OF_LIST)
    }

    private fun requestActualData(startFrom: String?) {
        netDisposable.clear()
        netLoadingNow = true
        netLoadingStartFrom = startFrom
        resolveLoadMoreFooter()
        resolveSwipeRefreshLoadingView()
        netDisposable.add(feedbackInteractor.getActualFeedbacks(
            accountId,
            COUNT_PER_REQUEST,
            startFrom
        )
            .fromIOToMain({
                onActualDataReceived(
                    startFrom,
                    it.first,
                    it.second
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        t.printStackTrace()
        netLoadingNow = false
        netLoadingStartFrom = null
        showError(getCauseIfRuntime(t))
        resolveLoadMoreFooter()
        resolveSwipeRefreshLoadingView()
    }

    private fun onActualDataReceived(
        startFrom: String?,
        feedbacks: List<Feedback>,
        nextFrom: String?
    ) {
        cacheDisposable.clear()
        cacheLoadingNow = false
        netLoadingNow = false
        netLoadingStartFrom = null
        mNextFrom = nextFrom
        mEndOfContent = nextFrom.isNullOrEmpty()
        actualDataReceived = true
        if (startFrom.isNullOrEmpty()) {
            mData.clear()
            mData.addAll(feedbacks)
            view?.notifyFirstListReceived()
        } else {
            val sizeBefore = mData.size
            mData.addAll(feedbacks)
            view?.notifyDataAdding(
                sizeBefore,
                feedbacks.size
            )
        }
        resolveLoadMoreFooter()
        resolveSwipeRefreshLoadingView()
    }

    private fun resolveSwipeRefreshLoadingView() {
        view?.showLoading(
            netLoadingNow && netLoadingStartFrom
                .isNullOrEmpty()
        )
    }

    private fun canLoadMore(): Boolean {
        return mNextFrom.nonNullNoEmpty() && !mEndOfContent && !cacheLoadingNow && !netLoadingNow && actualDataReceived
    }

    override fun onGuiCreated(viewHost: IFeedbackView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mData)
        resolveLoadMoreFooter()
        resolveSwipeRefreshLoadingView()
    }

    private fun loadAllFromDb() {
        cacheLoadingNow = true
        cacheDisposable.add(feedbackInteractor.getCachedFeedbacks(accountId)
            .fromIOToMain({ onCachedDataReceived(it) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(feedbacks: List<Feedback>) {
        cacheLoadingNow = false
        mData.clear()
        mData.addAll(feedbacks)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        netDisposable.cancel()
        super.onDestroyed()
    }

    fun fireItemClick(notification: Feedback) {
        view?.showLinksDialog(
            accountId,
            notification
        )
    }

    fun fireLoadMoreClick() {
        if (canLoadMore()) {
            requestActualData(mNextFrom)
        }
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        cacheLoadingNow = false
        netDisposable.clear()
        netLoadingNow = false
        netLoadingStartFrom = null
        requestActualData(null)
    }

    fun fireScrollToLast() {
        if (canLoadMore()) {
            requestActualData(mNextFrom)
        }
    }

    companion object {
        private const val COUNT_PER_REQUEST = 15
    }

    init {
        loadAllFromDb()
        requestActualData(null)
    }
}