package dev.ragnarok.fenrir.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.impl.AudioToMessageUploadable
import dev.ragnarok.fenrir.upload.impl.AudioUploadable
import dev.ragnarok.fenrir.upload.impl.ChatPhotoUploadable
import dev.ragnarok.fenrir.upload.impl.DocumentUploadable
import dev.ragnarok.fenrir.upload.impl.OwnerPhotoUploadable
import dev.ragnarok.fenrir.upload.impl.Photo2AlbumUploadable
import dev.ragnarok.fenrir.upload.impl.Photo2MessageUploadable
import dev.ragnarok.fenrir.upload.impl.Photo2WallUploadable
import dev.ragnarok.fenrir.upload.impl.RemoteAudioPlayUploadable
import dev.ragnarok.fenrir.upload.impl.StoryUploadable
import dev.ragnarok.fenrir.upload.impl.Video2WallUploadable
import dev.ragnarok.fenrir.upload.impl.VideoToMessageUploadable
import dev.ragnarok.fenrir.upload.impl.VideoUploadable
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.inMainThread
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.toast.CustomToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Executors
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
    private val scheduler =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val addingProcessor = createPublishSubject<List<Upload>>()
    private val deletingProcessor = createPublishSubject<IntArray>()
    private val completeProcessor = createPublishSubject<Pair<Upload, UploadResult<*>>>()
    private val statusProcessor = createPublishSubject<Upload>()

    private val timer: Flow<IProgressUpdate?> = flow {
        while (isActive()) {
            delay(PROGRESS_LOOKUP_DELAY.toLong())
            val ret: IProgressUpdate?
            synchronized(this) {
                val pCurrent = current
                ret = if (pCurrent == null) {
                    null
                } else {
                    ProgressUpdate(pCurrent.getObjectId(), pCurrent.progress)
                }
            }
            emit(ret)
        }
    }
    private val notificationUpdateDisposable = CompositeJob()
    private val compositeDisposable = CompositeJob()
    private val serverMap = Collections.synchronizedMap(HashMap<String, UploadServer>())

    @Volatile
    private var current: Upload? = null
    private var needCreateChannel = true
    override fun get(accountId: Long, destination: UploadDestination): Flow<List<Upload>> {
        return flow { emit(getByDestination(accountId, destination)) }
    }

    override fun get(accountId: Long, @Method filters: List<Int>): Flow<List<Upload>> {
        return flow {
            val data: MutableList<Upload> = ArrayList()
            synchronized(this) {
                for (upload in queue) {
                    if (accountId == upload.accountId && filters.contains(upload.destination.method)) {
                        data.add(upload)
                    }
                }
            }
            emit(data)
        }
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
        updateNotification(null)
        notificationUpdateDisposable.add(
            observeProgress().sharedFlowToMain {
                updateNotification(it)
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun updateNotification(updates: IProgressUpdate?) {
        updates?.let {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                    ?: return
            if (needCreateChannel) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.channel_upload_files),
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
            val server = serverMap[createServerKey(first)]
            compositeDisposable.add(
                scheduler.launch {
                    uploadable.doUpload(
                        first,
                        server,
                        WeakProgressPublisher(first)
                    ).catch {
                        if (isActive()) {
                            onUploadFail(first, it)
                        }
                    }.collect {
                        if (isActive()) {
                            onUploadComplete(
                                first,
                                it
                            )
                        }
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

            //final int accountId = upload.getAccountId();
            //final UploadDestination destination = upload.getDestination();
            //if (destination.getMethod() == Method.TO_MESSAGE && getByDestination(accountId, destination).isEmpty()) {
            //    sendMessageIfWaitForUpload(accountId, destination.getId());
            //}
            val destination = upload.destination
            if (destination.messageMethod != MessageMethod.VIDEO && destination.messageMethod != MessageMethod.AUDIO && destination.method != Method.VIDEO && destination.method != Method.AUDIO && destination.method != Method.STORY) serverMap[createServerKey(
                upload
            )] = result.server
            completeProcessor.myEmit(create(upload, result))
        }
        startIfNotStartedInternal()
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
                compositeDisposable.add(inMainThread {
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
            statusProcessor.myEmit(upload)
        }
        startIfNotStartedInternal()
    }

    override fun cancel(id: Int) {
        synchronized(this) {
            if (current?.getObjectId() == id) {
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
                startIfNotStarted()
            }
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
                    .map { intArrayOf(it.first.getObjectId()) }, deletingProcessor
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

    class WeakProgressPublisher(upload: Upload) :
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
