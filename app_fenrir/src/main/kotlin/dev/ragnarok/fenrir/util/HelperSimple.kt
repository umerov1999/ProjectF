package dev.ragnarok.fenrir.util

import androidx.core.content.edit
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.Includes

object HelperSimple {
    const val DIALOG_SEND_HELPER = "dialog_send_helper"
    const val PLAYLIST_HELPER = "playlist_helper"
    const val STORY_HELPER = "story_helper"
    const val SHORT_VIDEO_HELPER = "short_video_helper"
    const val AUDIO_DEAD = "audio_dead"
    const val HIDDEN_DIALOGS = "hidden_dialogs"
    const val MONITOR_CHANGES = "monitor_changes"
    const val NOTIFICATION_PERMISSION = "notification_permission"
    const val BATTERY_OPTIMIZATION = "battery_optimization"
    fun needHelp(key: String, count: Int): Boolean {
        val app = Includes.provideApplicationContext()
        val ret = PreferenceScreen.getPreferences(app).getInt(key, 0)
        if (ret < count) {
            PreferenceScreen.getPreferences(app).edit { putInt(key, ret + 1) }
            return true
        }
        return false
    }

    fun hasHelp(key: String, count: Int): Boolean {
        val app = Includes.provideApplicationContext()
        return PreferenceScreen.getPreferences(app).getInt(key, 0) < count
    }
}
