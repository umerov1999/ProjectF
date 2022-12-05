package dev.ragnarok.fenrir.settings

import android.content.Context
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.IRecentChats

internal class RecentChatsSettings(app: Context) : IRecentChats {
    private val app: Context = app.applicationContext
    override fun get(acountid: Int): MutableList<RecentChat> {
        val recentChats: MutableList<RecentChat> = ArrayList()
        val stringSet = getPreferences(app)
            .getStringSet(recentChatKeyFor(acountid), null)
        if (stringSet.nonNullNoEmpty()) {
            for (s in stringSet) {
                try {
                    val recentChat: RecentChat = kJson.decodeFromString(RecentChat.serializer(), s)
                    recentChats.add(recentChat)
                } catch (ignored: Exception) {
                }
            }
        }
        return recentChats
    }

    override fun store(accountid: Int, chats: List<RecentChat>) {
        val target: MutableSet<String> = LinkedHashSet()
        for (item in chats) {
            if (item.aid != accountid) continue
            target.add(kJson.encodeToString(RecentChat.serializer(), item))
        }
        getPreferences(app)
            .edit()
            .putStringSet(recentChatKeyFor(accountid), target)
            .apply()
    }

    companion object {
        internal fun recentChatKeyFor(aid: Int): String {
            return "recent$aid"
        }
    }

}