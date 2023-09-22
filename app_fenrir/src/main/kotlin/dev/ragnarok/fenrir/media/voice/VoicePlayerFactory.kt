package dev.ragnarok.fenrir.media.voice

import android.content.Context
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.ISettings

class VoicePlayerFactory(
    context: Context,
    private val proxySettings: IProxySettings,
    mainSettings: ISettings.IMainSettings
) : IVoicePlayerFactory {
    private val app: Context = context.applicationContext
    private val NotSensered: Boolean = mainSettings.isDisable_sensored_voice
    override fun createPlayer(): IVoicePlayer {
        val config = proxySettings.activeProxy
        return if (NotSensered) ExoVoicePlayer(app, config) else ExoVoicePlayerSensored(app, config)
    }

}