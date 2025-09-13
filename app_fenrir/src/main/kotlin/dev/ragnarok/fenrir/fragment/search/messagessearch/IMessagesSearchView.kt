package dev.ragnarok.fenrir.fragment.search.messagessearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer

interface IMessagesSearchView : IBaseSearchView<Message> {
    fun goToMessagesLookup(accountId: Long, peerId: Long, messageId: Int)
    fun configNowVoiceMessagePlaying(
        id: Int,
        progress: Float,
        duration: Long,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    )

    fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        duration: Long,
        amin: Boolean,
        speed: Boolean
    )

    fun goToPeerLookup(accountId: Long, peer: Peer)
    fun disableVoicePlaying()
}