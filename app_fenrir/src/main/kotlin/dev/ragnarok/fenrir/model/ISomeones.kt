package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.model.Identificable

interface ISomeones : Identificable {
    val ownerId: Int
}