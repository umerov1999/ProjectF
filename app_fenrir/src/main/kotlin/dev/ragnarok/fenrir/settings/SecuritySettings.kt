package dev.ragnarok.fenrir.settings

import android.content.Context
import android.content.SharedPreferences
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.module.StringHash
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.ISettings.ISecuritySettings
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import java.security.NoSuchAlgorithmException
import java.util.Collections

class SecuritySettings internal constructor(context: Context) : ISecuritySettings {
    private val mPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mApplication: Context = context.applicationContext
    private val mPinEnterHistory: MutableList<Long>
    private val hiddenPeers: MutableSet<String>
    private var mPinHash: String?
    private var mKeyEncryptionPolicyAccepted: Boolean
    override var showHiddenDialogs = false
    override fun reloadHiddenDialogSettings() {
        hiddenPeers.clear()
        hiddenPeers.addAll(
            getPreferences(mApplication).getStringSet(
                KEY_HIDDEN_PEERS,
                HashSet(1)
            ) ?: return
        )
    }

    override val IsShow_hidden_accounts: Boolean
        get() = getPreferences(mApplication)
            .getBoolean("show_hidden_accounts", true)

    private var pinHash: String?
        get() = mPinHash
        private set(pinHash) {
            mPinHash = pinHash
            if (pinHash == null) {
                mPrefs.edit().remove(KEY_PIN_HASH).apply()
            } else {
                mPrefs.edit().putString(KEY_PIN_HASH, pinHash).apply()
            }
        }
    override val pinEnterHistory: List<Long>
        get() = mPinEnterHistory

    private fun storePinHistory() {
        val target: MutableSet<String> = HashSet(mPinEnterHistory.size)
        for (value in mPinEnterHistory) {
            target.add(value.toString())
        }
        mPrefs.edit().putStringSet(KEY_PIN_ENTER_HISTORY, target).apply()
    }

    override fun clearPinHistory() {
        mPinEnterHistory.clear()
        mPrefs.edit().remove(KEY_PIN_ENTER_HISTORY).apply()
    }

    override fun firePinAttemptNow() {
        val now = System.currentTimeMillis()
        mPinEnterHistory.add(now)
        if (mPinEnterHistory.size > pinHistoryDepth) {
            mPinEnterHistory.removeAt(0)
        }
        storePinHistory()
    }

    override fun enableMessageEncryption(
        accountId: Long,
        peerId: Long,
        @KeyLocationPolicy policy: Int
    ) {
        mPrefs.edit()
            .putInt(encryptionKeyFor(accountId, peerId), policy)
            .apply()
    }

    override fun isMessageEncryptionEnabled(accountId: Long, peerId: Long): Boolean {
        return mPrefs.contains(encryptionKeyFor(accountId, peerId))
    }

    override fun disableMessageEncryption(accountId: Long, peerId: Long) {
        mPrefs.edit()
            .remove(encryptionKeyFor(accountId, peerId))
            .apply()
    }

    @KeyLocationPolicy
    override fun getEncryptionLocationPolicy(accountId: Long, peerId: Long): Int {
        return mPrefs.getInt(encryptionKeyFor(accountId, peerId), KeyLocationPolicy.PERSIST)
    }

    override val hasPinHash: Boolean
        get() = mPinHash.nonNullNoEmpty()

    override val pinHistoryDepthValue: Int
        get() = pinHistoryDepth

    override val needHideMessagesBodyForNotif: Boolean
        get() = getPreferences(mApplication)
            .getBoolean("hide_notif_message_body", false)

    override var isUsePinForSecurity: Boolean
        get() = hasPinHash && getPreferences(mApplication)
            .getBoolean(KEY_USE_PIN_FOR_SECURITY, false)
        set(value) = getPreferences(mApplication).edit()
            .putBoolean(KEY_USE_PIN_FOR_SECURITY, value && hasPinHash)
            .apply()

    override var isEntranceByFingerprintAllowed: Boolean
        get() = getPreferences(mApplication).getBoolean("allow_fingerprint", false)
        set(value) = getPreferences(mApplication).edit().putBoolean("allow_fingerprint", value)
            .apply()

    override var isUsePinForEntrance: Boolean
        get() = hasPinHash && getPreferences(mApplication)
            .getBoolean(KEY_USE_PIN_FOR_ENTRANCE, false)
        set(value) = getPreferences(mApplication).edit().putBoolean(KEY_USE_PIN_FOR_ENTRANCE, value)
            .apply()

    override var isDelayedAllow: Boolean
        get() {
            if (!getPreferences(mApplication).getBoolean(DELAYED_PIN_FOR_ENTRANCE, false)) {
                return false
            }
            val last = getPreferences(mApplication).getLong(LAST_PIN_ENTERED, -1)
            if (last <= 0) {
                return false
            }
            val fin = System.currentTimeMillis() - last
            return fin in 1..600000
        }
        set(value) = getPreferences(mApplication).edit().putBoolean(DELAYED_PIN_FOR_ENTRANCE, value)
            .apply()

    override fun updateLastPinTime() {
        getPreferences(mApplication).edit().putLong(LAST_PIN_ENTERED, System.currentTimeMillis())
            .apply()
    }

    override fun setPin(pin: IntArray?) {
        pinHash = pin?.let { calculatePinHash(it) }
    }

    private fun calculatePinHash(values: IntArray): String {
        val builder = StringBuilder()
        for (value in values) {
            builder.append(value)
        }
        return calculateHash(builder.toString())
    }

    override fun isPinValid(values: IntArray): Boolean {
        val hash = calculatePinHash(values)
        return hash == pinHash
    }

    override var isKeyEncryptionPolicyAccepted: Boolean
        get() = mKeyEncryptionPolicyAccepted
        set(accepted) {
            mKeyEncryptionPolicyAccepted = accepted
            mPrefs.edit()
                .putBoolean(KEY_ENCRYPTION_POLICY_ACCEPTED, accepted)
                .apply()
        }

    override fun addHiddenDialog(peerId: Long) {
        hiddenPeers.add(peerId.toString())
        getPreferences(mApplication).edit()
            .putStringSet(KEY_HIDDEN_PEERS, hiddenPeers)
            .apply()
    }

    override fun removeHiddenDialog(peerId: Long) {
        hiddenPeers.remove(peerId.toString())
        getPreferences(mApplication).edit()
            .putStringSet(KEY_HIDDEN_PEERS, hiddenPeers)
            .apply()
    }

    override val hasHiddenDialogs: Boolean
        get() = hiddenPeers.isNotEmpty()

    override fun isHiddenDialog(peerId: Long): Boolean {
        return hiddenPeers.contains(peerId.toString())
    }

    companion object {
        const val KEY_USE_PIN_FOR_SECURITY = "use_pin_for_security"
        const val KEY_CHANGE_PIN = "change_pin"
        const val KEY_DELETE_KEYS = "delete_all_encryption_keys"
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_PIN_HASH = "app_pin"
        private const val KEY_PIN_ENTER_HISTORY = "pin_enter_history"
        const val KEY_USE_PIN_FOR_ENTRANCE = "use_pin_for_entrance"
        const val DELAYED_PIN_FOR_ENTRANCE = "delayed_pin_for_entrance"
        private const val LAST_PIN_ENTERED = "last_pin_entered"
        private const val KEY_ENCRYPTION_POLICY_ACCEPTED = "encryption_policy_accepted"
        private const val KEY_HIDDEN_PEERS = "hidden_peers"
        private const val pinHistoryDepth = 3

        internal fun extractPinEnterHistory(preferences: SharedPreferences): ArrayList<Long> {
            val set = preferences.getStringSet(KEY_PIN_ENTER_HISTORY, null)
            val result = ArrayList<Long>(safeCountOf(set))
            if (set != null) {
                for ((index, value) in set.withIndex()) {
                    result.add(index, value.toLong())
                }
            }
            result.sort()
            return result
        }

        internal fun encryptionKeyFor(accountId: Long, peerId: Long): String {
            return "encryptionkeypolicy" + accountId + "_" + peerId
        }

        internal fun calculateHash(value: String): String {
            return try {
                StringHash.calculateSha1(value)
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException()
            }
        }
    }

    init {
        mPinHash = mPrefs.getString(KEY_PIN_HASH, null)
        mPinEnterHistory = extractPinEnterHistory(mPrefs)
        mKeyEncryptionPolicyAccepted = mPrefs.getBoolean(KEY_ENCRYPTION_POLICY_ACCEPTED, false)
        hiddenPeers = Collections.synchronizedSet(HashSet(1))
        reloadHiddenDialogSettings()
    }
}
