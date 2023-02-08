package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.settings.Settings

internal class CustomTokenVKApiInterceptor(
    override val token: String?,
    v: String,
    @AccountType private val accountType: Int,
    override val customDeviceName: String?,
    private val account_id: Long?
) : AbsVKApiInterceptor(
    v
) {

    @AccountType
    override val type: Int
        get() {
            if (accountType == AccountType.NULL && account_id == null) {
                return Constants.DEFAULT_ACCOUNT_TYPE
            } else if (accountType == AccountType.NULL) {
                return Settings.get().accounts().getType(account_id ?: -1)
            }
            return accountType
        }
    override val accountId: Long = account_id ?: -1
}
