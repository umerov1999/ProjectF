package dev.ragnarok.fenrir.fragment.proxymanager

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain

class ProxyManagerPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IProxyManagerView>(savedInstanceState) {
    private val settings: IProxySettings = proxySettings
    private val configs: MutableList<ProxyConfig> = settings.all
    private fun onActiveChanged(config: ProxyConfig?) {
        view?.setActiveAndNotifyDataSetChanged(
            config
        )
    }

    private fun onProxyDeleted(config: ProxyConfig) {
        val index = findIndexById(configs, config.getObjectId())
        if (index != -1) {
            configs.removeAt(index)
            view?.notifyItemRemoved(
                index
            )
        }
    }

    private fun onProxyAdded(config: ProxyConfig) {
        configs.add(0, config)
        view?.notifyItemAdded(0)
    }

    override fun onGuiCreated(viewHost: IProxyManagerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(configs, settings.activeProxy)
    }

    fun fireDeleteClick(config: ProxyConfig) {
        if (config == settings.activeProxy) {
            showError(Exception("Proxy is active. First, disable the proxy"))
            return
        }
        settings.delete(config)
    }

    fun fireActivateClick(config: ProxyConfig) {
        settings.setActive(config)
    }

    fun fireDisableClick() {
        settings.setActive(null)
    }

    fun fireAddClick() {
        view?.goToAddingScreen()
    }

    init {
        appendJob(settings.observeAdding
            .sharedFlowToMain { onProxyAdded(it) })
        appendJob(settings.observeRemoving
            .sharedFlowToMain { onProxyDeleted(it) })
        appendJob(settings.observeActive
            .sharedFlowToMain { onActiveChanged(it.get()) })
    }
}