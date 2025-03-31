package dev.ragnarok.filegallery.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.media.music.NotificationHelper
import dev.ragnarok.filegallery.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.filegallery.upload.impl.RemoteAudioPlayUploadable
import dev.ragnarok.filegallery.util.AppPerms
import dev.ragnarok.filegallery.util.Optional
import dev.ragnarok.filegallery.util.Optional.Companion.wrap
import dev.ragnarok.filegallery.util.Pair
import dev.ragnarok.filegallery.util.Pair.Companion.create
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import dev.ragnarok.filegallery.util.Utils.getCauseIfRuntime
import dev.ragnarok.filegallery.util.coroutines.CompositeJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.inMainThread
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.myEmit
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.filegallery.util.toast.CustomToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class UploadManagerImpl(
    context: Context,
    private val networker: INetworker
) : IUploadManager {
    private val context: Context = context.applicationContext
    private val queue: MutableList<Upload> = ArrayList()
    private val scheduler =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val addingProcessor = MutableSharedFlow<List<Upload>>(replay = 1)
    private val deletingProcessor =
        MutableSharedFlow<IntArray>(replay = 1)
    private val completeProcessor = MutableSharedFlow<Pair<Upload, UploadResult<*>>>(replay = 1)
    private val statusProcessor =
        MutableSharedFlow<Upload>(replay = 1)

    private val timer: Flow<IProgressUpdate?> = flow {
        while (isActive()) {
            delay(PROGRESS_LOOKUP_DELAY.toLong())
            val ret: IProgressUpdate?
            synchronized(this) {
                val pCurrent = current
                ret = if (pCurrent == null) {
                    null
                } else {
                    ProgressUpdate(pCurrent.id, pCurrent.progress)
                }
            }
            emit(ret)
        }
    }
    private val notificationUpdateDisposable = CompositeJob()
    private val compositeDisposable = CompositeJob()

    @Volatile
    private var current: Upload? = null
    private var needCreateChannel = true
    override fun get(destination: UploadDestination): Flow<List<Upload>> {
        return flow { emit(getByDestination(destination)) }
    }

    private fun getByDestination(destination: UploadDestination): List<Upload> {
        synchronized(this) {
            val data: MutableList<Upload> = ArrayList()
            for (upload in queue) {
                if (destination.compareTo(upload.destination)) {
                    data.add(upload)
                }
            }
            return data
        }
    }

    private fun startWithNotification() {
        updateNotification(null)
        notificationUpdateDisposable.add(
            observeProgress().sharedFlowToMain {
                updateNotification(it)
            }
        )
    }

    private fun updateNotification(updates: IProgressUpdate?) {
        updates?.let {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                    ?: return
            if (needCreateChannel) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.files_uploading_notification_title),
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
                needCreateChannel = false
            }
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.files_uploading_notification_title) + " " + it.progress.toString() + "%")
                    .setSmallIcon(R.drawable.ic_notification_upload)
                    .setOngoing(true)
                    .setProgress(100, it.progress, false)
            if (AppPerms.hasNotificationPermissionSimple(context)) {
                notificationManager.notify(NotificationHelper.NOTIFICATION_UPLOAD, builder.build())
            }
        }
    }

    private fun stopNotification() {
        notificationUpdateDisposable.clear()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            notificationManager?.cancel(NotificationHelper.NOTIFICATION_UPLOAD)
        }
    }

    override fun enqueue(intents: List<UploadIntent>) {
        synchronized(this) {
            val all: MutableList<Upload> = ArrayList(intents.size)
            for (intent in intents) {
                val upload = intent2Upload(intent)
                all.add(upload)
                queue.add(upload)
            }
            addingProcessor.myEmit(all)
            startIfNotStarted()
        }
    }

    private fun startIfNotStarted() {
        scheduler.launch {
            startIfNotStartedInternal()
        }
    }

    private fun findFirstQueue(): Upload? {
        var first: Upload? = null
        for (u in queue) {
            if (u.status == Upload.STATUS_QUEUE) {
                first = u
                break
            }
        }
        return first
    }

    private fun startIfNotStartedInternal() {
        synchronized(this) {
            if (current != null) {
                return
            }
            val first = findFirstQueue()
            if (first == null) {
                stopNotification()
                return
            }
            startWithNotification()
            current = first
            first.setStatus(Upload.STATUS_UPLOADING).errorText = null
            statusProcessor.myEmit(first)
            val uploadable = createUploadable(first)
            compositeDisposable.add(
                scheduler.launch {
                    uploadable.doUpload(
                        first,
                        WeakProgressPublisher(first)
                    ).catch {
                        onUploadFail(first, it)
                    }.collect {
                        onUploadComplete(
                            first,
                            it
                        )
                    }
                }
            )
        }
    }

    private fun onUploadComplete(upload: Upload, result: UploadResult<*>) {
        synchronized(this) {
            queue.remove(upload)
            if (current === upload) {
                current = null
            }

            completeProcessor.myEmit(create(upload, result))
            startIfNotStartedInternal()
        }
    }

    private fun onUploadFail(upload: Upload, t: Throwable) {
        synchronized(this) {
            if (current === upload) {
                current = null
                val cause = getCauseIfRuntime(t)
                val message: String? = firstNonEmptyString(cause.message, cause.toString())
                t.printStackTrace()
                compositeDisposable.add(inMainThread {
                    CustomToast.createCustomToast(context, null)
                        ?.setDuration(Toast.LENGTH_SHORT)
                        ?.showToastError(message)
                })
            }
            val errorMessage = firstNonEmptyString(t.message, t.toString())
            upload.setStatus(Upload.STATUS_ERROR).errorText = errorMessage
            statusProcessor.myEmit(upload)
            startIfNotStartedInternal()
        }
    }

    private fun findIndexById(data: List<Upload?>?, id: Int): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i]?.id == id) {
                return i
            }
        }
        return -1
    }

    override fun cancel(id: Int) {
        synchronized(this) {
            if (current != null && (current ?: return@synchronized).id == id) {
                compositeDisposable.clear()
                current = null
            }
            val index = findIndexById(queue, id)
            if (index != -1) {
                queue.removeAt(index)
                deletingProcessor.myEmit(intArrayOf(id))
            }
            startIfNotStarted()
        }
    }

    override fun retry(id: Int) {
        synchronized(this) {
            val index = findIndexById(queue, id)
            if (index != -1) {
                val upload = queue[index]
                upload.setStatus(Upload.STATUS_QUEUE).errorText = null
                statusProcessor.myEmit(upload)
                startIfNotStartedInternal()
            }
        }
    }

    override fun cancelAll(destination: UploadDestination) {
        synchronized(this) {
            if (current != null && destination.compareTo(
                    current?.destination
                )
            ) {
                compositeDisposable.clear()
                current = null
            }
            val target: MutableList<Upload> = ArrayList()
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (destination.compareTo(next.destination)) {
                    iterator.remove()
                    target.add(next)
                }
            }
            if (target.isNotEmpty()) {
                val ids = IntArray(target.size)
                for (i in target.indices) {
                    ids[i] = target[i].id
                }
                deletingProcessor.myEmit(ids)
            }
            startIfNotStarted()
        }
    }

    override fun getCurrent(): Optional<Upload> {
        synchronized(this) { return wrap(current) }
    }

    override fun observeDeleting(includeCompleted: Boolean): Flow<IntArray> {
        if (includeCompleted) {
            return merge(
                completeProcessor
                    .map { intArrayOf(it.first.id) }, deletingProcessor
            )
        }
        return deletingProcessor
    }

    override fun observeAdding(): SharedFlow<List<Upload>> {
        return addingProcessor
    }

    override fun observeStatus(): SharedFlow<Upload> {
        return statusProcessor
    }

    override fun observeResults(): SharedFlow<Pair<Upload, UploadResult<*>>> {
        return completeProcessor
    }

    override fun observeProgress(): Flow<IProgressUpdate?> {
        return timer
    }

    private fun createUploadable(upload: Upload): IUploadable<*> {
        val destination = upload.destination
        when (destination.method) {
            Method.REMOTE_PLAY_AUDIO -> return RemoteAudioPlayUploadable(
                context,
                networker
            )
        }
        throw UnsupportedOperationException()
    }

    class WeakProgressPublisher(upload: Upload) :
        PercentagePublisher {
        private val reference: WeakReference<Upload> = WeakReference(upload)
        override fun onProgressChanged(percentage: Int) {
            val upload = reference.get()
            if (upload != null) {
                upload.progress = percentage
            }
        }

    }

    class ProgressUpdate(override val id: Int, override val progress: Int) : IProgressUpdate
    companion object {
        const val PROGRESS_LOOKUP_DELAY = 500
        const val NOTIFICATION_CHANNEL_ID = "upload_files"
        fun intent2Upload(intent: UploadIntent): Upload {
            return Upload()
                .setAutoCommit(intent.pAutoCommit)
                .setDestination(intent.destination)
                .setFileId(intent.fileId)
                .setFileUri(intent.pFileUri)
                .setStatus(Upload.STATUS_QUEUE)
                .setSize(intent.size)
        }

        fun createServerKey(upload: Upload): String {
            val dest = upload.destination
            val builder = StringBuilder()
            builder.append(Extra.METHOD).append(dest.method)
            return builder.toString()
        }
    }

}
