package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IDialogsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Chat
import dev.ragnarok.fenrir.model.Peer
import io.reactivex.rxjava3.core.Single

class DialogsInteractor(private val networker: INetworker, private val repositories: IStorages) :
    IDialogsInteractor {
    override fun getChatById(accountId: Int, peerId: Int): Single<Chat> {
        return repositories.dialogs()
            .findChatById(accountId, peerId)
            .flatMap { optional ->
                if (optional.nonEmpty()) {
                    return@flatMap Single.just(optional.requireNonEmpty())
                }
                val chatId = Peer.toChatId(peerId)
                networker.vkDefault(accountId)
                    .messages()
                    .getChat(chatId, null, null, null)
                    .map { chats ->
                        if (chats.isEmpty()) {
                            throw NotFoundException()
                        }
                        chats[0]
                    }
                    .map { transform(it) }
            }
    }
}