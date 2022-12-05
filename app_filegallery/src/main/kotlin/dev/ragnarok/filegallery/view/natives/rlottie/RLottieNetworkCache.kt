package dev.ragnarok.filegallery.view.natives.rlottie

import android.content.Context
import android.util.Log
import dev.ragnarok.filegallery.Constants
import java.io.File
import java.io.InputStream

class RLottieNetworkCache(context: Context) {
    private val appContext = context.applicationContext

    fun fetch(url: String): File? {
        val cachedFile = getCachedFile(url) ?: return null
        if (Constants.IS_DEBUG) {
            Log.d("RLottieNetworkCache", "Cache hit for $url at ${cachedFile.absolutePath}")
        }
        return cachedFile
    }

    fun isCachedFile(url: String): Boolean {
        return File(parentDir(), filenameForUrl(url, false)).exists()
    }

    fun writeTempCacheFile(url: String, stream: InputStream): File {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(), fileName)
        file.outputStream().use {
            stream.copyTo(it)
            it.flush()
            it.close()
        }
        return file
    }

    fun renameTempFile(url: String) {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(), fileName)
        val newFileName = file.absolutePath.replace(".temp", "")
        val newFile = File(newFileName)
        val renamed = file.renameTo(newFile)
        if (Constants.IS_DEBUG) {
            Log.d("RLottieNetworkCache", "Copying temp file to real file ($newFile)")
        }
        if (!renamed) {
            if (Constants.IS_DEBUG) {
                Log.w(
                    "RLottieNetworkCache",
                    "Unable to rename cache file ${file.absolutePath} to ${newFile.absolutePath}."
                )
            }
        }
    }

    private fun getCachedFile(url: String): File? {
        val file = File(parentDir(), filenameForUrl(url, false))
        return if (file.exists()) {
            file
        } else null
    }

    private fun parentDir(): File {
        val file = File(appContext.cacheDir, "lottie_cache")
        if (file.isFile) {
            file.delete()
        }
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    companion object {
        private const val TEMP_JSON_EXTENSION = ".temp.json"
        private const val JSON_EXTENSION = ".json"
        fun filenameForUrl(url: String, isTemp: Boolean) =
            "lottie_cache_" + url.replace(
                "\\W+".toRegex(),
                ""
            ) + if (isTemp) TEMP_JSON_EXTENSION else JSON_EXTENSION
    }
}
