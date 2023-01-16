package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiFriendList {
    /**
     * идентификатор списка друзей
     */
    var id = 0L

    /**
     * название списка друзей
     */
    var name: String? = null
}