package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Chat
import io.reactivex.rxjava3.core.Single

interface IDialogsInteractor {
    fun getChatById(accountId: Int, peerId: Int): Single<Chat>
}