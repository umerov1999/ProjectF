package dev.ragnarok.fenrir.settings

import android.content.Context
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.settings.ISettings.IPushSettings

internal class PushSettings(context: Context) : IPushSettings {
    private val app: Context = context.applicationContext
    override fun savePushRegistations(data: Collection<VkPushRegistration>) {
        val target: MutableSet<String> = HashSet(data.size)
        for (registration in data) {
            target.add(kJson.encodeToString(VkPushRegistration.serializer(), registration))
        }
        getPreferences(app)
            .edit()
            .putStringSet(KEY_REGISTERED_FOR, target)
            .apply()
    }

    override val registrations: List<VkPushRegistration>
        get() {
            val set = getPreferences(app)
                .getStringSet(KEY_REGISTERED_FOR, null)
            val result: MutableList<VkPushRegistration> = ArrayList(
                set?.size ?: 0
            )
            if (set != null) {
                for (s in set) {
                    val registration: VkPushRegistration =
                        kJson.decodeFromString(VkPushRegistration.serializer(), s)
                    result.add(registration)
                }
            }
            return result
        }

    companion object {
        private const val KEY_REGISTERED_FOR = "push_registered_for"
    }

}