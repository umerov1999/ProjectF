package dev.ragnarok.fenrir.fragment.messages.conversationattachments.abschatattachments

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.Flow

abstract class BaseChatAttachmentsPresenter<T, V : IBaseChatAttachmentsView<T>> internal constructor(
    private val peerId: Long, accountId: Long, savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState) {
    val data: MutableList<T> = ArrayList()
    private var nextFrom: String? = null
    private var endOfContent = false
    private var loadingHolder = CompositeJob()
    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayAttachments(data)
        resolveEmptyTextVisibility()
    }

    override fun onDestroyed() {
        loadingHolder.cancel()
        super.onDestroyed()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.showLoading(loadingHolder.size() > 0)
    }

    private fun initLoading() {
        load(null)
    }

    private fun load(startFrom: String?) {
        loadingHolder.add(
            requestAttachments(peerId, startFrom)
                .fromIOToMain(
                    {
                        onDataReceived(
                            startFrom,
                            it
                        )
                    },
                    { throwable ->
                        onRequestError(
                            Utils.getCauseIfRuntime(
                                throwable
                            )
                        )
                    })
        )
        resolveLoadingView()
    }

    private fun onRequestError(throwable: Throwable) {
        loadingHolder.clear()
        resolveLoadingView()
        view?.showError(throwable.message)
    }

    private fun onDataReceived(startFrom: String?, result: Pair<String?, List<T>>) {
        loadingHolder.clear()
        resolveLoadingView()
        nextFrom = result.first
        endOfContent = nextFrom.isNullOrEmpty()
        val newData = result.second
        if (startFrom != null) {
            val startSize = data.size
            data.addAll(newData)
            view?.notifyDataAdded(startSize, newData.size)
        } else {
            data.clear()
            data.addAll(newData)
            view?.notifyDatasetChanged()
        }
        resolveEmptyTextVisibility()
        onDataChanged()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(data.isEmpty())
    }

    open fun onDataChanged() {}
    private fun canLoadMore(): Boolean {
        return !endOfContent && loadingHolder.size() <= 0
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            load(nextFrom)
        }
    }

    fun fireRefresh() {
        loadingHolder.clear()
        nextFrom = null
        initLoading()
    }

    abstract fun requestAttachments(
        peerId: Long,
        nextFrom: String?
    ): Flow<Pair<String?, List<T>>>

    init {
        initLoading()
    }
}