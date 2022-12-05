package dev.ragnarok.fenrir.media.voice

import android.content.Context
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.ISettings.IOtherSettings

class VoicePlayerFactory(
    context: Context,
    private val proxySettings: IProxySettings,
    otherSettings: IOtherSettings
) : IVoicePlayerFactory {
    private val app: Context = context.applicationContext
    private val NotSensered: Boolean = otherSettings.isDisable_sensored_voice
    override fun createPlayer(): IVoicePlayer {
        val config = proxySettings.activeProxy
        return if (NotSensered) ExoVoicePlayer(app, config) else ExoVoicePlayerSensored(app, config)
    }

}