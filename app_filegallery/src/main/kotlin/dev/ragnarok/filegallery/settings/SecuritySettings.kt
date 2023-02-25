package dev.ragnarok.filegallery.settings

import android.content.Context
import android.content.SharedPreferences
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.module.StringHash
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.ISettings.ISecuritySettings
import dev.ragnarok.filegallery.util.Utils.safeCountOf
import java.security.NoSuchAlgorithmException

class SecuritySettings internal constructor(context: Context) : ISecuritySettings {
    private val mPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mApplication: Context = context.applicationContext
    private val mPinEnterHistory: MutableList<Long>
    private var mPinHash: String?

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
    override val isUsePinForEntrance: Boolean
        get() = hasPinHash() && getPreferences(mApplication).getBoolean(
            KEY_USE_PIN_FOR_ENTRANCE,
            false
        )

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

    override fun hasPinHash(): Boolean {
        return mPinHash.nonNullNoEmpty()
    }

    override fun pinHistoryDepthValue(): Int {
        return pinHistoryDepth
    }

    override val isEntranceByFingerprintAllowed: Boolean
        get() = getPreferences(mApplication).getBoolean("allow_fingerprint", false)

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

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_PIN_HASH = "app_pin"
        private const val KEY_PIN_ENTER_HISTORY = "pin_enter_history"
        private const val LAST_PIN_ENTERED = "last_pin_entered"
        const val KEY_USE_PIN_FOR_ENTRANCE = "use_pin_for_entrance"
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
    }
}
