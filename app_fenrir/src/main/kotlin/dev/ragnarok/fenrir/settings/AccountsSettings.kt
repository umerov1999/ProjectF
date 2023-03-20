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
import java.util.Collections

internal class AccountsSettings @SuppressLint("UseSparseArrays") constructor(context: Context) :
    IAccountsSettings {
    private val app: Context = context.applicationContext
    private val changesPublisher = PublishProcessor.create<IAccountsSettings>()
    private val preferences: SharedPreferences = getPreferences(context)
    private val tokens: MutableMap<Long, String> = Collections.synchronizedMap(HashMap(1))
    private val types: MutableMap<Long, Int> = Collections.synchronizedMap(HashMap(1))
    private val devices: MutableMap<Long, String> = Collections.synchronizedMap(HashMap(1))
    private val accounts: MutableSet<String> = Collections.synchronizedSet(
        preferences.getStringSet(
            KEY_ACCOUNT_UIDS,
            HashSet(1)
        )!!
    )
    private val currentPublisher = PublishProcessor.create<Long>()
    private fun notifyAboutRegisteredChanges() {
        changesPublisher.onNext(this)
    }

    override fun observeRegistered(): Flowable<IAccountsSettings> {
        return changesPublisher.onBackpressureBuffer()
    }

    override fun observeChanges(): Flowable<Long> {
        return currentPublisher.onBackpressureBuffer()
    }

    override val registered: List<Long>
        get() {
            val ids: MutableList<Long> = ArrayList(accounts.size)
            for (stringuid in accounts) {
                val uid = stringuid.toLong()
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

    override var current: Long
        get() = preferences.getLong(KEY_CURRENT, IAccountsSettings.INVALID_ID)
        set(accountId) {
            if (current == accountId) return
            getPreferences(app)
                .edit()
                .putLong(KEY_CURRENT, accountId)
                .apply()
            fireAccountChange()
        }

    override fun remove(accountId: Long) {
        val currentAccountId = current
        val preferences = getPreferences(app)
        accounts.remove(accountId.toString())
        preferences.edit()
            .putStringSet(KEY_ACCOUNT_UIDS, accounts)
            .apply()
        if (accountId == currentAccountId) {
            val accountIds = registered
            var fisrtUserAccountId: Long? = null

            // делаем активным первый аккаунт ПОЛЬЗОВАТЕЛЯ
            for (existsId in accountIds) {
                if (existsId > 0) {
                    fisrtUserAccountId = existsId
                    break
                }
            }
            if (fisrtUserAccountId != null) {
                preferences.edit()
                    .putLong(KEY_CURRENT, fisrtUserAccountId)
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

    override fun registerAccountId(accountId: Long, setCurrent: Boolean) {
        val preferences = getPreferences(app)
        accounts.add(accountId.toString())
        val editor = preferences.edit()
        editor.putStringSet(KEY_ACCOUNT_UIDS, accounts)
        if (setCurrent) {
            editor.putLong(KEY_CURRENT, accountId)
        }
        editor.apply()
        notifyAboutRegisteredChanges()
        if (setCurrent) {
            fireAccountChange()
        }
    }

    override fun storeAccessToken(accountId: Long, accessToken: String?) {
        accessToken ?: return
        tokens[accountId] = accessToken
        preferences.edit()
            .putString(tokenKeyFor(accountId), accessToken)
            .apply()
    }

    override fun storeLogin(accountId: Long, loginCombo: String?) {
        preferences.edit()
            .putString(loginKeyFor(accountId), loginCombo)
            .apply()
    }

    override fun storeDevice(accountId: Long, deviceName: String?) {
        if (deviceName.isNullOrEmpty()) {
            removeDevice(accountId)
            return
        }
        devices[accountId] = deviceName
        preferences.edit()
            .putString(deviceKeyFor(accountId), deviceName)
            .apply()
    }

    override fun getLogin(accountId: Long): String? {
        return preferences.getString(loginKeyFor(accountId), null)
    }

    override fun storeTokenType(accountId: Long, @AccountType type: Int) {
        types[accountId] = type
        preferences.edit()
            .putInt(typeAccKeyFor(accountId), type)
            .apply()
    }

    override fun getAccessToken(accountId: Long): String? {
        return tokens[accountId]
    }

    override val currentAccessToken: String?
        get() = tokens[current]

    @AccountType
    override fun getType(accountId: Long): Int {
        if (types.containsKey(accountId)) {
            val ret = types[accountId]
            if (ret != null) {
                return ret
            }
        }
        return Constants.DEFAULT_ACCOUNT_TYPE
    }

    override fun getDevice(accountId: Long): String? {
        return if (devices.containsKey(accountId)) {
            devices[accountId]
        } else null
    }

    override fun removeAccessToken(accountId: Long) {
        tokens.remove(accountId)
        preferences.edit()
            .remove(tokenKeyFor(accountId))
            .apply()
    }

    override fun removeType(accountId: Long) {
        types.remove(accountId)
        preferences.edit()
            .remove(typeAccKeyFor(accountId))
            .apply()
    }

    override fun removeLogin(accountId: Long) {
        preferences.edit()
            .remove(loginKeyFor(accountId))
            .apply()
    }

    override fun removeDevice(accountId: Long) {
        devices.remove(accountId)
        preferences.edit()
            .remove(deviceKeyFor(accountId))
            .apply()
    }

    companion object {
        private const val KEY_ACCOUNT_UIDS = "account_uids"
        private const val KEY_CURRENT = "current_account_id_long"
        internal fun tokenKeyFor(uid: Long): String {
            return "token$uid"
        }

        internal fun loginKeyFor(uid: Long): String {
            return "login$uid"
        }

        internal fun deviceKeyFor(uid: Long): String {
            return "device$uid"
        }

        internal fun typeAccKeyFor(uid: Long): String {
            return "account_type$uid"
        }
    }

    init {
        val aids: Collection<Long> = registered
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