package dev.ragnarok.filegallery.view.natives.animation

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import dev.ragnarok.filegallery.Constants
import java.io.File
import java.io.InputStream

class AnimationNetworkCache(context: Context) {
    private val appContext = context.applicationContext

    fun fetch(url: String): File? {
        val cachedFile = getCachedFile(url) ?: return null
        if (Constants.IS_DEBUG) {
            Log.d("AnimationNetworkCache", "Cache hit for $url at ${cachedFile.absolutePath}")
        }
        return cachedFile
    }

    fun fetch(@RawRes res: Int): File? {
        val cachedFile = getCachedFile(res) ?: return null
        if (Constants.IS_DEBUG) {
            Log.d("AnimationNetworkCache", "Cache hit for $res at ${cachedFile.absolutePath}")
        }
        return cachedFile
    }

    fun isCachedFile(url: String): Boolean {
        return File(parentDir(appContext), filenameForUrl(url, false)).exists()
    }

    fun isCachedRes(@RawRes res: Int): Boolean {
        return File(parentResDir(appContext), filenameForRes(res, false)).exists()
    }

    fun writeTempCacheFile(url: String, stream: InputStream): File {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(appContext), fileName)
        file.outputStream().use {
            stream.copyTo(it)
            it.flush()
            it.close()
        }
        return file
    }

    fun renameTempFile(url: String) {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(appContext), fileName)
        val newFileName = file.absolutePath.replace(".temp", "")
        val newFile = File(newFileName)
        val renamed = file.renameTo(newFile)
        if (Constants.IS_DEBUG) {
            Log.d("AnimationNetworkCache", "Copying temp file to real file ($newFile)")
        }
        if (!renamed) {
            if (Constants.IS_DEBUG) {
                Log.w(
                    "AnimationNetworkCache",
                    "Unable to rename cache file ${file.absolutePath} to ${newFile.absolutePath}."
                )
            }
        }
    }

    fun renameTempFile(@RawRes res: Int) {
        val fileName = filenameForRes(res, true)
        val file = File(parentResDir(appContext), fileName)
        val newFileName = file.absolutePath.replace(".temp", "")
        val newFile = File(newFileName)
        val renamed = file.renameTo(newFile)
        if (Constants.IS_DEBUG) {
            Log.d("AnimationNetworkCache", "Copying temp file to real file ($newFile)")
        }
        if (!renamed) {
            if (Constants.IS_DEBUG) {
                Log.w(
                    "AnimationNetworkCache",
                    "Unable to rename cache file ${file.absolutePath} to ${newFile.absolutePath}."
                )
            }
        }
    }

    private fun getCachedFile(url: String): File? {
        val file = File(parentDir(appContext), filenameForUrl(url, false))
        return if (file.exists()) {
            file
        } else null
    }

    private fun getCachedFile(@RawRes res: Int): File? {
        val file = File(parentResDir(appContext), filenameForRes(res, false))
        return if (file.exists()) {
            file
        } else null
    }

    companion object {
        private const val TEMP_EXTENSION = ".temp.video"
        private const val EXTENSION = ".video"
        fun filenameForUrl(url: String, isTemp: Boolean) =
            "video_cache_" + url.replace(
                "\\W+".toRegex(),
                ""
            ) + if (isTemp) TEMP_EXTENSION else EXTENSION

        fun filenameForRes(@RawRes res: Int, isTemp: Boolean) =
            "video_res_cache_" + res + if (isTemp) TEMP_EXTENSION else EXTENSION

        fun parentDir(context: Context): File {
            val file = File(context.cacheDir, "video_network_cache")
            if (file.isFile) {
                file.delete()
            }
            if (!file.exists()) {
                file.mkdirs()
            }
            return file
        }

        fun parentResDir(context: Context): File {
            val file = File(context.cacheDir, "video_resource_cache")
            if (file.isFile) {
                file.delete()
            }
            if (!file.exists()) {
                file.mkdirs()
            }
            return file
        }
    }
}
