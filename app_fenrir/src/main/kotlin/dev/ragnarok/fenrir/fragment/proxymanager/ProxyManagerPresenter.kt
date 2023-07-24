package dev.ragnarok.fenrir.fragment.proxymanager

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.util.Utils.findIndexById

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
        appendDisposable(settings.observeAdding
            .observeOn(provideMainThreadScheduler())
            .subscribe { onProxyAdded(it) })
        appendDisposable(settings.observeRemoving
            .observeOn(provideMainThreadScheduler())
            .subscribe { onProxyDeleted(it) })
        appendDisposable(settings.observeActive
            .observeOn(provideMainThreadScheduler())
            .subscribe { onActiveChanged(it.get()) })
    }
}