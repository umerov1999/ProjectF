package dev.ragnarok.fenrir.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.impl.*
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class UploadManagerImpl(
    context: Context,
    private val networker: INetworker,
    private val storages: IStorages,
    private val attachmentsRepository: IAttachmentsRepository,
    private val walls: IWallsRepository
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
    private val serverMap = Collections.synchronizedMap(HashMap<String, UploadServer>())
    private val compositeDisposable = CompositeDisposable()

    @Volatile
    private var current: Upload? = null
    private var needCreateChannel = true
    override fun get(accountId: Long, destination: UploadDestination): Single<List<Upload>> {
        return Single.fromCallable { getByDestination(accountId, destination) }
    }

    private fun getByDestination(accountId: Long, destination: UploadDestination): List<Upload> {
        synchronized(this) {
            val data: MutableList<Upload> = ArrayList()
            for (upload in queue) {
                if (accountId == upload.accountId && destination.compareTo(upload.destination)) {
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
                        context.getString(R.string.channel_upload_files),
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
            val server = serverMap[createServerKey(first)]
            compositeDisposable.add(
                uploadable.doUpload(
                    first,
                    server,
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

            //final int accountId = upload.getAccountId();
            //final UploadDestination destination = upload.getDestination();
            //if (destination.getMethod() == Method.TO_MESSAGE && getByDestination(accountId, destination).isEmpty()) {
            //    sendMessageIfWaitForUpload(accountId, destination.getId());
            //}
            val destination = upload.destination
            if (destination.messageMethod != MessageMethod.VIDEO && destination.messageMethod != MessageMethod.AUDIO && destination.method != Method.VIDEO && destination.method != Method.AUDIO && destination.method != Method.STORY) serverMap[createServerKey(
                upload
            )] = result.server
            completeProcessor.onNext(create(upload, result))
            startIfNotStartedInternal()
        }
    }

    private fun onUploadFail(upload: Upload, t: Throwable) {
        synchronized(this) {
            if (current === upload) {
                current = null
                val cause = getCauseIfRuntime(t)
                val message: String? = if (cause is ApiException) {
                    localizeThrowable(context, cause)
                } else {
                    firstNonEmptyString(cause.message, cause.toString())
                }
                t.printStackTrace()
                compositeDisposable.add(Completable.complete()
                    .observeOn(provideMainThreadScheduler())
                    .subscribe {
                        CustomToast.createCustomToast(context).setDuration(Toast.LENGTH_SHORT)
                            .showToastError(message)
                    })
            }
            val errorMessage: String? = if (t is ApiException) {
                localizeThrowable(context, t)
            } else {
                firstNonEmptyString(t.message, t.toString())
            }
            upload.setStatus(Upload.STATUS_ERROR).errorText = errorMessage
            statusProcessor.onNext(upload)
            startIfNotStartedInternal()
        }
    }

    override fun cancel(id: Int) {
        synchronized(this) {
            if (current != null && (current ?: return@synchronized).getObjectId() == id) {
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

    override fun cancelAll(accountId: Long, destination: UploadDestination) {
        synchronized(this) {
            if (current != null && accountId == current?.accountId && destination.compareTo(
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
                if (accountId == next.accountId && destination.compareTo(next.destination)) {
                    iterator.remove()
                    target.add(next)
                }
            }
            if (target.isNotEmpty()) {
                val ids = IntArray(target.size)
                for (i in target.indices) {
                    ids[i] = target[i].getObjectId()
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
                .map { intArrayOf(it.first.getObjectId()) }
            return Flowable.merge(deletingProcessor.onBackpressureBuffer(), completeIds)
        }
        return deletingProcessor.onBackpressureBuffer()
    }

    override fun observeAdding(): Flowable<List<Upload>> {
        return addingProcessor.onBackpressureBuffer()
    }

    override fun obseveStatus(): Flowable<Upload> {
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
                    ProgressUpdate(pCurrent.getObjectId(), pCurrent.progress)
                return@map listOf(update)
            }
        }
    }

    private fun createUploadable(upload: Upload): IUploadable<*> {
        val destination = upload.destination
        when (destination.method) {
            Method.VIDEO -> return VideoUploadable(context, networker)
            Method.STORY -> return StoryUploadable(context, networker)
            Method.AUDIO -> return AudioUploadable(context, networker)
            Method.REMOTE_PLAY_AUDIO -> return RemoteAudioPlayUploadable(
                context,
                networker
            )

            Method.TO_MESSAGE -> return when (destination.messageMethod) {
                MessageMethod.PHOTO -> Photo2MessageUploadable(
                    context,
                    networker,
                    attachmentsRepository,
                    storages.messages()
                )

                MessageMethod.VIDEO -> VideoToMessageUploadable(
                    context,
                    networker,
                    attachmentsRepository,
                    storages.messages()
                )

                MessageMethod.AUDIO -> AudioToMessageUploadable(
                    context,
                    networker,
                    attachmentsRepository,
                    storages.messages()
                )

                else -> throw UnsupportedOperationException()
            }

            Method.PHOTO_TO_ALBUM -> return Photo2AlbumUploadable(
                context,
                networker,
                storages.photos()
            )

            Method.DOCUMENT -> return DocumentUploadable(
                context,
                networker,
                storages.docs()
            )

            Method.TO_COMMENT, Method.TO_WALL -> return when (destination.messageMethod) {
                MessageMethod.PHOTO -> Photo2WallUploadable(
                    context,
                    networker,
                    attachmentsRepository
                )

                MessageMethod.VIDEO -> Video2WallUploadable(
                    context,
                    networker,
                    attachmentsRepository
                )

                else -> throw UnsupportedOperationException()
            }

            Method.PHOTO_TO_PROFILE -> return OwnerPhotoUploadable(
                context,
                networker,
                walls
            )

            Method.PHOTO_TO_CHAT -> return ChatPhotoUploadable(context, networker)
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
            return Upload(intent.accountId)
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
            builder.append(Extra.ACCOUNT_ID).append(upload.accountId)
            builder.append(Extra.METHOD).append(dest.method)
            when (upload.destination.method) {
                Method.DOCUMENT, Method.VIDEO, Method.AUDIO, Method.TO_COMMENT, Method.TO_WALL -> if (dest.ownerId < 0) {
                    builder.append(Extra.GROUP_ID).append(abs(dest.ownerId))
                }

                Method.PHOTO_TO_ALBUM -> {
                    builder.append(Extra.ALBUM_ID).append(dest.id)
                    if (dest.ownerId < 0) {
                        builder.append(Extra.GROUP_ID).append(abs(dest.ownerId))
                    }
                }

                Method.STORY, Method.REMOTE_PLAY_AUDIO, Method.TO_MESSAGE -> {}
                Method.PHOTO_TO_PROFILE, Method.PHOTO_TO_CHAT -> builder.append(
                    Extra.OWNER_ID
                ).append(dest.ownerId)
            }
            return builder.toString()
        }
    }

}