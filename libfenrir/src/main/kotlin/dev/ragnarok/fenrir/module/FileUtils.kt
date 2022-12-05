package dev.ragnarok.fenrir.module

import androidx.annotation.Keep

object FileUtils {
    private external fun listDirRecursiveNative(dir: String, listener: ListDirRecursive)
    private external fun listDirRecursiveNativePointer(dir: String, pointer: Long)
    private external fun audioTagStripNative(audio_file: String): Boolean
    private external fun audioTagModifyNative(
        audio_file: String,
        cover_file: String?,
        cover_mime_type: String?,
        title: String?,
        artist: String?,
        album_title: String?,
        ifGenre: String?,
        comment: String?
    ): Boolean

    fun audioTagStrip(audio_file: String): Boolean {
        return audioTagStripNative(audio_file)
    }

    fun audioTagModify(
        audio_file: String,
        cover_file: String?,
        cover_mime_type: String?,
        title: String?,
        artist: String?,
        album_title: String?,
        ifGenre: String?,
        comment: String?
    ): Boolean {
        return audioTagModifyNative(
            audio_file,
            cover_file,
            cover_mime_type,
            title,
            artist,
            album_title,
            ifGenre,
            comment
        )
    }

    fun listDirRecursive(dir: String, listener: ListDirRecursive) {
        return listDirRecursiveNative(dir, listener)
    }

    fun listDirRecursive(dir: String, pointer: Long) {
        return listDirRecursiveNativePointer(dir, pointer)
    }

    @Keep
    interface ListDirRecursive {
        fun onEntry(file: String)
    }
}