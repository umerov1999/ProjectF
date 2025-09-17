package dev.ragnarok.fenrir.util

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.core.graphics.createBitmap
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {
    fun makeScreenshot(activity: Activity) {
        try {
            val window = activity.window
            val handler = Handler(Looper.getMainLooper())
            val rectangle = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rectangle)
            val bitmap = createBitmap(rectangle.width(), rectangle.height())
            PixelCopy.request(
                window,
                rectangle,
                bitmap,
                {
                    when (it) {
                        PixelCopy.SUCCESS -> {
                            val saveDir = File(Settings.get().main().photoDir + "/Screenshots")
                            if (!saveDir.exists()) {
                                saveDir.mkdirs()
                            }
                            if (!saveDir.exists()) {
                                CustomToast.createCustomToast(activity, null)
                                    ?.setDuration(Toast.LENGTH_LONG)?.showToastError(
                                        activity.getText(R.string.error)
                                            .toString() + " " + saveDir.absolutePath
                                    )
                                return@request
                            }
                            val file = File(
                                saveDir,
                                "screenshot_" + (System.currentTimeMillis() / 1000) + ".jpg"
                            )
                            val fileOutputStream = FileOutputStream(file)
                            try {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                                fileOutputStream.flush()
                                CustomToast.createCustomToast(activity, null)
                                    ?.setDuration(Toast.LENGTH_LONG)
                                    ?.showToastSuccessBottom(activity.getString(R.string.success) + " " + file.absolutePath)
                                activity.sendBroadcast(
                                    @Suppress("deprecation")
                                    Intent(
                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                        Uri.fromFile(file)
                                    )
                                )
                                fileOutputStream.close()
                            } catch (e: Exception) {
                                fileOutputStream.close()
                                CustomToast.createCustomToast(activity, null)?.showToastThrowable(e)
                            }
                        }

                        else -> {
                            CustomToast.createCustomToast(activity, null)
                                ?.setDuration(Toast.LENGTH_LONG)
                                ?.showToastError(
                                    activity.getText(R.string.error)
                                        .toString() + " " + it.toString()
                                )
                        }
                    }
                },
                handler
            )
        } catch (e: Exception) {
            CustomToast.createCustomToast(activity, null)?.setDuration(Toast.LENGTH_LONG)
                ?.showToastError(
                    activity.getText(R.string.error).toString() + " " + e
                )
        }
    }
}