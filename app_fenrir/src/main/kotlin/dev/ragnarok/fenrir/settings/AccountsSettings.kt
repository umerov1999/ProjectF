package dev.ragnarok.fenrir.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.pushRegistrationResolver
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import java.util.*

internal class AccountsSettings @SuppressLint("UseSparseArrays") constructor(context: Context) :
    IAccountsSettings {
    private val app: Context = context.applicationContext
    private val changesPublisher = PublishProcessor.create<IAccountsSettings>()
    private val preferences: SharedPreferences = getPreferences(context)
    private val tokens: MutableMap<Int, String> = Collections.synchronizedMap(HashMap(1))
    private val types: MutableMap<Int, Int> = Collections.synchronizedMap(HashMap(1))
    private val devices: MutableMap<Int, String> = Collections.synchronizedMap(HashMap(1))
    private val accounts: MutableSet<String> = Collections.synchronizedSet(
        preferences.getStringSet(
            KEY_ACCOUNT_UIDS,
            HashSet(1)
        )!!
    )
    private val currentPublisher = PublishProcessor.create<Int>()
    private fun notifyAboutRegisteredChanges() {
        changesPublisher.onNext(this)
    }

    override fun observeRegistered(): Flowable<IAccountsSettings> {
        return changesPublisher.onBackpressureBuffer()
    }

    override fun observeChanges(): Flowable<Int> {
        return currentPublisher.onBackpressureBuffer()
    }

    override val registered: List<Int>
        get() {
            val ids: MutableList<Int> = ArrayList(accounts.size)
            for (stringuid in accounts) {
                val uid = stringuid.toInt()
                ids.add(uid)
            }
            return ids
        }

    @SuppressLint("CheckResult")
    private fun fireAccountChange() {
        val registrationResolver = pushRegistrationResolver
        registrationResolver.resolvePushRegistration()
            .fromIOToMain()
            .subscribe(dummy(), ignore())
        currentPublisher.onNext(current)
    }

    override var current: Int
        get() = preferences.getInt(KEY_CURRENT, IAccountsSettings.INVALID_ID)
        set(accountId) {
            if (current == accountId) return
            getPreferences(app)
                .edit()
                .putInt(KEY_CURRENT, accountId)
                .apply()
            fireAccountChange()
        }

    override fun remove(accountId: Int) {
        val currentAccountId = current
        val preferences = getPreferences(app)
        accounts.remove(accountId.toString())
        preferences.edit()
            .putStringSet(KEY_ACCOUNT_UIDS, accounts)
            .apply()
        if (accountId == currentAccountId) {
            val accountIds = registered
            var fisrtUserAccountId: Int? = null

            // делаем активным первый аккаунт ПОЛЬЗОВАТЕЛЯ
            for (existsId in accountIds) {
                if (existsId > 0) {
                    fisrtUserAccountId = existsId
                    break
                }
            }
            if (fisrtUserAccountId != null) {
                preferences.edit()
                    .putInt(KEY_CURRENT, fisrtUserAccountId)
                    .apply()
            } else {
                preferences.edit()
                    .remove(KEY_CURRENT)
                    .apply()
            }
        }
        notifyAboutRegisteredChanges()
        fireAccountChange()
    }

    override fun registerAccountId(accountId: Int, setCurrent: Boolean) {
        val preferences = getPreferences(app)
        accounts.add(accountId.toString())
        val editor = preferences.edit()
        editor.putStringSet(KEY_ACCOUNT_UIDS, accounts)
        if (setCurrent) {
            editor.putInt(KEY_CURRENT, accountId)
        }
        editor.apply()
        notifyAboutRegisteredChanges()
        if (setCurrent) {
            fireAccountChange()
        }
    }

    override fun storeAccessToken(accountId: Int, accessToken: String?) {
        accessToken ?: return
        tokens[accountId] = accessToken
        preferences.edit()
            .putString(tokenKeyFor(accountId), accessToken)
            .apply()
    }

    override fun storeLogin(accountId: Int, loginCombo: String?) {
        preferences.edit()
            .putString(loginKeyFor(accountId), loginCombo)
            .apply()
    }

    override fun storeDevice(accountId: Int, deviceName: String?) {
        if (deviceName.isNullOrEmpty()) {
            removeDevice(accountId)
            return
        }
        devices[accountId] = deviceName
        preferences.edit()
            .putString(deviceKeyFor(accountId), deviceName)
            .apply()
    }

    override fun getLogin(accountId: Int): String? {
        return preferences.getString(loginKeyFor(accountId), null)
    }

    override fun storeTokenType(accountId: Int, @AccountType type: Int) {
        types[accountId] = type
        preferences.edit()
            .putInt(typeAccKeyFor(accountId), type)
            .apply()
    }

    override fun getAccessToken(accountId: Int): String? {
        return tokens[accountId]
    }

    override val currentAccessToken: String?
        get() = tokens[current]

    @AccountType
    override fun getType(accountId: Int): Int {
        if (types.containsKey(accountId)) {
            val ret = types[accountId]
            if (ret != null) {
                return ret
            }
        }
        return Constants.DEFAULT_ACCOUNT_TYPE
    }

    override fun getDevice(accountId: Int): String? {
        return if (devices.containsKey(accountId)) {
            devices[accountId]
        } else null
    }

    override fun removeAccessToken(accountId: Int) {
        tokens.remove(accountId)
        preferences.edit()
            .remove(tokenKeyFor(accountId))
            .apply()
    }

    override fun removeType(accountId: Int) {
        types.remove(accountId)
        preferences.edit()
            .remove(typeAccKeyFor(accountId))
            .apply()
    }

    override fun removeLogin(accountId: Int) {
        preferences.edit()
            .remove(loginKeyFor(accountId))
            .apply()
    }

    override fun removeDevice(accountId: Int) {
        devices.remove(accountId)
        preferences.edit()
            .remove(deviceKeyFor(accountId))
            .apply()
    }

    companion object {
        private const val KEY_ACCOUNT_UIDS = "account_uids"
        private const val KEY_CURRENT = "current_account_id"
        internal fun tokenKeyFor(uid: Int): String {
            return "token$uid"
        }

        internal fun loginKeyFor(uid: Int): String {
            return "login$uid"
        }

        internal fun deviceKeyFor(uid: Int): String {
            return "device$uid"
        }

        internal fun typeAccKeyFor(uid: Int): String {
            return "account_type$uid"
        }
    }

    init {
        val aids: Collection<Int> = registered
        for (aid in aids) {
            val token = preferences.getString(tokenKeyFor(aid), null)
            if (token.nonNullNoEmpty()) {
                tokens[aid] = token
            }
            val device = preferences.getString(deviceKeyFor(aid), null)
            if (device.nonNullNoEmpty()) {
                devices[aid] = device
            }
            types[aid] = preferences.getInt(typeAccKeyFor(aid), Constants.DEFAULT_ACCOUNT_TYPE)
        }
    }
}