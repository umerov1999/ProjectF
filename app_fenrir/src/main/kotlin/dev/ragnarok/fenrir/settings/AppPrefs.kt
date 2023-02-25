package dev.ragnarok.fenrir.settings

import android.content.Context
import android.content.pm.PackageManager
import dev.ragnarok.fenrir.util.Utils

object AppPrefs {
    var revanced: Pair<String, String>? = null

    fun isCoubInstalled(context: Context): Boolean {
        return isPackageIntalled(context, "com.coub.android")
    }

    fun isNewPipeInstalled(context: Context): Boolean {
        return isPackageIntalled(context, "org.schabi.newpipe")
    }

    fun isYoutubeInstalled(context: Context): Boolean {
        return isPackageIntalled(context, "com.google.android.youtube")
    }

    fun isReVancedYoutubeInstalled(context: Context): Boolean {
        if (isPackageIntalled(context, "app.revanced.android.youtube")) {
            revanced = Pair(
                "app.revanced.android.youtube",
                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
            )
            return true
        } else if (isPackageIntalled(context, "app.rvx.android.youtube")) {
            revanced = Pair(
                "app.rvx.android.youtube",
                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
            )
            return true
        }
        return false
    }

    @Suppress("deprecation")
    private fun isPackageIntalled(context: Context, name: String): Boolean {
        val pm = context.packageManager
        return try {
            if (Utils.hasTiramisu()) pm.getPackageInfo(
                name, PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_ACTIVITIES.toLong()
                )
            ) else pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES)
            if (Utils.hasTiramisu()) pm.getApplicationInfo(
                name,
                PackageManager.ApplicationInfoFlags.of(0)
            ).enabled else pm.getApplicationInfo(name, 0).enabled
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }
}