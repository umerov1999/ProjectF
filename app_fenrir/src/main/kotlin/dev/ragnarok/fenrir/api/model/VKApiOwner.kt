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
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VKApiOwner

        if (id != other.id) return false

        return true
    }

    open val fullName: String?
        get() = null
    open val maxSquareAvatar: String?
        get() = null
}