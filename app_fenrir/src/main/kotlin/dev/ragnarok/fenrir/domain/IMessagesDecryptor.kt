package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Message
import io.reactivex.rxjava3.core.SingleTransformer

interface IMessagesDecryptor {
    /**
     * Предоставляет RX-трансформер для дешифровки сообщений
     *
     * @param accountId идентификатор аккаунта
     * @return RX-трансформер
     */
    fun withMessagesDecryption(accountId: Int): SingleTransformer<List<Message>, List<Message>>
}