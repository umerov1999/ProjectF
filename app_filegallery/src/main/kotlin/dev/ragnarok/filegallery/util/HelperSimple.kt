package dev.ragnarok.filegallery.util

import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.filegallery.Includes

object HelperSimple {
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
