package dev.ragnarok.fenrir.settings

import android.content.Context
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import java.util.Collections

class NotificationsPrefs internal constructor(context: Context) : INotificationSettings {
    private val app: Context = context.applicationContext
    private val silentPeers: MutableSet<String> = Collections.synchronizedSet(HashSet(1))
    private val silentTypes: MutableMap<String, Boolean> = Collections.synchronizedMap(HashMap(1))
    override val silentPeersMap: Map<String, Boolean>
        get() = HashMap(silentTypes)

    override fun reloadSilentSettings(onlyRoot: Boolean) {
        val preferences = getPreferences(app)
        silentPeers.clear()
        silentPeers.addAll(preferences.getStringSet(KEY_PEERS_UIDS, HashSet(1)) ?: return)
        if (onlyRoot) {
            return
        }
        silentTypes.clear()
        for (i in silentPeers) {
            silentTypes[i] = preferences.getBoolean(i, false)
        }
    }

    override fun setSilentPeer(aid: Long, peerid: Long, silent: Boolean) {
        val preferences = getPreferences(app)
        silentPeers.add(keyFor(aid, peerid))
        silentTypes[keyFor(aid, peerid)] = silent
        preferences.edit()
            .putBoolean(keyFor(aid, peerid), silent)
            .putStringSet(KEY_PEERS_UIDS, silentPeers)
            .apply()
    }

    override fun isSilentPeer(aid: Long, peerId: Long): Boolean {
        if (silentTypes.containsKey(keyFor(aid, peerId))) {
            return silentTypes[keyFor(aid, peerId)] == true
        }
        return false
    }

    override fun resetAll() {
        val preferences = getPreferences(app)
        for (i in silentPeers) {
            preferences.edit().remove(i).apply()
        }
        silentPeers.clear()
        silentTypes.clear()
        preferences.edit()
            .putStringSet(KEY_PEERS_UIDS, silentPeers)
            .apply()
    }

    override fun resetAccount(aid: Long) {
        val preferences = getPreferences(app)
        for (i in HashSet(silentPeers)) {
            if (i.contains(keyForAccount(aid))) {
                silentPeers.remove(i)
                silentTypes.remove(i)
                preferences.edit().remove(i).apply()
            }
        }
        preferences.edit()
            .putStringSet(KEY_PEERS_UIDS, silentPeers)
            .apply()
    }

    companion object {
        private const val KEY_PEERS_UIDS = "silent_peer_uids"
        internal fun keyFor(aid: Long, peerId: Long): String {
            return "silent_peer_" + aid + "_" + peerId
        }

        internal fun keyForAccount(aid: Long): String {
            return "silent_peer_$aid"
        }
    }

    init {
        reloadSilentSettings(false)
    }
}