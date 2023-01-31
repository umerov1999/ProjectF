package dev.ragnarok.filegallery

import android.os.Build
import dev.ragnarok.filegallery.BuildConfig.*
import dev.ragnarok.filegallery.util.Utils
import java.util.*

object Constants {
    const val PICASSO_TAG = "picasso_tag"
    const val EXPORT_SETTINGS_FORMAT = 1
    const val forceDeveloperMode = FORCE_DEVELOPER_MODE

    val IS_DEBUG: Boolean = DEBUG
    const val PIN_DIGITS_COUNT = 4

    val USER_AGENT = String.format(
        Locale.US,
        "FileGalleryAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s)",
        VERSION_NAME,
        VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.SUPPORTED_ABIS[0],
        Utils.deviceName,
        "ru"
    )
    const val AUDIO_PLAYER_SERVICE_IDLE = 300000
    const val FILE_PROVIDER_AUTHORITY = APPLICATION_ID + ".file_provider"
}