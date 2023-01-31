package dev.ragnarok.fenrir

import android.content.res.Resources
import android.os.Build
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import java.util.Locale

object UserAgentTool {
    private inline fun <reified T> byDefaultAccountType(vk_official: T, kate: T): T {
        return if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID) {
            vk_official
        } else kate
    }

    private val SCREEN_RESOLUTION = Resources.getSystem().displayMetrics?.let {
        it.heightPixels.toString() + "x" + it.widthPixels
    } ?: "1920x1080"

    private val KATE_USER_AGENT = String.format(
        Locale.US,
        "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
        Constants.KATE_APP_VERSION_NAME,
        Constants.KATE_APP_VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.SUPPORTED_ABIS[0],
        Utils.deviceName,
        Constants.DEVICE_COUNTRY_CODE,
        SCREEN_RESOLUTION
    )

    private val KATE_USER_AGENT_FAKE = String.format(
        Locale.US,
        "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
        Constants.KATE_APP_VERSION_NAME,
        Constants.KATE_APP_VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        BuildConfig.FAKE_ABI,
        BuildConfig.FAKE_DEVICE,
        Constants.DEVICE_COUNTRY_CODE,
        SCREEN_RESOLUTION
    )
    private val VK_ANDROID_USER_AGENT = String.format(
        Locale.US,
        "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
        Constants.VK_ANDROID_APP_VERSION_NAME,
        Constants.VK_ANDROID_APP_VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.SUPPORTED_ABIS[0],
        Utils.deviceName,
        Constants.DEVICE_COUNTRY_CODE,
        SCREEN_RESOLUTION
    )
    private val VK_ANDROID_USER_AGENT_FAKE = String.format(
        Locale.US,
        "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
        Constants.VK_ANDROID_APP_VERSION_NAME,
        Constants.VK_ANDROID_APP_VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        BuildConfig.FAKE_ABI,
        BuildConfig.FAKE_DEVICE,
        Constants.DEVICE_COUNTRY_CODE,
        SCREEN_RESOLUTION
    )

    fun getAccountUserAgent(@AccountType type: Int, device: String?): String {
        if (type == AccountType.VK_ANDROID_HIDDEN || type == AccountType.KATE_HIDDEN) {
            if (device.nonNullNoEmpty()) {
                return if (type == AccountType.KATE_HIDDEN) String.format(
                    Locale.US,
                    "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
                    Constants.KATE_APP_VERSION_NAME,
                    Constants.KATE_APP_VERSION_CODE,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                    BuildConfig.FAKE_ABI,
                    device,
                    Constants.DEVICE_COUNTRY_CODE,
                    SCREEN_RESOLUTION
                ) else String.format(
                    Locale.US,
                    "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
                    Constants.VK_ANDROID_APP_VERSION_NAME,
                    Constants.VK_ANDROID_APP_VERSION_CODE,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                    BuildConfig.FAKE_ABI,
                    device,
                    Constants.DEVICE_COUNTRY_CODE,
                    SCREEN_RESOLUTION
                )
            }
        }
        return when (type) {
            AccountType.NULL, AccountType.VK_ANDROID -> VK_ANDROID_USER_AGENT
            AccountType.VK_ANDROID_HIDDEN -> VK_ANDROID_USER_AGENT_FAKE
            AccountType.KATE -> KATE_USER_AGENT
            AccountType.KATE_HIDDEN -> KATE_USER_AGENT_FAKE
            else -> byDefaultAccountType(VK_ANDROID_USER_AGENT, KATE_USER_AGENT)
        }
    }

    fun getAccountUserAgent(accountId: Long): String {
        return getAccountUserAgent(
            Settings.get().accounts().getType(accountId),
            Settings.get().accounts().getDevice(accountId)
        )
    }

    val USER_AGENT_CURRENT_ACCOUNT
        get() = Settings.get().accounts().current.let {
            if (it == ISettings.IAccountsSettings.INVALID_ID) {
                byDefaultAccountType(
                    VK_ANDROID_USER_AGENT,
                    KATE_USER_AGENT
                )
            } else getAccountUserAgent(
                Settings.get().accounts().getType(it), Settings.get().accounts().getDevice(it)
            )
        }

    fun getUserAgentByType(@AccountType type: Int): String {
        return when (type) {
            AccountType.NULL, AccountType.VK_ANDROID -> VK_ANDROID_USER_AGENT
            AccountType.VK_ANDROID_HIDDEN -> VK_ANDROID_USER_AGENT_FAKE
            AccountType.KATE -> KATE_USER_AGENT
            AccountType.KATE_HIDDEN -> KATE_USER_AGENT_FAKE
            else -> byDefaultAccountType(VK_ANDROID_USER_AGENT, KATE_USER_AGENT)
        }
    }
}