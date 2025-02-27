package dev.ragnarok.fenrir.fragment.shortedlinks

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class ShortedLinksPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IShortedLinksView>(accountId, savedInstanceState) {
    private val links: MutableList<ShortLink> = ArrayList()
    private val fInteractor: IUtilsInteractor = InteractorFactory.createUtilsInteractor()
    private val actualDataDisposable = CompositeJob()
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private var mInput: String? = null
    override fun onGuiCreated(viewHost: IShortedLinksView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(links)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(
            fInteractor.getLastShortenedLinks(accountId, 10, offset)
                .fromIOToMain({
                    onActualDataReceived(
                        offset,
                        it
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: List<ShortLink>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (offset == 0) {
            links.clear()
            links.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = links.size
            links.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && links.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            loadActualData(links.size)
            return false
        }
        return true
    }

    fun fireDelete(index: Int, link: ShortLink) {
        actualDataDisposable.add(
            fInteractor.deleteFromLastShortened(accountId, link.key)
                .fromIOToMain({
                    links.removeAt(index)
                    view?.notifyDataSetChanged()
                }) { t -> onActualDataGetError(t) })
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    fun fireInputEdit(s: CharSequence?) {
        mInput = s.toString()
    }

    fun fireShort() {
        actualDataDisposable.add(
            fInteractor.getShortLink(accountId, mInput, 1)
                .fromIOToMain({ data ->
                    data.setTimestamp(now())
                    data.setViews(0)
                    links.add(0, data)
                    view?.notifyDataSetChanged()
                    view?.updateLink(
                        data.short_url
                    )
                }) { t -> onActualDataGetError(t) })
    }

    fun fireValidate() {
        actualDataDisposable.add(
            fInteractor.checkLink(accountId, mInput)
                .fromIOToMain({ data ->
                    view?.updateLink(
                        data.link
                    )
                    view?.showLinkStatus(
                        data.status
                    )
                }) { t -> onActualDataGetError(t) })
    }

    init {
        loadActualData(0)
    }
}