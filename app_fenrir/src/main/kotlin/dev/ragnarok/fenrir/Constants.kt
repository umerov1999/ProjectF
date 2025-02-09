package dev.ragnarok.fenrir

import android.content.res.Resources

object Constants {
    const val API_VERSION = "5.186"
    const val AUTH_API_VERSION = API_VERSION
    const val OLD_API_FOR_AUDIO_VERSION = "5.90"

    const val DATABASE_FENRIR_VERSION = 45
    const val DATABASE_TEMPORARY_VERSION = 10
    const val EXPORT_SETTINGS_FORMAT = 1
    const val forceDeveloperMode = BuildConfig.FORCE_DEVELOPER_MODE

    @AccountType
    val DEFAULT_ACCOUNT_TYPE: Int = AccountType.toAccountType(BuildConfig.DEFAULT_ACCOUNT_TYPE)

    const val FILE_PROVIDER_AUTHORITY: String = "${BuildConfig.APPLICATION_ID}.file_provider"
    const val VK_ANDROID_APP_VERSION_NAME = "8.15"
    const val VK_ANDROID_APP_VERSION_CODE = 15271
    const val KATE_APP_VERSION_NAME = "125 lite"
    const val KATE_APP_VERSION_CODE = 588

    const val IOS_APP_VERSION_CODE = 3893

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

    const val API_TIMEOUT = 25L
    const val EXO_PLAYER_TIMEOUT = 60L
    const val UPLOAD_TIMEOUT = 3600L
    const val DOWNLOAD_TIMEOUT = 3600L
    const val GIF_TIMEOUT = 5L
    const val LONGPOLL_TIMEOUT = 45L
    const val LONGPOLL_WAIT = 25L
    const val PICASSO_TIMEOUT = 15L

    val CATALOG_V2_IGNORE_SECTIONS = arrayOf("podcasts", "radiostations")
}
