package dev.ragnarok.filegallery.upload

import dev.ragnarok.filegallery.util.Optional
import dev.ragnarok.filegallery.util.Pair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IUploadManager {
    operator fun get(destination: UploadDestination): Flow<List<Upload>>
    fun enqueue(intents: List<UploadIntent>)
    fun cancel(id: Int)
    fun cancelAll(destination: UploadDestination)
    fun getCurrent(): Optional<Upload>
    fun observeDeleting(includeCompleted: Boolean): Flow<IntArray>
    fun observeAdding(): SharedFlow<List<Upload>>
    fun observeStatus(): SharedFlow<Upload>
    fun observeResults(): SharedFlow<Pair<Upload, UploadResult<*>>>
    fun observeProgress(): Flow<IProgressUpdate?>
    interface IProgressUpdate {
        val id: Int
        val progress: Int
    }
}