package dev.ragnarok.fenrir.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppPrefs {
    var revanced: Pair<String, String>? = null

    fun isCoubInstalled(context: Context): Boolean {
        return isPackageInstalled(context, "com.coub.android")
    }

    fun isNewPipeInstalled(context: Context): Boolean {
        return isPackageInstalled(context, "org.schabi.newpipe")
    }

    fun isYoutubeInstalled(context: Context): Boolean {
        return isPackageInstalled(context, "com.google.android.youtube")
    }

    fun isReVancedYoutubeInstalled(context: Context): Boolean {
        if (isPackageInstalled(context, "app.revanced.android.youtube")) {
            revanced = Pair(
                "app.revanced.android.youtube",
                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
            )
            return true
        } else if (isPackageInstalled(context, "app.rvx.android.youtube")) {
            revanced = Pair(
                "app.rvx.android.youtube",
                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
            )
            return true
        }
        return false
    }

    private fun isPackageInstalled(context: Context, name: String): Boolean {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getPackageInfo(
                name, PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_ACTIVITIES.toLong()
                )
            ) else pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getApplicationInfo(
                name,
                PackageManager.ApplicationInfoFlags.of(0)
            ).enabled else pm.getApplicationInfo(name, 0).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}