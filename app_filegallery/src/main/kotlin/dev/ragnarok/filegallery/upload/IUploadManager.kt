package dev.ragnarok.filegallery.upload

import dev.ragnarok.filegallery.util.Optional
import dev.ragnarok.filegallery.util.Pair
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface IUploadManager {
    operator fun get(destination: UploadDestination): Single<List<Upload>>
    fun enqueue(intents: List<UploadIntent>)
    fun cancel(id: Int)
    fun cancelAll(destination: UploadDestination)
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