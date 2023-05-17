package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitylinks

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView

interface ICommunityLinksView : IErrorView, IMvpView {
    fun displayRefreshing(loadingNow: Boolean)
    fun notifyDataSetChanged()
    fun displayData(links: List<VKApiCommunity.Link>)
    fun openLink(link: String?)
}