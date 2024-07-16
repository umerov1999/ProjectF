package dev.ragnarok.fenrir.settings

import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.util.Optional
import kotlinx.coroutines.flow.SharedFlow

interface IProxySettings {
    fun put(address: String, port: Int)
    fun put(address: String, port: Int, username: String, pass: String)
    val observeAdding: SharedFlow<ProxyConfig>
    val observeRemoving: SharedFlow<ProxyConfig>
    val observeActive: SharedFlow<Optional<ProxyConfig>>
    val all: MutableList<ProxyConfig>
    val activeProxy: ProxyConfig?
    fun setActive(config: ProxyConfig?)
    fun broadcastUpdate(config: ProxyConfig?)
    fun delete(config: ProxyConfig)
}