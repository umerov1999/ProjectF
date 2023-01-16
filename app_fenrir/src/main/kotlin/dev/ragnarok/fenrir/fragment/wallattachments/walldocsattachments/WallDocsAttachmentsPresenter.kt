package dev.ragnarok.fenrir.fragment.wallattachments.walldocsattachments

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.disposables.CompositeDisposable

class WallDocsAttachmentsPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallDocsAttachmentsView>(accountId, savedInstanceState) {
    private val mDocs: ArrayList<Document> = ArrayList()
    private val fInteractor: IWallsRepository = walls
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    override fun onGuiCreated(viewHost: IWallDocsAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mDocs)
        resolveToolbar()
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getWallNoCache(
            accountId,
            owner_id,
            offset,
            100,
            WallCriteria.MODE_ALL
        )
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun update(data: List<Post>) {
        for (i in data) {
            i.attachments?.docs.nonNullNoEmpty {
                mDocs.addAll(it)
            }
            if (i.hasCopyHierarchy()) i.getCopyHierarchy()?.let { update(it) }
        }
    }

    private fun onActualDataReceived(offset: Int, data: List<Post>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(
            2
        )
        if (offset == 0) {
            loaded = data.size
            mDocs.clear()
            update(data)
            resolveToolbar()
            view?.notifyDataSetChanged()
        } else {
            val startSize = mDocs.size
            loaded += data.size
            update(data)
            resolveToolbar()
            view?.notifyDataAdded(
                startSize,
                mDocs.size - startSize
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(
            actualDataLoading
        )
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            it.toolbarSubtitle(
                getString(
                    R.string.documents_count,
                    safeCountOf(mDocs)
                ) + " " + getString(R.string.posts_analized, loaded)
            )
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded)
            return false
        }
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    init {
        loadActualData(0)
    }
}