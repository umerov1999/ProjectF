package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.nonNullNoEmpty
import io.reactivex.rxjava3.core.Completable

class LongPollEventSaver {
    private val messagesInteractor: IMessagesRepository = messages
    private val ownersRepository: IOwnersRepository = owners
    fun save(accountId: Long, updates: VKApiLongpollUpdates): Completable {
        var completable = Completable.complete()
        if (updates.output_messages_set_read_updates.nonNullNoEmpty() || updates.input_messages_set_read_updates.nonNullNoEmpty()) {
            completable = completable.andThen(
                messagesInteractor.handleReadUpdates(
                    accountId,
                    updates.output_messages_set_read_updates,
                    updates.input_messages_set_read_updates
                )
            )
        }
        if (updates.message_flags_reset_updates.nonNullNoEmpty() || updates.message_flags_set_updates.nonNullNoEmpty()) {
            completable = completable.andThen(
                messagesInteractor.handleFlagsUpdates(
                    accountId,
                    updates.message_flags_set_updates,
                    updates.message_flags_reset_updates
                )
            )
        }
        if (updates.user_is_online_updates.nonNullNoEmpty() || updates.user_is_offline_updates.nonNullNoEmpty()) {
            completable = completable.andThen(
                ownersRepository.handleOnlineChanges(
                    accountId,
                    updates.user_is_offline_updates,
                    updates.user_is_online_updates
                )
            )
        }
        if (updates.badge_count_change_updates.nonNullNoEmpty()) {
            completable = completable.andThen(
                messagesInteractor.handleUnreadBadgeUpdates(
                    accountId,
                    updates.badge_count_change_updates
                )
            )
        }
        if (updates.write_text_in_dialog_updates.nonNullNoEmpty()) {
            completable = completable.andThen(
                messagesInteractor.handleWriteUpdates(
                    accountId,
                    updates.write_text_in_dialog_updates
                )
            )
        }
        return completable
    }

}