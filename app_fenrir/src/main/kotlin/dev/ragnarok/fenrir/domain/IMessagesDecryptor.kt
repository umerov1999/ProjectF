package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Message
import kotlinx.coroutines.flow.Flow

interface IMessagesDecryptor {
    /**
     * Предоставляет RX-трансформер для дешифровки сообщений
     *
     * @param accountId идентификатор аккаунта
     * @return RX-трансформер
     */
    fun withMessagesDecryption(accountId: Long): suspend (List<Message>) -> Flow<List<Message>>
}