package dev.ragnarok.fenrir.service

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.SaveMessageBuilder
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import java.io.File

@Suppress("DEPRECATION")
class QuickReplyService : android.app.IntentService(QuickReplyService::class.java.name) {
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && ACTION_ADD_MESSAGE == intent.action && intent.extras != null) {
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            val peerId = (intent.extras ?: return).getLong(Extra.PEER_ID)
            val msg = RemoteInput.getResultsFromIntent(intent)
            if (msg != null) {
                val body = msg.getCharSequence(Extra.BODY)
                addMessage(accountId, peerId, body?.toString())
            }
        } else if (intent != null && ACTION_MARK_AS_READ == intent.action && intent.extras != null) {
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            val peerId = (intent.extras ?: return).getLong(Extra.PEER_ID)
            val msgId = (intent.extras ?: return).getInt(Extra.MESSAGE_ID)
            messages.markAsRead(accountId, peerId, msgId).blockingSubscribe(dummy(), ignore())
        } else if (intent != null && ACTION_DELETE_FILE == intent.action && intent.extras != null) {
            if (AppPerms.hasNotificationPermissionSimple(this)) {
                NotificationManagerCompat.from(this).cancel(
                    (intent.extras ?: return).getString(Extra.TYPE), (intent.extras ?: return)
                        .getInt(Extra.ID)
                )
            }
            File((intent.extras ?: return).getString(Extra.DOC) ?: return).delete()
        }
    }

    private fun addMessage(accountId: Long, peerId: Long, body: String?) {
        val messagesInteractor = messages
        val builder = SaveMessageBuilder(accountId, peerId).setBody(body)
        messagesInteractor.put(builder).blockingSubscribe()
        messages.runSendingQueue()
    }

    companion object {
        const val ACTION_ADD_MESSAGE = "SendService.ACTION_ADD_MESSAGE"
        const val ACTION_MARK_AS_READ = "SendService.ACTION_MARK_AS_READ"
        const val ACTION_DELETE_FILE = "SendService.ACTION_DELETE_FILE"


        fun intentForAddMessage(
            context: Context,
            accountId: Long,
            peerId: Long,
            msg: Message
        ): Intent {
            val intent = Intent(context, QuickReplyService::class.java)
            intent.action = ACTION_ADD_MESSAGE
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.PEER_ID, peerId)
            intent.putExtra(Extra.MESSAGE, msg)
            return intent
        }

        fun intentForDeleteFile(
            context: Context,
            path: String,
            notificationId: Int,
            notificationTag: String
        ): Intent {
            val intent = Intent(context, QuickReplyService::class.java)
            intent.action = ACTION_DELETE_FILE
            intent.putExtra(Extra.DOC, path)
            intent.putExtra(Extra.ID, notificationId)
            intent.putExtra(Extra.TYPE, notificationTag)
            return intent
        }


        fun intentForReadMessage(
            context: Context,
            accountId: Long,
            peerId: Long,
            msgId: Int
        ): Intent {
            val intent = Intent(context, QuickReplyService::class.java)
            intent.action = ACTION_MARK_AS_READ
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.PEER_ID, peerId)
            intent.putExtra(Extra.MESSAGE_ID, msgId)
            return intent
        }
    }
}