package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ChatDtoAdapter
import kotlinx.serialization.Serializable

/**
 * Chat object describes a user's chat.
 */
@Serializable(with = ChatDtoAdapter::class)
class VKApiChat
/**
 * Creates empty Chat instance.
 */
{
    /**
     * Chat ID, positive number.
     */
    var id = 0

    /**
     * Type of chat.
     */
    var type: String? = null

    /**
     * Chat title.
     */
    var title: String? = null

    /**
     * ID of the chat starter, positive number
     */
    var admin_id = 0

    /**
     * List of chat participants' IDs.
     */
    var users: ArrayList<ChatUserDto>? = null
    var photo_50: String? = null
    var photo_100: String? = null
    var photo_200: String? = null
}