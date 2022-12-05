package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiFriendList {
    /**
     * идентификатор списка друзей
     */
    var id = 0

    /**
     * название списка друзей
     */
    var name: String? = null
}