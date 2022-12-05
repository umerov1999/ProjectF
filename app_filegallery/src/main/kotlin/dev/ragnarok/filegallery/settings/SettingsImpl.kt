package dev.ragnarok.filegallery.settings

import android.content.Context
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings

class SettingsImpl(app: Context) : ISettings {
    private val mainSettings: IMainSettings
    private val securitySettings: ISettings.ISecuritySettings
    override fun main(): IMainSettings {
        return mainSettings
    }

    override fun security(): ISettings.ISecuritySettings {
        return securitySettings
    }

    init {
        mainSettings = MainSettings(app)
        securitySettings = SecuritySettings(app)
    }
}
