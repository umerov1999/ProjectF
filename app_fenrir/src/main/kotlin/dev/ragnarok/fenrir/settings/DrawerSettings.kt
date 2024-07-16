package dev.ragnarok.fenrir.settings

import android.content.Context
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.SwitchableCategory
import dev.ragnarok.fenrir.settings.ISettings.IDrawerSettings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.builtins.ListSerializer

internal class DrawerSettings(context: Context) : IDrawerSettings {
    private val app: Context = context.applicationContext
    private val publishSubject = createPublishSubject<List<DrawerCategory>>()

    private fun makeDefaults(): List<DrawerCategory> {
        return listOf(
            DrawerCategory(SwitchableCategory.FRIENDS),
            DrawerCategory(SwitchableCategory.NEWSFEED_COMMENTS),
            DrawerCategory(SwitchableCategory.GROUPS),
            DrawerCategory(SwitchableCategory.PHOTOS),
            DrawerCategory(SwitchableCategory.VIDEOS),
            DrawerCategory(SwitchableCategory.MUSIC),
            DrawerCategory(SwitchableCategory.DOCS),
            DrawerCategory(SwitchableCategory.FAVES)
        )
    }

    override var categoriesOrder: List<DrawerCategory>
        get() {
            val tmp = PreferenceScreen.getPreferences(app).getString("navigation_menu_order", null)
                ?: return makeDefaults()
            return kJson.decodeFromString(ListSerializer(DrawerCategory.serializer()), tmp)
        }
        set(list) {
            PreferenceScreen.getPreferences(app).edit().putString(
                "navigation_menu_order",
                kJson.encodeToString(ListSerializer(DrawerCategory.serializer()), list)
            )
                .apply()
            publishSubject.myEmit(list)
        }

    override val observeChanges: SharedFlow<List<DrawerCategory>>
        get() = publishSubject

    override fun reset() {
        PreferenceScreen.getPreferences(app).edit().remove(
            "navigation_menu_order"
        ).apply()
        publishSubject.myEmit(makeDefaults())
    }
}
