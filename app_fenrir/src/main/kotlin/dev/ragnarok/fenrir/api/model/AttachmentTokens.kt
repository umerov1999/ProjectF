package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import kotlinx.serialization.Serializable

object AttachmentTokens {
    @Serializable
    class AttachmentToken : IAttachmentToken {
        val type: String
        val id: Int
        val ownerId: Long
        val accessKey: String?

        constructor(type: String, id: Int, ownerId: Long) {
            this.type = type
            this.id = id
            this.ownerId = ownerId
            accessKey = null
        }

        constructor(type: String, id: Int, ownerId: Long, accessKey: String?) {
            this.type = type
            this.id = id
            this.ownerId = ownerId
            this.accessKey = accessKey
        }

        override fun format(): String {
            return "$type${ownerId}_$id" + if (accessKey.isNullOrEmpty()) "" else "_$accessKey"
        }
    }

    @Serializable
    class AttachmentTokenForArtist(val type: String, val id: String?) : IAttachmentToken {
        override fun format(): String {
            return type + if (id.isNullOrEmpty()) "" else id
        }
    }

    @Serializable
    class AttachmentTokenStringSpecial(val type: String, val id: String?, val guid: String?) :
        IAttachmentToken {
        override fun format(): String {
            return type + if (id.isNullOrEmpty()) "" else "_$id" + if (guid.isNullOrEmpty()) "" else "_$guid"
        }
    }

    @Serializable
    class LinkAttachmentToken(val url: String?) : IAttachmentToken {
        override fun format(): String {
            return url ?: "null"
        }
    }
}