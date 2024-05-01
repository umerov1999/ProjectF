package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitylinks

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain

class CommunityLinksPresenter(
    accountId: Long,
    private val groupId: Long,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<ICommunityLinksView>(accountId, savedInstanceState) {
    private val networker: INetworker = networkInterfaces
    private val links: MutableList<VKApiCommunity.Link> = ArrayList()
    private var loadingNow = false
    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    override fun onGuiCreated(viewHost: ICommunityLinksView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(links)
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    private fun requestLinks() {
        setLoadingNow(true)
        appendDisposable(networker.vkDefault(accountId)
            .groups()
            .getById(listOf(groupId), null, null, "links")
            .map { dtos ->
                if (dtos.groups.isNullOrEmpty()) {
                    throw NotFoundException()
                }
                val links = dtos.groups?.get(0)?.links
                links ?: emptyList()
            }
            .fromIOToMain()
            .subscribe({ onLinksReceived(it) }) { throwable ->
                onRequestError(
                    throwable
                )
            })
    }

    private fun onRequestError(throwable: Throwable) {
        setLoadingNow(false)
        showError(throwable)
    }

    private fun onLinksReceived(links: List<VKApiCommunity.Link>) {
        setLoadingNow(false)
        this.links.clear()
        this.links.addAll(links)
        view?.notifyDataSetChanged()
    }

    fun fireRefresh() {
        requestLinks()
    }

    fun fireLinkClick(link: VKApiCommunity.Link) {
        view?.openLink(link.url)
    }

    fun fireLinkEditClick() {}
    fun fireLinkDeleteClick() {}
    fun fireButtonAddClick() {}

    init {
        requestLinks()
    }
}