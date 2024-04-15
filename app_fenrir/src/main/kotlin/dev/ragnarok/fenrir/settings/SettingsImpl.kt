package dev.ragnarok.fenrir.settings

import android.content.Context
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.settings.ISettings.IDrawerSettings
import dev.ragnarok.fenrir.settings.ISettings.IMainSettings
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import dev.ragnarok.fenrir.settings.ISettings.IPushSettings
import dev.ragnarok.fenrir.settings.ISettings.IRecentChats
import dev.ragnarok.fenrir.settings.ISettings.ISecuritySettings
import dev.ragnarok.fenrir.settings.ISettings.ISideDrawerSettings
import dev.ragnarok.fenrir.settings.ISettings.IUISettings

class SettingsImpl(app: Context) : ISettings {
    private val recentChats: IRecentChats = RecentChatsSettings(app)
    private val drawerSettings: IDrawerSettings = DrawerSettings(app)
    private val sideDrawerSettings: ISideDrawerSettings = SideDrawerSettings(app)
    private val pushSettings: IPushSettings = PushSettings(app)
    private val securitySettings: ISecuritySettings = SecuritySettings(app)
    private val iuiSettings: IUISettings = UISettings(app)
    private val notificationSettings: INotificationSettings = NotificationsPrefs(app)
    private val mainSettings: IMainSettings = MainSettings(app)
    private val accountsSettings: IAccountsSettings = AccountsSettings(app)
    override fun recentChats(): IRecentChats {
        return recentChats
    }

    override fun drawerSettings(): IDrawerSettings {
        return drawerSettings
    }

    override fun sideDrawerSettings(): ISideDrawerSettings {
        return sideDrawerSettings
    }

    override fun pushSettings(): IPushSettings {
        return pushSettings
    }

    override fun security(): ISecuritySettings {
        return securitySettings
    }

    override fun ui(): IUISettings {
        return iuiSettings
    }

    override fun notifications(): INotificationSettings {
        return notificationSettings
    }

    override fun main(): IMainSettings {
        return mainSettings
    }

    override fun accounts(): IAccountsSettings {
        return accountsSettings
    }
}