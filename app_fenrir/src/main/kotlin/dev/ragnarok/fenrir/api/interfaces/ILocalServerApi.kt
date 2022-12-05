package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.model.FileRemote
import io.reactivex.rxjava3.core.Single

interface ILocalServerApi {
    @CheckResult
    fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiVideo>>

    @CheckResult
    fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiAudio>>

    @CheckResult
    fun getDiscography(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiAudio>>

    @CheckResult
    fun getPhotos(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiPhoto>>

    @CheckResult
    fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiVideo>>

    @CheckResult
    fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiAudio>>

    @CheckResult
    fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiAudio>>

    @CheckResult
    fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiPhoto>>

    @CheckResult
    fun update_time(hash: String?): Single<Int>

    @CheckResult
    fun delete_media(hash: String?): Single<Int>

    @CheckResult
    fun get_file_name(hash: String?): Single<String>

    @CheckResult
    fun update_file_name(hash: String?, name: String?): Single<Int>

    @CheckResult
    fun rebootPC(type: String?): Single<Int>

    @CheckResult
    fun fsGet(dir: String?): Single<Items<FileRemote>>

    @CheckResult
    fun uploadAudio(hash: String?): Single<Int>
}