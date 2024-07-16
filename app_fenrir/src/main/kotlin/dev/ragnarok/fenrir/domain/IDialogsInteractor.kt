package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Chat
import kotlinx.coroutines.flow.Flow

interface IDialogsInteractor {
    fun getChatById(accountId: Long, peerId: Long): Flow<Chat>
}