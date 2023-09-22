package dev.ragnarok.fenrir.api.model.longpoll

import dev.ragnarok.fenrir.api.model.VKApiReaction

class ReactionMessageChangeUpdate(
    @ReactionEventType val event_type_of_reaction: Int,
    val peer_id: Long,
    val conversation_message_id: Int,
    val myReaction: Int,
    val myReactionChanged: Boolean,
    val arrayReactionList: ArrayList<VKApiReaction>
) : AbsLongpollEvent(ACTION_MESSAGE_REACTION_CHANGE)