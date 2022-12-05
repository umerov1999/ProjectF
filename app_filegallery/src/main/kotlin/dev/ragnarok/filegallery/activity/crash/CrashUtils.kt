package dev.ragnarok.filegallery.activity.crash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.activity.MainActivity
import dev.ragnarok.filegallery.util.Utils
import java.io.PrintWriter
import java.io.StringWriter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object CrashUtils {
    private const val TAG = "CrashUtils"

    fun install(context: Context) {
        try {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread?, throwable: Throwable ->
                if (isStackTraceLikelyConflictive(
                        throwable,
                        DefaultErrorActivity::class.java
                    )
                ) {
                    throwable.printStackTrace()
                    killCurrentProcess()
                } else {
                    val intent = Intent(context, DefaultErrorActivity::class.java)
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    var stackTraceString = sw.toString()
                    if (stackTraceString.length > 1677721) {
                        val disclaimer = " [stack trace too large]"
                        stackTraceString = stackTraceString.substring(
                            0,
                            1677721 - disclaimer.length
                        ) + disclaimer
                    }
                    intent.putExtra(Extra.STACK_TRACE, stackTraceString)
                    intent.putExtra(Extra.IS_OUT_OF_MEMORY, throwable is OutOfMemoryError)

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    killCurrentProcess()
                }
            }
            Log.i(TAG, "CrashActivity has been installed.")
        } catch (t: Throwable) {
            Log.e(
                TAG,
                "An unknown error occurred while installing CrashActivity, it may not have been properly initialized. Please report this as a bug if needed.",
                t
            )
        }
    }

    fun closeApplication(activity: Activity) {
        activity.finish()
        killCurrentProcess()
    }

    private fun getStackTraceFromIntent(intent: Intent): String? {
        return intent.getStringExtra(Extra.STACK_TRACE)
    }

    private val androidVersion: String
        get() {
            val release = Build.VERSION.RELEASE
            val sdkVersion = Build.VERSION.SDK_INT
            return "Android SDK: $sdkVersion ($release)"
        }

    fun getAllErrorDetailsFromIntent(context: Context, intent: Intent): String {
        val currentDate = Date()
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val versionName = getVersionName(context)
        var errorDetails = ""
        errorDetails += "Build version: $versionName \n"
        errorDetails += "Current date: ${dateFormat.format(currentDate)}\n"
        errorDetails += "Android: $androidVersion\n"
        errorDetails += "Device: $deviceModelName\n"
        errorDetails += "Stack trace:\n"
        errorDetails += getStackTraceFromIntent(intent)
        return errorDetails
    }

    private fun restartApplicationWithIntent(
        activity: Activity,
        intent: Intent
    ) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        activity.finish()
        activity.startActivity(intent)
        killCurrentProcess()
    }

    fun restartApplication(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        restartApplicationWithIntent(activity, intent)
    }

    private fun isStackTraceLikelyConflictive(
        throwable: Throwable,
        activityClass: Class<out Activity>
    ): Boolean {
        var pThrowable: Throwable? = throwable
        do {
            val stackTrace = throwable.stackTrace
            for (element in stackTrace) {
                if (element.className == "android.app.ActivityThread" && element.methodName == "handleBindApplication" || element.className == activityClass.name) {
                    return true
                }
            }
        } while (pThrowable?.cause.also { pThrowable = it } != null)
        return false
    }

    @Suppress("deprecation")
    private fun getVersionName(context: Context): String {
        return try {
            val packageInfo = if (Utils.hasTiramisu()) context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ) else context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private val deviceModelName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }

    private fun capitalize(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    private fun killCurrentProcess() {
        Process.killProcess(Process.myPid())
    }
}
