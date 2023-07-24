package dev.ragnarok.fenrir.settings

import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.util.Optional
import io.reactivex.rxjava3.core.Observable

interface IProxySettings {
    fun put(address: String, port: Int)
    fun put(address: String, port: Int, username: String, pass: String)
    val observeAdding: Observable<ProxyConfig>
    val observeRemoving: Observable<ProxyConfig>
    val observeActive: Observable<Optional<ProxyConfig>>
    val all: MutableList<ProxyConfig>
    val activeProxy: ProxyConfig?
    fun setActive(config: ProxyConfig?)
    fun broadcastUpdate(config: ProxyConfig?)
    fun delete(config: ProxyConfig)
}