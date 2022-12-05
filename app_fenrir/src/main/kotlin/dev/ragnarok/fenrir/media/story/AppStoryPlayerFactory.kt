package dev.ragnarok.fenrir.media.story

import dev.ragnarok.fenrir.settings.IProxySettings

class AppStoryPlayerFactory(private val proxySettings: IProxySettings) : IStoryPlayerFactory {
    override fun createStoryPlayer(url: String, isRepeat: Boolean): IStoryPlayer {
        val config = proxySettings.activeProxy
        return ExoStoryPlayer(url, config, isRepeat)
    }
}