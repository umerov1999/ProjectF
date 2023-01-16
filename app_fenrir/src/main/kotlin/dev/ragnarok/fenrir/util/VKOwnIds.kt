package dev.ragnarok.fenrir.util

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.feedback.Copies
import dev.ragnarok.fenrir.api.model.feedback.UserArray
import dev.ragnarok.fenrir.api.model.feedback.VKApiUsersFeedback
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import kotlin.math.abs

class VKOwnIds {
    private val uids: MutableSet<Long>
    private val gids: MutableSet<Long>
    fun append(userArray: UserArray): VKOwnIds {
        userArray.ids?.let {
            for (id in it) {
                append(id)
            }
        }
        return this
    }

    fun appendStory(story: VKApiStory): VKOwnIds {
        append(story.owner_id)
        return this
    }

    fun append(dto: VKApiUsersFeedback): VKOwnIds {
        dto.users?.let { append(it) }
        return this
    }

    fun append(topic: VKApiTopic): VKOwnIds {
        append(topic.created_by)
        append(topic.updated_by)
        return this
    }

    fun append(copies: Copies): VKOwnIds {
        copies.pairs?.let {
            for (pair in it) {
                append(pair.owner_id)
            }
        }
        return this
    }

    fun append(commentsDto: CommentsDto?): VKOwnIds {
        commentsDto?.list.nonNullNoEmpty {
            for (comment in it) {
                append(comment)
            }
        }
        return this
    }

    fun append(comment: VKApiComment): VKOwnIds {
        if (comment.from_id != 0L) {
            append(comment.from_id)
        }
        comment.attachments.requireNonNull {
            append(it)
        }
        comment.threads.nonNullNoEmpty {
            for (i in it) {
                append(i)
            }
        }
        return this
    }

    private fun appendAttachmentDto(attachment: VKApiAttachment?): VKOwnIds {
        if (attachment is VKApiPost) {
            append(attachment)
        } else if (attachment is VKApiStory) {
            appendStory(attachment)
        }
        return this
    }

    fun append(attachments: VKApiAttachments): VKOwnIds {
        val entries = attachments.entryList()
        for (entry in entries) {
            appendAttachmentDto(entry.attachment)
        }
        return this
    }

    val all: Collection<Long>
        get() {
            val result: MutableCollection<Long> = HashSet(uids.size + gids.size)
            result.addAll(uids)
            for (gid in gids) {
                result.add(-abs(gid))
            }
            return result
        }

    fun getUids(): Set<Long> {
        return uids
    }

    fun getGids(): Set<Long> {
        return gids
    }

    fun appendAll(ids: Collection<Long>): VKOwnIds {
        for (id in ids) {
            append(id)
        }
        return this
    }

    fun append(messages: Collection<VKApiMessage>?): VKOwnIds {
        if (messages != null) {
            for (message in messages) {
                append(message)
            }
        }
        return this
    }

    fun append(dialog: VKApiDialog): VKOwnIds {
        dialog.lastMessage.requireNonNull {
            append(it)
        }
        return this
    }

    fun append(conversation: VKApiConversation): VKOwnIds {
        conversation.peer.requireNonNull {
            if (!Peer.isGroupChat(it.id) && "contact" != conversation.peer?.type) {
                append(it.id)
            }
        }
        return this
    }

    fun append(message: VKApiMessage): VKOwnIds {
        append(message.from_id)
        append(message.action_mid)
        if (!Peer.isContactChat(message.peer_id) && !Peer.isGroupChat(message.peer_id)) {
            append(message.peer_id)
        }
        message.fwd_messages.nonNullNoEmpty {
            for (fwd in it) {
                append(fwd)
            }
        }
        message.attachments.requireNonNull {
            val entries = it.entryList()
            for (entry in entries) {
                if (entry.attachment is VKApiPost) {
                    append(entry.attachment)
                } else if (entry.attachment is VKApiStory) {
                    appendStory(entry.attachment)
                }
            }
        }
        return this
    }

    fun append(messages: ArrayList<Message>): VKOwnIds {
        for (message in messages) {
            append(message)
        }
        return this
    }

    fun append(message: Message): VKOwnIds {
        append(message.senderId)
        append(message.actionMid) // тут 100% пользователь, нюанс в том, что он может быть < 0, если email
        if (!Peer.isGroupChat(message.peerId) && !Peer.isContactChat(message.peerId)) {
            append(message.peerId)
        }
        message.fwd.requireNonNull {
            val forwardMessages: List<Message> = it
            for (fwd in forwardMessages) {
                append(fwd)
            }
        }
        return this
    }

    fun appendNews(news: VKApiNews): VKOwnIds {
        append(news.source_id)
        append(news.copy_owner_id)
        if (news.hasCopyHistory()) {
            for (post in news.copy_history.orEmpty()) {
                append(post)
            }
        }
        if (news.hasAttachments()) {
            news.attachments?.let { append(it) }
        }
        news.friends.nonNullNoEmpty {
            appendAll(it)
        }
        return this
    }

    fun append(post: VKApiPost): VKOwnIds {
        //append(post.owner_id);
        append(post.from_id)
        append(post.signer_id)
        append(post.created_by)
        post.copy_history.requireNonNull {
            for (copy in it) {
                append(copy)
            }
        }
        return this
    }

    fun append(ownerId: Long?) {
        ownerId ?: return
        if (ownerId == 0L) return
        if (ownerId > 0) {
            appendUid(ownerId)
        } else {
            appendGid(ownerId)
        }
    }

    fun appendAll(ownerIds: LongArray?) {
        if (ownerIds != null) {
            for (id in ownerIds) {
                append(id)
            }
        }
    }

    private fun appendUid(uid: Long) {
        uids.add(uid)
    }

    private fun appendGid(gid: Long) {
        gids.add(abs(gid))
    }

    private fun constainsUids(): Boolean {
        return uids.isNotEmpty()
    }

    private fun constainsGids(): Boolean {
        return gids.isNotEmpty()
    }

    val isEmpty: Boolean
        get() = !constainsUids() && !constainsGids()

    fun nonEmpty(): Boolean {
        return constainsGids() || constainsUids()
    }

    override fun toString(): String {
        return "uids: $uids, gids: $gids"
    }

    companion object {
        fun fromPosts(posts: Collection<VKApiPost>): VKOwnIds {
            val ids = VKOwnIds()
            for (post in posts) {
                ids.append(post)
            }
            return ids
        }
    }

    init {
        uids = HashSet()
        gids = HashSet()
    }
}
