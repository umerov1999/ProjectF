package dev.ragnarok.fenrir.upload

import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IUploadManager {
    operator fun get(accountId: Long, destination: UploadDestination): Single<List<Upload>>
    fun enqueue(intents: List<UploadIntent>)
    fun cancel(id: Int)
    fun cancelAll(accountId: Long, destination: UploadDestination)
    fun getCurrent(): Optional<Upload>
    fun observeDeleting(includeCompleted: Boolean): Flowable<IntArray>
    fun observeAdding(): Flowable<List<Upload>>
    fun obseveStatus(): Flowable<Upload>
    fun observeResults(): Flowable<Pair<Upload, UploadResult<*>>>
    fun observeProgress(): Flowable<List<IProgressUpdate>>
    interface IProgressUpdate {
        val id: Int
        val progress: Int
    }
}