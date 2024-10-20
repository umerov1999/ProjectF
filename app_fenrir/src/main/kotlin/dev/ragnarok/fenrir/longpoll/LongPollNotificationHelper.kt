package dev.ragnarok.fenrir.longpoll

import android.content.Context
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Logger.d

object LongPollNotificationHelper {
    val TAG: String = LongPollNotificationHelper::class.simpleName.orEmpty()

    /**
     * Действие при добавлении нового сообщения в диалог или чат
     *
     * @param message нотификация с сервера
     */
    fun notifyAbountNewMessage(context: Context, message: Message) {
        if (message.isOut) {
            return
        }

        //if (message.isRead()) {
        //    return;
        //}

        //boolean needSendNotif = needNofinicationFor(message.getAccountId(), message.getPeerId());
        //if(!needSendNotif){
        //    return;
        //}
        notifyAbountNewMessage(context, message.accountId, message)
    }

    private fun notifyAbountNewMessage(context: Context, accountId: Long, message: Message) {
        if (Settings.get().notifications().isSilentPeer(accountId, message.peerId)) {
            return
        }
        if (Settings.get().accounts().current != accountId) {
            d(
                TAG,
                "notifyAbountNewMessage, Attempting to send a notification does not in the current account!!!"
            )
            return
        }
        NotificationHelper.notifyNewMessage(context, accountId, message)
    }
}