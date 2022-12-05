package dev.ragnarok.fenrir.fragment.proxymanager

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.ProxyConfig

interface IProxyManagerView : IMvpView, IErrorView {
    fun displayData(configs: MutableList<ProxyConfig>, active: ProxyConfig?)
    fun notifyItemAdded(position: Int)
    fun notifyItemRemoved(position: Int)
    fun setActiveAndNotifyDataSetChanged(config: ProxyConfig?)
    fun goToAddingScreen()
}