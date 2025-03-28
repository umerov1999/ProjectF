package dev.ragnarok.fenrir.fragment.feed.newsfeedmentions

import android.os.Bundle
import dev.ragnarok.fenrir.domain.INewsfeedInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fragment.feed.newsfeedcomments.INewsfeedCommentsView
import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class NewsfeedMentionsPresenter(
    accountId: Long,
    private val ownerId: Long,
    savedInstanceState: Bundle?
) :
    PlaceSupportPresenter<INewsfeedCommentsView>(accountId, savedInstanceState) {
    private val data: MutableList<NewsfeedComment> = ArrayList()
    private val interactor: INewsfeedInteractor = InteractorFactory.createNewsfeedInteractor()
    private var isEndOfContent = false
    private var loadingNow = false
    private var offset: Int
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
        load(0)
    }

    private fun load(offset: Int) {
        appendJob(
            interactor.getMentions(accountId, ownerId, 50, offset, null, null)
                .fromIOToMain({
                    onDataReceived(
                        offset,
                        it.first
                    )
                }) { throwable -> onRequestError(throwable) })
    }

    private fun onRequestError(throwable: Throwable) {
        showError(getCauseIfRuntime(throwable))
        setLoadingNow(false)
    }

    private fun onDataReceived(offset: Int, comments: List<NewsfeedComment>) {
        setLoadingNow(false)
        this.offset = offset + 50
        isEndOfContent = comments.isEmpty()
        if (offset == 0) {
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
        return !isEndOfContent && !loadingNow
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            load(offset)
        }
    }

    fun fireRefresh() {
        if (loadingNow) {
            return
        }
        offset = 0
        loadAtLast()
    }

    fun fireCommentBodyClick(newsfeedComment: NewsfeedComment) {
        val comment = newsfeedComment.comment
        comment?.commented?.let {
            view?.openComments(
                accountId,
                it,
                null
            )
        }
    }

    init {
        offset = 0
        loadAtLast()
    }
}