package dev.ragnarok.fenrir.fragment.fave.favelinks

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FaveLinksPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFaveLinksView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor
    private val links: MutableList<FaveLink>
    private val cacheDisposable = CompositeDisposable()
    private val actualDisposable = CompositeDisposable()
    private var endOfContent = false
    private var actualDataReceived = false
    private var cacheLoading = false
    private var actualLoading = false
    private var doLoadTabs = false
    private fun loadCachedData() {
        cacheLoading = true
        cacheDisposable.add(
            faveInteractor.getCachedLinks(accountId)
                .fromIOToMain()
                .subscribe({ onCachedDataReceived(it) }, ignore())
        )
    }

    private fun loadActual(offset: Int) {
        actualLoading = true
        resolveRefreshingView()
        actualDisposable.add(faveInteractor.getLinks(accountId, getCount, offset)
            .fromIOToMain()
            .subscribe({
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
        endOfContent = safeCountOf(data) < getCount
        actualDataReceived = true
        if (offset == 0) {
            links.clear()
            links.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val sizeBefore = links.size
            links.addAll(data)
            view?.notifyDataAdded(
                sizeBefore,
                data.size
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
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
        loadActual(0)
    }

    fun fireScrollToEnd() {
        if (actualDataReceived && !endOfContent && !cacheLoading && !actualLoading && links.nonNullNoEmpty()) {
            loadActual(links.size)
        }
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        actualDisposable.dispose()
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
        appendDisposable(faveInteractor.removeLink(accountId, id)
            .fromIOToMain()
            .subscribe({ onLinkRemoved(accountId, id) }) { t ->
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
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                actualDisposable.add(faveInteractor.addLink(
                    accountId,
                    (root.findViewById<View>(R.id.edit_link) as TextInputEditText).text.toString()
                        .trim { it <= ' ' })
                    .fromIOToMain()
                    .subscribe({ fireRefresh() }) { t ->
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
        private const val getCount = 50
    }

    init {
        links = ArrayList()
        faveInteractor = InteractorFactory.createFaveInteractor()
        loadCachedData()
    }
}