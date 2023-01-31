package dev.ragnarok.fenrir.util.refresh

import android.util.Log
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

object RefreshToken {
    /*
    private fun upgradeTokenKate(account: Int, oldToken: String): Boolean {
        if (Utils.isHiddenAccount(account)) {
            return false
        }
        val ret = Single.create<Boolean> { s ->
            FirebaseInstallations.getInstance().getToken(true).addOnSuccessListener {
                val token =
                    networkInterfaces.vkDefault(account).account()
                        .refreshToken(it.token, null, null, null)
                        .blockingGet().token
                if (oldToken == token || token.isNullOrEmpty()) {
                    s.onSuccess(false)
                } else {
                    Settings.get().accounts().storeAccessToken(account, token)
                    s.onSuccess(true)
                }
            }.addOnFailureListener { s.onSuccess(false) }
        }.blockingGet()

        return ret
    }
     */

    private fun upgradeTokenKate(account: Long, oldToken: String, force: Boolean): Boolean {
        if (Utils.isHiddenAccount(account) && !force) {
            return false
        }
        val gms = TokenModKate.requestToken() ?: return false
        val token =
            InteractorFactory.createAccountInteractor().refreshToken(account, gms, null, null, null)
                .blockingGet().token
        Log.w("refresh", "$oldToken $token $gms")
        if (oldToken == token || token.isNullOrEmpty()) {
            return false
        }
        Settings.get().accounts().storeAccessToken(account, token)
        return true
    }

    private fun upgradeTokenOfficial(account: Long, oldToken: String, force: Boolean): Boolean {
        if (Utils.isHiddenAccount(account) && !force) {
            return false
        }
        val gms = TokenModOfficialVK.requestToken() ?: return false
        val timestamp = System.currentTimeMillis()
        val token = InteractorFactory.createAccountInteractor().refreshToken(
            account,
            gms[0],
            gms[1],
            TokenModOfficialVK.getNonce(timestamp),
            timestamp
        )
            .blockingGet().token
        Log.w("refresh", "$oldToken $token $gms")
        if (oldToken == token || token.isNullOrEmpty()) {
            return false
        }
        Settings.get().accounts().storeAccessToken(account, token)
        return true
    }

    fun upgradeToken(account: Long, oldToken: String, force: Boolean = false): Boolean {
        if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.KATE) {
            return upgradeTokenKate(account, oldToken, force)
        } else if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID) {
            return upgradeTokenOfficial(account, oldToken, force)
        }
        return false
    }

    fun upgradeTokenRxPref(account: Long, oldToken: String): Single<Boolean> {
        return Single.create {
            try {
                it.onSuccess(upgradeToken(account, oldToken, true))
            } catch (ignored: Exception) {
                it.onSuccess(false)
            }
        }
    }
}