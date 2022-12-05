package dev.ragnarok.fenrir.crypt

import androidx.annotation.IntDef

@IntDef(
    SessionState.INITIATOR_EMPTY,
    SessionState.NO_INITIATOR_EMPTY,
    SessionState.INITIATOR_STATE_1,
    SessionState.NO_INITIATOR_STATE_1,
    SessionState.INITIATOR_STATE_2,
    SessionState.INITIATOR_FINISHED,
    SessionState.NO_INITIATOR_FINISHED,
    SessionState.FAILED,
    SessionState.CLOSED
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class SessionState {
    companion object {
        /**
         * Сессия не начата
         */
        const val INITIATOR_EMPTY = 1

        /**
         * Сессия не начата
         */
        const val NO_INITIATOR_EMPTY = 2

        /**
         * Начальный этап сессии обмена ключами
         * Я, как инициатор, отправил свой публичный ключ
         * и жду, пока мне в ответ отправят AES-ключ, зашифрованный этим публичным ключом
         */
        const val INITIATOR_STATE_1 = 3

        /**
         * Получен запрос от инициатора на обмен ключами
         * В сообщении должен быть публичный ключ инициатора,
         * Я отправил ему AES-ключ, зашифрованный ЕГО публичным ключом.
         * В то же время я вложил в сообщение свой публичный ключ,
         * чтобы инициатор отправил в ответ свой AES-ключ
         */
        const val NO_INITIATOR_STATE_1 = 4

        /**
         * Я - инициатор обмена
         * Получен AES-ключ от собеседника и его публичный ключ
         * Я в ответ отправил ему свой AES-ключ, зашифрованный его публичным ключом
         */
        const val INITIATOR_STATE_2 = 5

        /**
         * Получен запрос от собеседника на успешное закрытие сессии обмена ключами
         * Собеседнику отправляем пустое сообщение как подтверждение успешного обмена
         */
        const val NO_INITIATOR_FINISHED = 6

        /**
         * Отправляем подтверждение получение ключа и запрос на завершение сессии
         * Собеседнику отправляем пустое сообщение как подтверждение успешного обмена
         */
        const val INITIATOR_FINISHED = 7
        const val CLOSED = 8
        const val FAILED = 9
    }
}