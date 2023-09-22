package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {

    @Suppress("DEPRECATION")
    fun makeScreenshot(activity: Activity) {
        val saveDir = File(Settings.get().main().photoDir + "/Screenshots")
        if (!saveDir.exists()) {
            saveDir.mkdirs()
        }
        if (!saveDir.exists()) {
            CustomToast.createCustomToast(activity).setDuration(Toast.LENGTH_LONG).showToastError(
                activity.getText(R.string.error).toString() + " " + saveDir.absolutePath
            )
            return
        }
        val file = File(
            saveDir,
            "screenshot_" + (System.currentTimeMillis() / 1000) + ".jpg"
        )
        val decorView = activity.window.decorView
        decorView.isDrawingCacheEnabled = true
        val drawingCache = decorView.drawingCache
        val statusBarHeight = getStatusBarHeight(activity)
        val createBitmap = Bitmap.createBitmap(
            drawingCache,
            0,
            statusBarHeight,
            drawingCache.width,
            drawingCache.height - statusBarHeight,
            null,
            true
        )
        decorView.isDrawingCacheEnabled = false
        try {
            val fileOutputStream = FileOutputStream(file)
            try {
                createBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.flush()
                CustomToast.createCustomToast(activity).setDuration(Toast.LENGTH_LONG)
                    .showToastSuccessBottom(activity.getString(R.string.success) + " " + file.absolutePath)
                activity.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)
                    )
                )
                fileOutputStream.close()
            } catch (e: Exception) {
                try {
                    e.printStackTrace()
                    fileOutputStream.close()
                } catch (th: Throwable) {
                    fileOutputStream.close()
                    CustomToast.createCustomToast(activity).showToastThrowable(th)
                }
            } catch (th2: Throwable) {
                fileOutputStream.close()
                CustomToast.createCustomToast(activity).showToastThrowable(th2)
            }
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getStatusBarHeight(activity: Activity): Int {
        val identifier = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (identifier > 0) {
            activity.resources.getDimensionPixelSize(identifier)
        } else 0
    }
}