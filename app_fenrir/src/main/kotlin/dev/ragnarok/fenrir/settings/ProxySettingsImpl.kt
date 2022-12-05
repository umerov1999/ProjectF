package dev.ragnarok.fenrir.settings

import android.content.Context
import android.content.SharedPreferences
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class ProxySettingsImpl(context: Context) : IProxySettings {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val addPublisher: PublishSubject<ProxyConfig> = PublishSubject.create()
    private val deletePublisher: PublishSubject<ProxyConfig> = PublishSubject.create()
    private val activePublisher: PublishSubject<Optional<ProxyConfig>> = PublishSubject.create()
    override fun put(address: String, port: Int) {
        val id = generateNextId()
        val config = ProxyConfig().set(id, address, port)
        put(config)
    }

    private fun put(config: ProxyConfig) {
        val set: MutableSet<String> =
            HashSet(preferences.getStringSet(KEY_LIST, HashSet(0)) ?: return)
        set.add(kJson.encodeToString(ProxyConfig.serializer(), config))
        preferences.edit()
            .putStringSet(KEY_LIST, set)
            .apply()
        addPublisher.onNext(config)
    }

    override fun put(address: String, port: Int, username: String, pass: String) {
        val id = generateNextId()
        val config = ProxyConfig().set(id, address, port).setAuth(username, pass)
        put(config)
    }

    override fun observeAdding(): Observable<ProxyConfig> {
        return addPublisher
    }

    override fun observeRemoving(): Observable<ProxyConfig> {
        return deletePublisher
    }

    override fun observeActive(): Observable<Optional<ProxyConfig>> {
        return activePublisher
    }

    override val all: MutableList<ProxyConfig>
        get() {
            val set = preferences.getStringSet(KEY_LIST, HashSet(0))!!
            val configs: MutableList<ProxyConfig> = ArrayList(
                set.size
            )
            for (s in set) {
                configs.add(kJson.decodeFromString(ProxyConfig.serializer(), s))
            }
            return configs
        }
    override val activeProxy: ProxyConfig?
        get() {
            val active = preferences.getString(KEY_ACTIVE, null)
            return if (active.nonNullNoEmpty()) kJson.decodeFromString(
                ProxyConfig.serializer(),
                active
            ) else null
        }

    override fun setActive(config: ProxyConfig?) {
        preferences.edit()
            .putString(
                KEY_ACTIVE,
                if (config == null) null else kJson.encodeToString(ProxyConfig.serializer(), config)
            )
            .apply()
        activePublisher.onNext(wrap(config))
    }

    override fun broadcastUpdate(config: ProxyConfig?) {
        if (config == null) {
            activePublisher.onNext(
                wrap(
                    activeProxy
                )
            )
        } else {
            activePublisher.onNext(wrap(config))
        }
    }

    override fun delete(config: ProxyConfig) {
        val set: MutableSet<String> =
            HashSet(preferences.getStringSet(KEY_LIST, HashSet(0)) ?: return)
        set.remove(kJson.encodeToString(ProxyConfig.serializer(), config))
        preferences.edit()
            .putStringSet(KEY_LIST, set)
            .apply()
        deletePublisher.onNext(config)
    }

    private fun generateNextId(): Int {
        val next = preferences.getInt(KEY_NEXT_ID, 1)
        preferences.edit()
            .putInt(KEY_NEXT_ID, next + 1)
            .apply()
        return next
    }

    companion object {
        private const val PREF_NAME = "proxy_settings"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_LIST = "list"
        private const val KEY_ACTIVE = "active_proxy"
    }

}