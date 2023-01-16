package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.model.interfaces.Identificable

interface ISomeones : Identificable {
    val ownerId: Long
}