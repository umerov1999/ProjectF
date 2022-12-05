package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class AttachmentToken : IAttachmentToken {
    val type: String
    val id: Int
    val ownerId: Int
    val accessKey: String?

    constructor(type: String, id: Int, ownerId: Int) {
        this.type = type
        this.id = id
        this.ownerId = ownerId
        accessKey = null
    }

    constructor(type: String, id: Int, ownerId: Int, accessKey: String?) {
        this.type = type
        this.id = id
        this.ownerId = ownerId
        this.accessKey = accessKey
    }

    override fun format(): String {
        return type + ownerId + "_" + id + if (accessKey == null || accessKey.isEmpty()) "" else "_$accessKey"
    }
}