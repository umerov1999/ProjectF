package dev.ragnarok.fenrir.settings

import android.content.Context
import androidx.core.content.edit
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.model.DrawerCategory
import dev.ragnarok.fenrir.model.SwitchableCategory
import dev.ragnarok.fenrir.settings.ISettings.ISideDrawerSettings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.builtins.ListSerializer

internal class SideDrawerSettings(context: Context) : ISideDrawerSettings {
    private val app: Context = context.applicationContext
    private val publishSubject = createPublishSubject<List<DrawerCategory>>()

    private fun makeDefaults(): List<DrawerCategory> {
        return listOf(
            DrawerCategory(SwitchableCategory.FRIENDS),
            DrawerCategory(SwitchableCategory.DIALOGS),
            DrawerCategory(SwitchableCategory.FEED),
            DrawerCategory(SwitchableCategory.FEEDBACK),
            DrawerCategory(SwitchableCategory.STORIES),
            DrawerCategory(SwitchableCategory.GROUPS),
            DrawerCategory(SwitchableCategory.PHOTOS),
            DrawerCategory(SwitchableCategory.VIDEOS),
            DrawerCategory(SwitchableCategory.MUSIC),
            DrawerCategory(SwitchableCategory.DOCS),
            DrawerCategory(SwitchableCategory.FAVES),
            DrawerCategory(SwitchableCategory.SEARCH)
        )
    }

    override var categoriesOrder: List<DrawerCategory>
        get() {
            val tmp = getPreferences(app).getString("side_navigation_menu_order", null)
                ?: return makeDefaults()
            return kJson.decodeFromString(ListSerializer(DrawerCategory.serializer()), tmp)
        }
        set(list) {
            getPreferences(app).edit {
                putString(
                    "side_navigation_menu_order",
                    kJson.encodeToString(ListSerializer(DrawerCategory.serializer()), list)
                )
            }
            publishSubject.myEmit(list)
        }

    override val observeChanges: SharedFlow<List<DrawerCategory>>
        get() = publishSubject

    override fun reset() {
        getPreferences(app).edit {
            remove(
                "side_navigation_menu_order"
            )
        }
        publishSubject.myEmit(makeDefaults())
    }
}
