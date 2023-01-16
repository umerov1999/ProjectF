package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.settings.Settings

class DefaultVkApiInterceptor internal constructor(
    override val accountId: Long,
    v: String
) : AbsVkApiInterceptor(
    v
) {
    override val token: String?
        get() = Settings.get()
            .accounts()
            .getAccessToken(accountId)

    @AccountType
    override val type: Int
        get() = Settings.get()
            .accounts()
            .getType(accountId)
}