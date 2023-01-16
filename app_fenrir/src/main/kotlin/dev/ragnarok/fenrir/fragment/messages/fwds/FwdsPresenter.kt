package dev.ragnarok.fenrir.fragment.messages.fwds

import android.os.Bundle
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Message

class FwdsPresenter(accountId: Long, messages: List<Message>, savedInstanceState: Bundle?) :
    AbsMessageListPresenter<IFwdsView>(accountId, savedInstanceState) {
    fun fireTranscript(voiceMessageId: String?, messageId: Int) {
        appendDisposable(
            messages.recogniseAudioMessage(accountId, messageId, voiceMessageId)
                .fromIOToMain()
                .subscribe({ }) { })
    }

    init {
        if (messages.isNotEmpty()) {
            data.addAll(messages)
        }
    }
}