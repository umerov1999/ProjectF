package dev.ragnarok.fenrir.fragment.topics

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IBoardInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class TopicsPresenter(accountId: Long, private val ownerId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<ITopicsView>(accountId, savedInstanceState) {
    private val topics: MutableList<Topic> = ArrayList()
    private val boardInteractor: IBoardInteractor = InteractorFactory.createBoardInteractor()
    private val cacheDisposable = CompositeJob()
    private val netDisposable = CompositeJob()
    private var endOfContent = false
    private var actualDataReceived = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var netLoadingNowOffset = 0
    private fun loadCachedData() {
        cacheDisposable.add(
            boardInteractor.getCachedTopics(accountId, ownerId)
                .fromIOToMain { topics -> onCachedDataReceived(topics) }
        )
    }

    private fun onCachedDataReceived(topics: List<Topic>) {
        cacheLoadingNow = false
        this.topics.clear()
        this.topics.addAll(topics)
        view?.notifyDataSetChanged()
    }

    private fun requestActualData(offset: Int) {
        netLoadingNow = true
        netLoadingNowOffset = offset
        resolveRefreshingView()
        resolveLoadMoreFooter()
        netDisposable.add(
            boardInteractor.getActualTopics(
                accountId,
                ownerId,
                COUNT_PER_REQUEST,
                offset
            )
                .fromIOToMain({ topics ->
                    onActualDataReceived(
                        offset,
                        topics
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        resolveLoadMoreFooter()
        showError(t)
    }

    private fun onActualDataReceived(offset: Int, topics: List<Topic>) {
        cacheDisposable.clear()
        cacheLoadingNow = false
        netLoadingNow = false
        resolveRefreshingView()
        resolveLoadMoreFooter()
        actualDataReceived = true
        endOfContent = topics.isEmpty()
        if (offset == 0) {
            this.topics.clear()
            this.topics.addAll(topics)
            view?.notifyDataSetChanged()
        } else {
            val startCount = this.topics.size
            this.topics.addAll(topics)
            view?.notifyDataAdd(
                startCount,
                topics.size
            )
        }
    }

    override fun onGuiCreated(viewHost: ITopicsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(topics)
        resolveRefreshingView()
        resolveLoadMoreFooter()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        netDisposable.cancel()
        super.onDestroyed()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(netLoadingNow)
    }

    private fun resolveLoadMoreFooter() {
        if (netLoadingNow && netLoadingNowOffset > 0) {
            view?.setupLoadMore(LoadMoreState.LOADING)
            return
        }
        if (actualDataReceived && !netLoadingNow) {
            view?.setupLoadMore(LoadMoreState.CAN_LOAD_MORE)
        }
        view?.setupLoadMore(LoadMoreState.END_OF_LIST)
    }

    fun fireLoadMoreClick() {
        if (canLoadMore()) {
            requestActualData(topics.size)
        }
    }

    private fun canLoadMore(): Boolean {
        return actualDataReceived && !cacheLoadingNow && !endOfContent && topics.isNotEmpty()
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        cacheDisposable.clear()
        cacheLoadingNow = false
        requestActualData(0)
    }

    fun fireTopicClick(topic: Topic) {
        view?.goToComments(accountId, topic)
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestActualData(topics.size)
        }
    }

    companion object {
        private const val COUNT_PER_REQUEST = 20
    }

    init {
        loadCachedData()
        requestActualData(0)
    }
}