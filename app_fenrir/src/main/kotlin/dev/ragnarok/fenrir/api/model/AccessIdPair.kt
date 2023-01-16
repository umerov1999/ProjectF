package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class AccessIdPair(val id: Int, val ownerId: Long, val accessKey: String?) {
    companion object {

        fun format(pair: AccessIdPair): String {
            return pair.ownerId.toString() + "_" + pair.id + if (pair.accessKey == null) "" else "_" + pair.accessKey
        }
    }
}