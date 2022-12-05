package dev.ragnarok.fenrir.fragment.base

import android.os.Bundle
import androidx.annotation.CallSuper
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toMainThread

abstract class AccountDependencyPresenter<V>(
    accountId: Int,
    savedInstanceState: Bundle?,
    private var mSupportAccountHotSwap: Boolean = false
) :
    RxSupportPresenter<V>(savedInstanceState) where V : IMvpView {
    var accountId = 0
        private set

    private fun onAccountChange(newAccountId: Int) {
        val oldAccountId = accountId
        if (oldAccountId == newAccountId) {
            return
        }
        if (mSupportAccountHotSwap) {
            beforeAccountChange(oldAccountId, newAccountId)
            accountId = newAccountId
            afterAccountChange(oldAccountId, newAccountId)
        }
    }

    @CallSuper
    protected open fun afterAccountChange(oldAccountId: Int, newAccountId: Int) {
    }

    @CallSuper
    protected open fun beforeAccountChange(oldAccountId: Int, newAccountId: Int) {
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putInt(SAVE_ACCOUNT_ID, accountId)
    }

    fun toggleSupportAccountHotSwap() {
        if (mSupportAccountHotSwap) {
            return
        }
        mSupportAccountHotSwap = true
        observeChangesAccount()
    }

    private fun observeChangesAccount() {
        appendDisposable(
            Settings.get()
                .accounts()
                .observeChanges()
                .toMainThread()
                .subscribe { onAccountChange(it) })
    }

    init {
        this.accountId = savedInstanceState?.getInt(SAVE_ACCOUNT_ID) ?: accountId
        if (mSupportAccountHotSwap) {
            observeChangesAccount()
        }
    }

    companion object {
        private const val SAVE_ACCOUNT_ID = "save_account_id"
    }
}