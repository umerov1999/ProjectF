package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.settings.Settings

class DefaultVKApiInterceptor internal constructor(
    override val accountId: Long,
    v: String
) : AbsVKApiInterceptor(
    v
) {
    override val token: String?
        get() = Settings.get()
            .accounts()
            .getAccessToken(accountId)

    override val customDeviceName: String?
        get() = Settings.get()
            .accounts()
            .getDevice(accountId)

    @AccountType
    override val type: Int
        get() = Settings.get()
            .accounts()
            .getType(accountId)
}