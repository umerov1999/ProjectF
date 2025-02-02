package dev.ragnarok.fenrir.fragment.feed.newsfeedcomments

import android.os.Bundle
import dev.ragnarok.fenrir.domain.INewsfeedInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class NewsfeedCommentsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    PlaceSupportPresenter<INewsfeedCommentsView>(accountId, savedInstanceState) {
    private val data: MutableList<NewsfeedComment> = ArrayList()
    private val interactor: INewsfeedInteractor = InteractorFactory.createNewsfeedInteractor()
    private var nextFrom: String? = null
    private var loadingNow = false
    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveLoadingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        resumedView?.showLoading(
            loadingNow
        )
    }

    private fun loadAtLast() {
        setLoadingNow(true)
        load(null)
    }

    private fun load(startFrom: String?) {
        appendJob(
            interactor.getNewsfeedComments(
                accountId,
                10,
                startFrom,
                "post,photo,video,topic"
            )
                .fromIOToMain({
                    onDataReceived(
                        startFrom,
                        it.second,
                        it.first
                    )
                }) { throwable -> onRequestError(throwable) })
    }

    private fun loadNext() {
        setLoadingNow(true)
        val startFrom = nextFrom
        load(startFrom)
    }

    private fun onRequestError(throwable: Throwable) {
        showError(getCauseIfRuntime(throwable))
        setLoadingNow(false)
    }

    private fun onDataReceived(
        startFrom: String?,
        newNextFrom: String?,
        comments: List<NewsfeedComment>
    ) {
        setLoadingNow(false)
        val atLast = startFrom.isNullOrEmpty()
        nextFrom = newNextFrom
        if (atLast) {
            data.clear()
            data.addAll(comments)
            view?.notifyDataSetChanged()
        } else {
            val startCount = data.size
            data.addAll(comments)
            view?.notifyDataAdded(
                startCount,
                comments.size
            )
        }
    }

    override fun onGuiCreated(viewHost: INewsfeedCommentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun canLoadMore(): Boolean {
        return nextFrom.nonNullNoEmpty() && !loadingNow
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            loadNext()
        }
    }

    fun fireRefresh() {
        if (loadingNow) {
            return
        }
        loadAtLast()
    }

    fun fireCommentBodyClick(newsfeedComment: NewsfeedComment) {
        val comment = newsfeedComment.comment
        if (comment != null) {
            view?.openComments(
                accountId,
                comment.commented,
                null
            )
        }
    }

    init {
        loadAtLast()
    }
}