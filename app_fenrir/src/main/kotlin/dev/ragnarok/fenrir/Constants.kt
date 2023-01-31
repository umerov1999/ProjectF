package dev.ragnarok.fenrir

import android.content.res.Resources
import java.util.*

object Constants {
    const val API_VERSION = "5.131"
    const val DATABASE_FENRIR_VERSION = 26
    const val DATABASE_TEMPORARY_VERSION = 5
    const val EXPORT_SETTINGS_FORMAT = 1
    const val forceDeveloperMode = BuildConfig.FORCE_DEVELOPER_MODE

    @AccountType
    val DEFAULT_ACCOUNT_TYPE: Int = AccountType.toAccountType(BuildConfig.DEFAULT_ACCOUNT_TYPE)

    val AUTH_VERSION = if (DEFAULT_ACCOUNT_TYPE == AccountType.KATE) API_VERSION else "5.122"
    const val FILE_PROVIDER_AUTHORITY: String = BuildConfig.APPLICATION_ID + ".file_provider"
    const val VK_ANDROID_APP_VERSION_NAME = "8.12"
    const val VK_ANDROID_APP_VERSION_CODE = "15090"
    const val KATE_APP_VERSION_NAME = "96 lite"
    const val KATE_APP_VERSION_CODE = "529"
    const val API_ID: Int = BuildConfig.VK_API_APP_ID
    const val SECRET: String = BuildConfig.VK_CLIENT_SECRET
    const val PHOTOS_PATH = "DCIM/Fenrir"
    const val AUDIO_PLAYER_SERVICE_IDLE = 300000
    const val PIN_DIGITS_COUNT = 4
    const val MAX_RECENT_CHAT_COUNT = 4
    const val FRAGMENT_CHAT_APP_BAR_VIEW_COUNT = 1
    const val FRAGMENT_CHAT_DOWN_MENU_VIEW_COUNT = 0
    const val PICASSO_TAG = "picasso_tag"

    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    var DEVICE_COUNTRY_CODE = "ru"

    val SCREEN_WIDTH
        get() = Resources.getSystem().displayMetrics?.widthPixels ?: 1920

    val SCREEN_HEIGHT
        get() = Resources.getSystem().displayMetrics?.heightPixels ?: 1080
}
