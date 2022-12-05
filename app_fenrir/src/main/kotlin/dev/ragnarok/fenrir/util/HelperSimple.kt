package dev.ragnarok.fenrir.util

import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.Includes

object HelperSimple {
    const val DIALOG_SEND_HELPER = "dialog_send_helper"
    const val PLAYLIST_HELPER = "playlist_helper"
    const val STORY_HELPER = "story_helper"
    const val LOLLIPOP_21 = "lollipop21"
    const val AUDIO_DEAD = "audio_dead"
    const val HIDDEN_DIALOGS = "hidden_dialogs"
    const val MONITOR_CHANGES = "monitor_changes"
    const val NOTIFICATION_PERMISSION = "notification_permission"
    fun needHelp(key: String, count: Int): Boolean {
        val app = Includes.provideApplicationContext()
        val ret = PreferenceScreen.getPreferences(app).getInt(key, 0)
        if (ret < count) {
            PreferenceScreen.getPreferences(app).edit().putInt(key, ret + 1).apply()
            return true
        }
        return false
    }

    fun hasHelp(key: String, count: Int): Boolean {
        val app = Includes.provideApplicationContext()
        return PreferenceScreen.getPreferences(app).getInt(key, 0) < count
    }
}
