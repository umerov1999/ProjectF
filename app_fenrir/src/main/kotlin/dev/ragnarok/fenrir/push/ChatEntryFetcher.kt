package dev.ragnarok.fenrir.push

import android.content.Context
import android.graphics.Bitmap
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.push.NotificationUtils.loadRoundedImageRx
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

object ChatEntryFetcher {
    fun getRx(context: Context, accountId: Long, peerId: Long): Flow<DialogInfo> {
        val app = context.applicationContext
        when (Peer.getType(peerId)) {
            Peer.USER, Peer.GROUP -> {
                val ownerId = Peer.toOwnerId(peerId)
                return OwnerInfo.getRx(app, accountId, ownerId)
                    .map { info ->
                        val owner = info.owner
                        val response = DialogInfo()
                        response.title = owner.fullName
                        response.img = owner.maxSquareAvatar
                        response.icon = info.avatar
                        response
                    }
            }

            Peer.CHAT, Peer.CONTACT -> return messages
                .getConversation(accountId, peerId, Mode.ANY)
                .flatMapConcat { chat ->
                    loadRoundedImageRx(app, chat.imageUrl, R.drawable.ic_group_chat)
                        .map { Optional.wrap(it) }
                        .catch {
                            emit(empty())
                        }
                        .map { optional ->
                            val response = DialogInfo()
                            response.title = chat.getDisplayTitle()
                            response.img = chat.imageUrl
                            response.icon = optional.get()
                            response
                        }
                }
        }
        throw UnsupportedOperationException()
    }

    class DialogInfo {
        var title: String? = null
        var img: String? = null
        var icon: Bitmap? = null
    }
}