package dev.ragnarok.filegallery.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.Includes.provideMainThreadScheduler
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.media.music.NotificationHelper
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.filegallery.upload.impl.RemoteAudioPlayUploadable
import dev.ragnarok.filegallery.util.AppPerms
import dev.ragnarok.filegallery.util.Optional
import dev.ragnarok.filegallery.util.Optional.Companion.wrap
import dev.ragnarok.filegallery.util.Pair
import dev.ragnarok.filegallery.util.Pair.Companion.create
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import dev.ragnarok.filegallery.util.Utils.getCauseIfRuntime
import dev.ragnarok.filegallery.util.toast.CustomToast
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UploadManagerImpl(
    context: Context,
    private val networker: INetworker
) : IUploadManager {
    private val context: Context = context.applicationContext
    private val queue: MutableList<Upload> = ArrayList()
    private val scheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val addingProcessor = PublishProcessor.create<List<Upload>>()
    private val deletingProcessor = PublishProcessor.create<IntArray>()
    private val completeProcessor = PublishProcessor.create<Pair<Upload, UploadResult<*>>>()
    private val statusProcessor = PublishProcessor.create<Upload>()
    private val timer: Flowable<Long> = Flowable.interval(
        PROGRESS_LOOKUP_DELAY.toLong(),
        PROGRESS_LOOKUP_DELAY.toLong(),
        TimeUnit.MILLISECONDS
    ).onBackpressureBuffer()
    private val notificationUpdateDisposable = CompositeDisposable()
    private val compositeDisposable = CompositeDisposable()

    @Volatile
    private var current: Upload? = null
    private var needCreateChannel = true
    override fun get(destination: UploadDestination): Single<List<Upload>> {
        return Single.fromCallable { getByDestination(destination) }
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
        updateNotification(emptyList())
        notificationUpdateDisposable.add(observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { updateNotification(it) })
    }

    @Suppress("DEPRECATION")
    private fun updateNotification(updates: List<IProgressUpdate>) {
        if (updates.nonNullNoEmpty()) {
            val progress = updates[0].progress
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                    ?: return
            val builder: NotificationCompat.Builder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (needCreateChannel) {
                    val channel = NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.files_uploading_notification_title),
                        NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager.createNotificationChannel(channel)
                    needCreateChannel = false
                }
                builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            } else {
                builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).setPriority(
                    Notification.PRIORITY_LOW
                )
            }
            builder.setContentTitle(context.getString(R.string.files_uploading_notification_title))
                .setSmallIcon(R.drawable.ic_notification_upload)
                .setOngoing(true)
                .setProgress(100, progress, false)
                .build()
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
            addingProcessor.onNext(all)
            startIfNotStarted()
        }
    }

    private fun startIfNotStarted() {
        compositeDisposable.add(Completable.complete()
            .observeOn(scheduler)
            .subscribe { startIfNotStartedInternal() })
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
            statusProcessor.onNext(first)
            val uploadable = createUploadable(first)
            compositeDisposable.add(
                uploadable.doUpload(
                    first,
                    WeakProgressPublisgher(first)
                )
                    .subscribeOn(scheduler)
                    .observeOn(scheduler)
                    .subscribe({
                        onUploadComplete(
                            first,
                            it
                        )
                    }, { t -> onUploadFail(first, t) })
            )
        }
    }

    private fun onUploadComplete(upload: Upload, result: UploadResult<*>) {
        synchronized(this) {
            queue.remove(upload)
            if (current === upload) {
                current = null
            }

            completeProcessor.onNext(create(upload, result))
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
                compositeDisposable.add(Completable.complete()
                    .observeOn(provideMainThreadScheduler())
                    .subscribe {
                        CustomToast.createCustomToast(context, null)
                            ?.setDuration(Toast.LENGTH_SHORT)
                            ?.showToastError(message)
                    })
            }
            val errorMessage = firstNonEmptyString(t.message, t.toString())
            upload.setStatus(Upload.STATUS_ERROR).errorText = errorMessage
            statusProcessor.onNext(upload)
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
                deletingProcessor.onNext(intArrayOf(id))
            }
            startIfNotStarted()
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
                deletingProcessor.onNext(ids)
            }
            startIfNotStarted()
        }
    }

    override fun getCurrent(): Optional<Upload> {
        synchronized(this) { return wrap(current) }
    }

    override fun observeDeleting(includeCompleted: Boolean): Flowable<IntArray> {
        if (includeCompleted) {
            val completeIds = completeProcessor.onBackpressureBuffer()
                .map { intArrayOf(it.first.id) }
            return Flowable.merge(deletingProcessor.onBackpressureBuffer(), completeIds)
        }
        return deletingProcessor.onBackpressureBuffer()
    }

    override fun observeAdding(): Flowable<List<Upload>> {
        return addingProcessor.onBackpressureBuffer()
    }

    override fun observeStatus(): Flowable<Upload> {
        return statusProcessor.onBackpressureBuffer()
    }

    override fun observeResults(): Flowable<Pair<Upload, UploadResult<*>>> {
        return completeProcessor.onBackpressureBuffer()
    }

    override fun observeProgress(): Flowable<List<IProgressUpdate>> {
        return timer.map {
            synchronized(this) {
                val pCurrent = current ?: return@map emptyList()
                val update: IProgressUpdate =
                    ProgressUpdate(pCurrent.id, pCurrent.progress)
                return@map listOf(update)
            }
        }
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

    class WeakProgressPublisgher(upload: Upload) :
        PercentagePublisher {
        val reference: WeakReference<Upload> = WeakReference(upload)
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