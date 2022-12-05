package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.interfaces.INetworker

object Apis {

    fun get(): INetworker {
        return networkInterfaces
    }
}