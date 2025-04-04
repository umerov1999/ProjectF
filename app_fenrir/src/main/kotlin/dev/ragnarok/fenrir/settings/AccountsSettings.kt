package dev.ragnarok.fenrir.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.pushRegistrationResolver
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.flow.SharedFlow
import java.util.Collections

internal class AccountsSettings @SuppressLint("UseSparseArrays") constructor(context: Context) :
    IAccountsSettings {
    private val app: Context = context.applicationContext
    private val changesPublisher = createPublishSubject<IAccountsSettings>()
    private val preferences: SharedPreferences = getPreferences(context)
    private val tokens: MutableMap<Long, String> = Collections.synchronizedMap(HashMap(1))
    private val types: MutableMap<Long, Int> = Collections.synchronizedMap(HashMap(1))
    private val devices: MutableMap<Long, String> = Collections.synchronizedMap(HashMap(1))
    private val accounts: MutableSet<Long> = Collections.synchronizedSet(HashSet(1))
    private val currentPublisher = createPublishSubject<Long>()
    private fun notifyAboutRegisteredChanges() {
        changesPublisher.myEmit(this)
    }

    override val observeRegistered: SharedFlow<IAccountsSettings>
        get() = changesPublisher

    override val observeChanges: SharedFlow<Long>
        get() = currentPublisher

    override val registered: List<Long>
        get() = ArrayList(accounts)

    @SuppressLint("CheckResult")
    private fun fireAccountChange() {
        currentPublisher.myEmit(current)
        pushRegistrationResolver.resolvePushRegistration(current, app).hiddenIO()
    }

    override var current: Long
        get() = preferences.getLong(KEY_CURRENT, IAccountsSettings.INVALID_ID)
        set(accountId) {
            if (current == accountId) return
            getPreferences(app)
                .edit {
                    putLong(KEY_CURRENT, accountId)
                }
            fireAccountChange()
        }

    override fun remove(accountId: Long): Long? {
        val currentAccountId = current
        val preferences = getPreferences(app)
        accounts.remove(accountId)
        val tmpStore = fetchAccountsFromPrefs()
        tmpStore.remove(accountId)
        preferences.edit {
            putStringSet(KEY_ACCOUNT_UIDS, makeAccountsToPrefs(tmpStore))
        }
        var firstUserAccountId: Long? = null
        if (accountId == currentAccountId) {
            val accountIds = registered

            // делаем активным первый аккаунт ПОЛЬЗОВАТЕЛЯ
            for (existsId in accountIds) {
                if (existsId > 0 && !isHiddenType(
                        types[existsId] ?: Constants.DEFAULT_ACCOUNT_TYPE
                    )
                ) {
                    firstUserAccountId = existsId
                    break
                }
            }
            if (firstUserAccountId != null) {
                preferences.edit {
                    putLong(KEY_CURRENT, firstUserAccountId)
                }
            } else {
                preferences.edit {
                    remove(KEY_CURRENT)
                }
            }
        }
        notifyAboutRegisteredChanges()
        fireAccountChange()
        return firstUserAccountId
    }

    override fun registerAccountId(accountId: Long, setCurrent: Boolean) {
        val preferences = getPreferences(app)
        accounts.add(accountId)
        preferences.edit {
            val tmpStore = fetchAccountsFromPrefs()
            tmpStore.add(accountId)
            putStringSet(KEY_ACCOUNT_UIDS, makeAccountsToPrefs(tmpStore))
            if (setCurrent) {
                putLong(KEY_CURRENT, accountId)
            }
        }
        notifyAboutRegisteredChanges()
        if (setCurrent) {
            fireAccountChange()
        }
    }

    override fun storeAccessToken(accountId: Long, accessToken: String?) {
        accessToken ?: return
        tokens[accountId] = accessToken
        preferences.edit {
            putString(tokenKeyFor(accountId), accessToken)
        }
    }

    override fun storeLogin(accountId: Long, loginCombo: String?) {
        preferences.edit {
            putString(loginKeyFor(accountId), loginCombo)
        }
    }

    override fun storeDevice(accountId: Long, deviceName: String?) {
        if (deviceName.isNullOrEmpty()) {
            removeDevice(accountId)
            return
        }
        devices[accountId] = deviceName
        preferences.edit {
            putString(deviceKeyFor(accountId), deviceName)
        }
    }

    override fun getLogin(accountId: Long): String? {
        return preferences.getString(loginKeyFor(accountId), null)
    }

    override fun storeTokenType(accountId: Long, @AccountType type: Int) {
        types[accountId] = type
        preferences.edit {
            putInt(typeAccKeyFor(accountId), type)
        }
    }

    override fun getAccessToken(accountId: Long): String? {
        return tokens[accountId]
    }

    override val currentAccessToken: String?
        get() = tokens[current]

    @AccountType
    override val currentType: Int
        get() = types[current] ?: Constants.DEFAULT_ACCOUNT_TYPE

    private fun isHiddenType(@AccountType type: Int): Boolean {
        return type == AccountType.VK_ANDROID_HIDDEN || type == AccountType.KATE_HIDDEN || type == AccountType.IOS_HIDDEN
    }

    override val currentHidden: Boolean
        get() = isHiddenType(types[current] ?: Constants.DEFAULT_ACCOUNT_TYPE)

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
        preferences.edit {
            remove(tokenKeyFor(accountId))
        }
    }

    override fun removeType(accountId: Long) {
        types.remove(accountId)
        preferences.edit {
            remove(typeAccKeyFor(accountId))
        }
    }

    override fun removeLogin(accountId: Long) {
        preferences.edit {
            remove(loginKeyFor(accountId))
        }
    }

    override fun removeDevice(accountId: Long) {
        devices.remove(accountId)
        preferences.edit {
            remove(deviceKeyFor(accountId))
        }
    }

    private fun makeAccountsToPrefs(data: HashSet<Long>): HashSet<String> {
        val ret = HashSet<String>()
        for (i in data) {
            ret.add(i.toString())
        }
        return ret
    }

    private fun fetchAccountsFromPrefs(): HashSet<Long> {
        val ret = HashSet<Long>()
        for (i in preferences.getStringSet(
            KEY_ACCOUNT_UIDS,
            HashSet(1)
        ).orEmpty()) {
            ret.add(i.toLong())
        }
        return ret
    }

    override fun loadAccounts(refresh: Boolean) {
        if (refresh) {
            types.clear()
            tokens.clear()
            devices.clear()
            accounts.clear()
            accounts.addAll(
                fetchAccountsFromPrefs()
            )
        }
        val disableHidden = preferences.getBoolean("disable_hidden_accounts", false)
        val aids: Collection<Long> = registered

        for (aid in aids) {
            types[aid] = preferences.getInt(typeAccKeyFor(aid), Constants.DEFAULT_ACCOUNT_TYPE)
        }
        for (aid in aids) {
            if (disableHidden && isHiddenType(types[aid] ?: Constants.DEFAULT_ACCOUNT_TYPE)) {
                accounts.remove(aid)
                types.remove(aid)
                continue
            }
            val token = preferences.getString(tokenKeyFor(aid), null)
            if (token.nonNullNoEmpty()) {
                tokens[aid] = token
            }
            val device = preferences.getString(deviceKeyFor(aid), null)
            if (device.nonNullNoEmpty()) {
                devices[aid] = device
            }
        }
    }

    override var anonymToken: AnonymToken
        get() {
            val ret = getPreferences(app).getString("anonym_token_json", null)
            return if (ret == null) {
                AnonymToken()
            } else {
                kJson.decodeFromString(AnonymToken.serializer(), ret)
            }
        }
        set(settings) {
            getPreferences(app).edit {
                putString(
                    "anonym_token_json",
                    kJson.encodeToString(AnonymToken.serializer(), settings)
                )
            }
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
        accounts.addAll(
            fetchAccountsFromPrefs()
        )
        loadAccounts(false)
    }
}
