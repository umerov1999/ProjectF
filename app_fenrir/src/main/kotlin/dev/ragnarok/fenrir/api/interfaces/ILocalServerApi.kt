package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.model.FileRemote
import kotlinx.coroutines.flow.Flow

interface ILocalServerApi {
    @CheckResult
    fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiVideo>>

    @CheckResult
    fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getDiscography(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getPhotos(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiPhoto>>

    @CheckResult
    fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiVideo>>

    @CheckResult
    fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiAudio>>

    @CheckResult
    fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiAudio>>

    @CheckResult
    fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiPhoto>>

    @CheckResult
    fun update_time(hash: String?): Flow<Int>

    @CheckResult
    fun delete_media(hash: String?): Flow<Int>

    @CheckResult
    fun get_file_name(hash: String?): Flow<String>

    @CheckResult
    fun update_file_name(hash: String?, name: String?): Flow<Int>

    @CheckResult
    fun rebootPC(type: String?): Flow<Int>

    @CheckResult
    fun fsGet(dir: String?): Flow<Items<FileRemote>>

    @CheckResult
    fun uploadAudio(hash: String?): Flow<Int>
}