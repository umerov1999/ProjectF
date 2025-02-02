package dev.ragnarok.fenrir.fragment.fave.favelinks

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FaveLinksPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFaveLinksView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val links: MutableList<FaveLink> = ArrayList()
    private val cacheDisposable = CompositeJob()
    private val actualDisposable = CompositeJob()
    private var endOfContent = false
    private var actualDataReceived = false
    private var cacheLoading = false
    private var actualLoading = false
    private var doLoadTabs = false
    private var offsetPos = 0
    private fun loadCachedData() {
        cacheLoading = true
        cacheDisposable.add(
            faveInteractor.getCachedLinks(accountId)
                .fromIOToMain { onCachedDataReceived(it) }
        )
    }

    private fun loadActual(offset: Int) {
        actualLoading = true
        resolveRefreshingView()
        actualDisposable.add(
            faveInteractor.getLinks(accountId, COUNT, offset)
                .fromIOToMain({
                    onActualDataReceived(
                        it,
                        offset
                    )
                }) { t -> onActualGetError(t) })
    }

    private fun onActualGetError(t: Throwable) {
        actualLoading = false
        resolveRefreshingView()
        showError(getCauseIfRuntime(t))
    }

    private fun onActualDataReceived(data: List<FaveLink>, offset: Int) {
        cacheDisposable.clear()
        cacheLoading = false
        actualLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        offsetPos += COUNT
        if (offset == 0) {
            links.clear()
            links.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val tmp = Utils.stripEqualsWithCounter(data, links, COUNT)
            if (tmp.isEmpty()) {
                endOfContent = true
            } else {
                val sizeBefore = links.size
                links.addAll(tmp)
                view?.notifyDataAdded(
                    sizeBefore,
                    tmp.size
                )
            }
        }
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        offsetPos = 0
        loadActual(0)
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualLoading
        )
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        cacheLoading = false
        actualDisposable.clear()
        offsetPos = 0
        loadActual(0)
    }

    fun fireScrollToEnd() {
        if (actualDataReceived && !endOfContent && !cacheLoading && !actualLoading && links.nonNullNoEmpty()) {
            loadActual(offsetPos)
        }
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        actualDisposable.cancel()
        super.onDestroyed()
    }

    private fun onCachedDataReceived(links: List<FaveLink>) {
        cacheLoading = false
        this.links.clear()
        this.links.addAll(links)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IFaveLinksView) {
        super.onGuiCreated(viewHost)
        viewHost.displayLinks(links)
    }

    fun fireDeleteClick(link: FaveLink) {
        val id = link.id ?: return
        appendJob(
            faveInteractor.removeLink(accountId, id)
                .fromIOToMain({ onLinkRemoved(accountId, id) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    private fun onLinkRemoved(accountId: Long, id: String) {
        if (accountId != this.accountId) {
            return
        }
        for (i in links.indices) {
            if (links[i].id == id) {
                links.removeAt(i)
                view?.notifyItemRemoved(i)
                break
            }
        }
    }

    fun fireAdd(context: Context) {
        val root = View.inflate(context, R.layout.entry_link, null)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.enter_link)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                actualDisposable.add(
                    faveInteractor.addLink(
                        accountId,
                        root.findViewById<TextInputEditText>(R.id.edit_link).text.toString()
                            .trim()
                    )
                        .fromIOToMain({ fireRefresh() }) { t ->
                            showError(getCauseIfRuntime(t))
                        })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    fun fireLinkClick(link: FaveLink) {
        view?.openLink(accountId, link)
    }

    companion object {
        private const val COUNT = 50
    }

    init {
        loadCachedData()
    }
}