package dev.ragnarok.fenrir.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dev.ragnarok.fenrir.Constants
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.random.Random

object FileUtil {
    private val random = Random(System.nanoTime())
    private var PHOTO_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)

    fun updateDateLang() {
        PHOTO_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)
    }

    fun getExportedUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
    }

    @Suppress("DEPRECATION")
    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp = PHOTO_DATE_FORMAT.format(Date())
        val externalStorageDir = Environment.getExternalStorageDirectory().absolutePath
        val directory = File(externalStorageDir + "/" + Constants.PHOTOS_PATH)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }
        var targetFile: File? = null
        var noExist = false
        while (!noExist) {
            val fileName = "Captured_" + timeStamp + "_" + random.nextInt(1000000) + ".jpg"
            val file = File(directory, fileName)
            if (!file.exists()) {
                targetFile = file
                noExist = true
            }
        }
        return targetFile!!
    }
}