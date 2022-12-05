package dev.ragnarok.fenrir.fragment.search.messagessearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer

interface IMessagesSearchView : IBaseSearchView<Message> {
    fun goToMessagesLookup(accountId: Int, peerId: Int, messageId: Int)
    fun configNowVoiceMessagePlaying(
        id: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    )

    fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    )

    fun goToPeerLookup(accountId: Int, peer: Peer)
    fun disableVoicePlaying()
}