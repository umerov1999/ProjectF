package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IDialogsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Chat
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class DialogsInteractor(private val networker: INetworker, private val repositories: IStorages) :
    IDialogsInteractor {
    override fun getChatById(accountId: Long, peerId: Long): Flow<Chat> {
        return repositories.dialogs()
            .findChatById(accountId, peerId)
            .flatMapConcat { optional ->
                if (optional.nonEmpty()) {
                    toFlow(optional.requireNonEmpty())
                } else {
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
}