package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class IdPair(val id: Int, val ownerId: Long)