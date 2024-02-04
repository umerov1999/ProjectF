package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * This class represents owner of some VK object.
 */
@Serializable
open class VKApiOwner
/**
 * Creates an owner with empty ID.
 */
{
    /**
     * User or group ID.
     */
    var id = 0L
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is VKApiOwner && id == other.id
    }

    open val fullName: String?
        get() = null
    open val maxSquareAvatar: String?
        get() = null
}