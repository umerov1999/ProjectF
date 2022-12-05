package dev.ragnarok.fenrir.db

import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.interfaces.IStorages

object Stores {
    val instance: IStorages
        get() = Includes.stores
}