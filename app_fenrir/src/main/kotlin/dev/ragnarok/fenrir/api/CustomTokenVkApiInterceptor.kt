package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.settings.Settings

internal class CustomTokenVkApiInterceptor(
    override val token: String?,
    v: String,
    @AccountType private val accountType: Int,
    private val account_id: Int?
) : AbsVkApiInterceptor(
    v
) {

    @AccountType
    override val type: Int
        get() {
            if (accountType == AccountType.BY_TYPE && account_id == null) {
                return Constants.DEFAULT_ACCOUNT_TYPE
            } else if (accountType == AccountType.BY_TYPE) {
                return Settings.get().accounts().getType(account_id ?: -1)
            }
            return accountType
        }
    override val accountId: Int
        get() = account_id ?: -1
}