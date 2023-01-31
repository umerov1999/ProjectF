package dev.ragnarok.fenrir.settings

import android.content.Context
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.settings.ISettings.IDrawerSettings
import dev.ragnarok.fenrir.settings.ISettings.IMainSettings
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import dev.ragnarok.fenrir.settings.ISettings.IOtherSettings
import dev.ragnarok.fenrir.settings.ISettings.IPushSettings
import dev.ragnarok.fenrir.settings.ISettings.IRecentChats
import dev.ragnarok.fenrir.settings.ISettings.ISecuritySettings
import dev.ragnarok.fenrir.settings.ISettings.ISideDrawerSettings
import dev.ragnarok.fenrir.settings.ISettings.IUISettings

class SettingsImpl(app: Context) : ISettings {
    private val recentChats: IRecentChats
    private val drawerSettings: IDrawerSettings
    private val sideDrawerSettings: ISideDrawerSettings
    private val pushSettings: IPushSettings
    private val securitySettings: ISecuritySettings
    private val iuiSettings: IUISettings
    private val notificationSettings: INotificationSettings
    private val mainSettings: IMainSettings
    private val accountsSettings: IAccountsSettings
    private val otherSettings: IOtherSettings
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

    override fun other(): IOtherSettings {
        return otherSettings
    }

    init {
        notificationSettings = NotificationsPrefs(app)
        recentChats = RecentChatsSettings(app)
        drawerSettings = DrawerSettings(app)
        sideDrawerSettings = SideDrawerSettings(app)
        pushSettings = PushSettings(app)
        securitySettings = SecuritySettings(app)
        iuiSettings = UISettings(app)
        mainSettings = MainSettings(app)
        accountsSettings = AccountsSettings(app)
        otherSettings = OtherSettings(app)
    }
}