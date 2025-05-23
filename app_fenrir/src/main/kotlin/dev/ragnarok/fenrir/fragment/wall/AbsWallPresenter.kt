package dev.ragnarok.fenrir.fragment.wall

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.MessageMethod
import dev.ragnarok.fenrir.upload.Method
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.checkEditInfo
import dev.ragnarok.fenrir.util.Utils.findIndexByPredicate
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.Utils.generateQR
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.intValueNotIn
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import kotlinx.coroutines.flow.filter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Boolean.compare
import kotlin.math.abs

abstract class AbsWallPresenter<V : IWallView> internal constructor(
    accountId: Long,
    val ownerId: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState) {
    protected val wall: MutableList<Post> = ArrayList()

    val stories: MutableList<Story> = ArrayList()
    private val ownersRepository: IOwnersRepository
    private val storiesInteractor: IStoriesShortVideosInteractor
    private val walls: IWallsRepository
    private val cacheCompositeDisposable = CompositeJob()
    private val netCompositeDisposable = CompositeJob()
    protected var endOfContent = false
    var wallFilter: Int
        private set
    private var requestNow = false
    private var nowRequestOffset = 0
    private var nextOffset = 0
    private var actualDataReady = false
    private val uploadManager: IUploadManager = Includes.uploadManager
    private var skipWallOffset = 0
    private var toStory: String? = null
    private val uploadsData: MutableList<Upload> = ArrayList(0)
    open fun searchStory(ByName: Boolean) {
        throw IllegalArgumentException("Unknown story search")
    }

    private fun firePrepared() {
        appendJob(
            uploadManager[accountId, listOf(Method.STORY, Method.PHOTO_TO_PROFILE)]
                .fromIOToMain { data -> onUploadsDataReceived(data) })
        appendJob(
            uploadManager.observeAdding()
                .sharedFlowToMain { added -> onUploadsAdded(added) })
        appendJob(
            uploadManager.observeDeleting(true)
                .sharedFlowToMain { ids -> onUploadDeleted(ids) })
        appendJob(
            uploadManager.observeResults()
                .filter {
                    listOf(
                        Method.STORY,
                        Method.PHOTO_TO_PROFILE
                    ).contains(it.first.destination.method)
                }
                .sharedFlowToMain { pair -> onUploadFinished(pair) })
        appendJob(
            uploadManager.observeStatus()
                .sharedFlowToMain { upload -> onUploadStatusUpdate(upload) })
        appendJob(
            uploadManager.observeProgress()
                .sharedFlowToMain { updates -> onProgressUpdates(updates) })
    }

    fun updateToStory(toStory: String?) {
        this.toStory = toStory
    }

    fun fireRequestSkipOffset() {
        view?.onRequestSkipOffset(accountId, ownerId, wallFilter, nextOffset + skipWallOffset)
    }

    fun fireNarrativesClick() {
        view?.goNarratives(accountId, ownerId)
    }

    fun fireClipsClick() {
        view?.goClips(accountId, ownerId)
    }

    private fun onPostInvalid(postVkid: Int) {
        val index = findIndexByPredicate(
            wall
        ) {
            it.vkid == postVkid
        }
        if (index != -1) {
            wall.removeAt(index)
            view?.notifyWallItemRemoved(index)
        }
    }

    private fun onPostChange(post: Post) {
        val found =
            findInfoByPredicate(wall) {
                it.vkid == post.vkid
            }
        if (!isMatchFilter(post, wallFilter)) {
            // например, при публикации предложенной записи. Надо ли оно тут ?

            /*if (found != null) {
                int index = found.getFirst();
                wall.remove(index);

                if(isGuiReady()){
                    callView(v -> v.notifyWallItemRemoved(index);
                }
            }*/
            return
        }
        if (found != null) {
            val index = found.first
            wall[index] = post
            view?.notifyWallItemChanged(index)
        } else {
            val targetIndex: Int = if (!post.isPinned && wall.isNotEmpty() && wall[0].isPinned) {
                1
            } else {
                0
            }
            wall.add(targetIndex, post)
            view?.notifyWallDataAdded(targetIndex, 1)
        }
    }

    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayWallData(wall)
        viewHost.updateStory(stories)
        resolveLoadMoreFooterView()

        viewHost.displayUploads(uploadsData)
        resolveUploadDataVisibility()
    }

    internal fun onExecuteComplete() {
        onRefresh()
        view?.customToast?.showToast(R.string.success)
    }

    internal fun onExecuteError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    internal abstract fun getOwner(): Owner

    fun fireAddToBlacklistClick() {
        appendJob(
            InteractorFactory.createAccountInteractor()
                .banOwners(accountId, listOf(getOwner()))
                .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireRemoveBlacklistClick() {
        appendJob(
            InteractorFactory.createAccountInteractor()
                .unbanOwner(accountId, ownerId)
                .fromIOToMain({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireRemoveStoryClick(storyOwnerId: Long, id: Int) {
        appendJob(
            storiesInteractor
                .stories_delete(accountId, storyOwnerId, id)
                .fromIOToMain({ fireRefresh() }) { t -> onExecuteError(t) })
    }

    private fun loadWallCachedData() {
        cacheCompositeDisposable.add(
            walls.getCachedWall(accountId, ownerId, wallFilter)
                .fromIOToMain({ posts -> onCachedDataReceived(posts) }) { obj ->
                    obj.printStackTrace()
                    actualDataReady = false
                    requestWall(0)
                })
    }

    private fun onCachedDataReceived(posts: List<Post>) {
        wall.clear()
        wall.addAll(posts)
        actualDataReady = false
        view?.notifyWallDataSetChanged()
        requestWall(0)
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    override fun onDestroyed() {
        cacheCompositeDisposable.cancel()
        super.onDestroyed()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(requestNow && nowRequestOffset == 0)
    }

    private fun safeNotifyWallDataSetChanged() {
        view?.notifyWallDataSetChanged()
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
        resolveLoadMoreFooterView()
    }

    private fun setNowLoadingOffset(offset: Int) {
        nowRequestOffset = offset
    }

    private fun requestWall(offset: Int) {
        setNowLoadingOffset(offset)
        setRequestNow(true)
        val nextOffset = offset + COUNT
        val append = offset > 0
        netCompositeDisposable.add(
            walls.getWall(
                accountId,
                ownerId,
                offset + skipWallOffset,
                COUNT,
                wallFilter,
                skipWallOffset <= 0
            )
                .fromIOToMain({
                    onActualDataReceived(
                        nextOffset,
                        it,
                        append
                    )
                }) { throwable -> onActualDataGetError(throwable) })
    }

    private fun onActualDataGetError(throwable: Throwable) {
        setRequestNow(false)
        showError(getCauseIfRuntime(throwable))
    }

    private fun isExist(post: Post): Boolean {
        for (i in wall) {
            if (i.ownerId == post.ownerId && i.vkid == post.vkid) return true
        }
        return false
    }

    private fun addAll(posts: List<Post>): Int {
        var s = 0
        for (i in posts) {
            if (!isExist(i)) {
                wall.add(i)
                s++
            }
        }
        return s
    }

    private fun onActualDataReceived(nextOffset: Int, posts: List<Post>, append: Boolean) {
        cacheCompositeDisposable.clear()
        actualDataReady = true
        this.nextOffset = nextOffset
        endOfContent = posts.isEmpty()
        if (posts.nonNullNoEmpty()) {
            if (append) {
                val sizeBefore = wall.size
                val sz = addAll(posts)
                view?.notifyWallDataAdded(sizeBefore, sz)
            } else {
                wall.clear()
                addAll(posts)
                view?.notifyWallDataSetChanged()
            }
        }
        setRequestNow(false)
    }

    private fun resolveLoadMoreFooterView() {
        @LoadMoreState val state: Int = if (requestNow) {
            if (nowRequestOffset == 0) {
                LoadMoreState.INVISIBLE
            } else {
                LoadMoreState.LOADING
            }
        } else if (endOfContent) {
            LoadMoreState.END_OF_LIST
        } else {
            LoadMoreState.CAN_LOAD_MORE
        }
        view?.setupLoadMoreFooter(state)
    }

    private fun canLoadMore(): Boolean {
        return !endOfContent && actualDataReady && wall.nonNullNoEmpty() && !requestNow
    }

    private fun requestNext() {
        requestWall(nextOffset)
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    fun fireLoadMoreClick() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    open fun fireOptionViewCreated(view: IWallView.IOptionView) {
        view.setIsMy(accountId == ownerId)
        view.typeOwnerId(ownerId)
    }

    fun fireCreateClick() {
        view?.goToPostCreation(
            accountId,
            ownerId,
            EditingPostType.DRAFT
        )
    }

    private fun fireEdit(context: Context, p: VKApiProfileInfo) {
        val root = View.inflate(context, R.layout.entry_info, null)
        root.findViewById<TextInputEditText>(R.id.edit_first_name).setText(p.first_name)
        root.findViewById<TextInputEditText>(R.id.edit_last_name).setText(p.last_name)
        root.findViewById<TextInputEditText>(R.id.edit_maiden_name).setText(p.maiden_name)
        root.findViewById<TextInputEditText>(R.id.edit_screen_name).setText(p.screen_name)
        root.findViewById<TextInputEditText>(R.id.edit_bdate).setText(p.bdate)
        root.findViewById<TextInputEditText>(R.id.edit_home_town).setText(p.home_town)

        val spinnerItems = ArrayAdapter(
            context,
            R.layout.spinner_item,
            context.resources.getStringArray(R.array.array_sex)
        )

        root.findViewById<MaterialAutoCompleteTextView>(R.id.sex)
            .setText(spinnerItems.getItem(p.sex - 1))
        root.findViewById<MaterialAutoCompleteTextView>(R.id.sex)
            .setAdapter(spinnerItems)
        var selectedItem = p.sex - 1
        root.findViewById<MaterialAutoCompleteTextView>(R.id.sex)
            .setOnItemClickListener { _, _, position, _ ->
                selectedItem = position
            }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                appendJob(
                    InteractorFactory.createAccountInteractor().saveProfileInfo(
                        accountId,
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_first_name).editableText.toString()
                                .trim(), p.first_name
                        ),
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_last_name).editableText.toString()
                                .trim(), p.last_name
                        ),
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_maiden_name).editableText.toString()
                                .trim(), p.maiden_name
                        ),
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_screen_name).editableText.toString()
                                .trim(), p.screen_name
                        ),
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_bdate).editableText.toString()
                                .trim(), p.bdate
                        ),
                        checkEditInfo(
                            root.findViewById<TextInputEditText>(R.id.edit_home_town).editableText.toString()
                                .trim(), p.home_town
                        ),
                        checkEditInfo(
                            selectedItem + 1,
                            p.sex
                        )
                    )
                        .fromIOToMain({ t ->
                            when (t) {
                                0 -> createCustomToast(context).showToastError(R.string.not_changed)
                                1 -> createCustomToast(context).showToastSuccessBottom(R.string.success)
                                2 -> createCustomToast(context).showToastBottom(R.string.later)
                            }
                        }) { t ->
                            showError(t)
                        })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    fun fireEdit(context: Context) {
        appendJob(
            InteractorFactory.createAccountInteractor().getProfileInfo(accountId)
                .fromIOToMain { t -> fireEdit(context, t) })
    }

    fun fireToggleMonitor() {
        if (Settings.get().main().isOwnerInChangesMonitor(ownerId)) {
            Settings.get().main().removeOwnerInChangesMonitor(ownerId)
        } else {
            Settings.get().main().putOwnerInChangesMonitor(ownerId)
        }
    }

    fun canLoadUp(): Boolean {
        return skipWallOffset > 0
    }

    fun fireSkipOffset(skip: Int) {
        skipWallOffset = skip
        netCompositeDisposable.clear()
        cacheCompositeDisposable.clear()
        wall.clear()
        view?.notifyWallDataSetChanged()
        requestWall(0)
    }

    fun fireRefresh() {
        netCompositeDisposable.clear()
        cacheCompositeDisposable.clear()
        requestWall(0)
        if (!Settings.get().main().isDisable_history) {
            appendJob(
                storiesInteractor.getStories(
                    accountId,
                    if (accountId == ownerId) null else ownerId
                )
                    .fromIOToMain {
                        if (it.nonNullNoEmpty()) {
                            stories.clear()
                            stories.addAll(it)
                            view?.updateStory(stories)
                        }
                    })
        }
        onRefresh()
    }

    fun fireShowQR(context: Context) {
        val qr = generateQR(
            "https://vk.com/" + (if (ownerId < 0) "club" else "id") + abs(
                ownerId
            ), context
        )
        val view = LayoutInflater.from(context).inflate(R.layout.qr, null)
        val imageView: ShapeableImageView = view.findViewById(R.id.qr)
        imageView.setImageBitmap(qr)
        MaterialAlertDialogBuilder(context)
            .setCancelable(true)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val path = Environment.getExternalStorageDirectory().absolutePath
                val fOutputStream: OutputStream
                val file = File(
                    path, "qr_fenrir_" + (if (ownerId < 0) "club" else "id") + abs(
                        ownerId
                    ) + ".png"
                )
                try {
                    fOutputStream = FileOutputStream(file)
                    qr?.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream)
                    fOutputStream.flush()
                    fOutputStream.close()
                    context.sendBroadcast(
                        @Suppress("deprecation")
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)
                        )
                    )
                    createCustomToast(context).showToast(R.string.success)
                } catch (e: IOException) {
                    e.printStackTrace()
                    createCustomToast(context).showToastError("Save Failed")
                }
            }
            .setIcon(R.drawable.qr_code)
            .setTitle(R.string.show_qr)
            .setView(view)
            .show()
    }

    protected open fun onRefresh() {}
    fun firePostBodyClick(post: Post) {
        if (intValueIn(post.postType, VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            view?.openPostEditor(accountId, post)
            return
        }
        firePostClick(post)
    }

    fun firePostRestoreClick(post: Post) {
        appendJob(
            walls.restore(accountId, post.ownerId, post.vkid)
                .fromIOToMain(dummy()) { t ->
                    showError(t)
                })
    }

    fun fireLikeLongClick(post: Post) {
        view?.goToLikes(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireShareLongClick(post: Post) {
        view?.goToReposts(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireLikeClick(post: Post) {
        if (Settings.get().main().isDisable_likes || isHiddenAccount(accountId)) {
            return
        }
        appendJob(
            walls.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
                .fromIOToMain(dummy()) { t ->
                    showError(t)
                })
    }

    fun changeWallFilter(mode: Int): Boolean {
        val changed = mode != wallFilter
        wallFilter = mode
        if (changed) {
            cacheCompositeDisposable.clear()
            netCompositeDisposable.clear()
            loadWallCachedData()
            requestWall(0)
        }
        return changed
    }

    val isMyWall: Boolean
        get() = accountId == ownerId

    private fun onPostChange(update: PostUpdate) {
        val pinStateChanged = update.pinUpdate != null
        val index = findByVkid(update.ownerId, update.postId)
        if (index != -1) {
            val post = wall[index]
            update.likeUpdate.requireNonNull {
                post.setLikesCount(it.count)
                post.setUserLikes(it.isLiked)
            }
            update.deleteUpdate.requireNonNull {
                post.setDeleted(it.isDeleted)
            }
            update.pinUpdate.requireNonNull {
                for (p in wall) {
                    p.setPinned(false)
                }
                post.setPinned(it.isPinned)
            }
            if (pinStateChanged) {
                wall.sortWith(COMPARATOR)
                safeNotifyWallDataSetChanged()
            } else {
                view?.notifyWallItemChanged(index)
            }
        }
    }

    private fun findByVkid(ownerId: Long, vkid: Int): Int {
        return indexOf(
            wall
        ) {
            it.ownerId == ownerId && it.vkid == vkid
        }
    }

    fun fireCopyUrlClick() {
        view?.copyToClipboard(
            getString(R.string.link),
            "https://vk.com/" + (if (isCommunity) "club" else "id") + abs(
                ownerId
            )
        )
    }

    fun fireCopyIdClick() {
        view?.copyToClipboard(
            getString(R.string.id),
            ownerId.toString()
        )
    }

    abstract fun fireAddToShortcutClick(context: Context)
    private val isCommunity: Boolean
        get() = ownerId < 0

    fun fireSearchClick() {
        view?.goToWallSearch(accountId, ownerId)
    }

    fun openConversationAttachments() {
        view?.goToConversationAttachments(accountId, ownerId)
    }

    fun fireButtonRemoveClick(post: Post) {
        appendJob(
            walls.delete(accountId, ownerId, post.vkid)
                .fromIOToMain(dummy()) { t ->
                    showError(t)
                })
    }

    companion object {
        private const val COUNT = 20
        private val COMPARATOR = Comparator { rhs: Post, lhs: Post ->
            if (rhs.isPinned == lhs.isPinned) {
                return@Comparator lhs.vkid.compareTo(rhs.vkid)
            }
            compare(lhs.isPinned, rhs.isPinned)
        }

        internal fun isMatchFilter(post: Post, filter: Int): Boolean {
            when (filter) {
                WallCriteria.MODE_ALL -> return intValueNotIn(
                    post.postType,
                    VKApiPost.Type.POSTPONE,
                    VKApiPost.Type.SUGGEST
                )

                WallCriteria.MODE_OWNER -> return (post.authorId == post.ownerId
                        && intValueNotIn(
                    post.postType,
                    VKApiPost.Type.POSTPONE,
                    VKApiPost.Type.SUGGEST
                ))

                WallCriteria.MODE_SCHEDULED -> return post.postType == VKApiPost.Type.POSTPONE
                WallCriteria.MODE_SUGGEST -> return post.postType == VKApiPost.Type.SUGGEST
            }
            throw IllegalArgumentException("Unknown filter")
        }
    }

    private fun doUploadStoryFile(context: Context, file: String) {
        for (i in Settings.get().main().photoExt) {
            if (file.endsWith(i, true)) {
                Uri.fromFile(
                    File(
                        file
                    )
                )
                return
            }
        }
        for (i in Settings.get().main().videoExt) {
            if (file.endsWith(i, true)) {
                doUploadStoryFile(
                    file,
                    0,
                    true
                )
                return
            }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select)
            .setNegativeButton(R.string.video) { _, _ ->
                doUploadStoryFile(
                    file,
                    0,
                    true
                )
            }
            .setPositiveButton(R.string.photo) { _, _ ->
                view?.doEditStoryPhoto(
                    Uri.fromFile(
                        File(
                            file
                        )
                    )
                )
            }
            .create().show()
    }

    fun doUploadStoryFile(file: String, size: Int, isVideo: Boolean) {
        val intents: List<UploadIntent> = if (isVideo) {
            UploadUtils.createIntents(
                accountId,
                UploadDestination.forStory(MessageMethod.VIDEO, toStory),
                file,
                size,
                true
            )
        } else {
            UploadUtils.createIntents(
                accountId,
                UploadDestination.forStory(MessageMethod.PHOTO, toStory),
                file,
                size,
                true
            )
        }
        toStory = null
        uploadManager.enqueue(intents)
    }

    fun fireRemoveClick(upload: Upload) {
        uploadManager.cancel(upload.getObjectId())
    }

    private fun doUploadStoryVideo(file: String) {
        val intents = UploadUtils.createVideoIntents(
            accountId,
            UploadDestination.forStory(MessageMethod.VIDEO, toStory),
            file,
            true
        )
        toStory = null
        uploadManager.enqueue(intents)
    }

    private fun doUploadStoryPhotos(photos: List<LocalPhoto>) {
        if (photos.size == 1) {
            var to_up = photos[0].fullImageUri ?: return
            if (to_up.path?.let { File(it).isFile } == true) {
                to_up = Uri.fromFile(to_up.path?.let { File(it) })
            }
            view?.doEditStoryPhoto(to_up)
            return
        }
        val intents = UploadUtils.createIntents(
            accountId,
            UploadDestination.forStory(MessageMethod.PHOTO, toStory),
            photos,
            Upload.IMAGE_SIZE_FULL,
            true
        )
        toStory = null
        uploadManager.enqueue(intents)
    }

    fun fireStorySelected(
        context: Context,
        localPhotos: ArrayList<LocalPhoto>?,
        file: String?,
        video: LocalVideo?
    ) {
        when {
            file.nonNullNoEmpty() -> doUploadStoryFile(context, file)
            localPhotos.nonNullNoEmpty() -> {
                doUploadStoryPhotos(localPhotos)
            }

            video != null -> {
                doUploadStoryVideo(video.data.toString())
            }
        }
    }

    fun fireNewAvatarPhotoSelected(file: String?) {
        val intent = UploadIntent(accountId, UploadDestination.forProfilePhoto(ownerId))
            .setAutoCommit(true)
            .setFileUri(file?.toUri())
            .setSize(Upload.IMAGE_SIZE_FULL)
        uploadManager.enqueue(listOf(intent))
    }

    private fun onUploadFinished(pair: Pair<Upload, UploadResult<*>>) {
        val destination = pair.first.destination
        if (destination.method == Method.PHOTO_TO_PROFILE && destination.ownerId == ownerId) {
            onRefresh()
            val post = pair.second.result as Post
            resumedView?.showAvatarUploadedMessage(
                accountId,
                post
            )
        } else if (destination.method == Method.STORY && Settings.get()
                .accounts().current == ownerId
        ) {
            fireRefresh()
        }
    }

    private fun resolveUploadDataVisibility() {
        view?.setUploadDataVisible(uploadsData.isNotEmpty())
    }

    private fun onUploadsDataReceived(data: List<Upload>) {
        uploadsData.clear()
        uploadsData.addAll(data)
        resolveUploadDataVisibility()
    }

    private fun onProgressUpdates(updates: IUploadManager.IProgressUpdate?) {
        updates?.let { update ->
            val index = Utils.findIndexById(uploadsData, update.id)
            if (index != -1) {
                view?.notifyUploadProgressChanged(
                    index,
                    update.progress,
                    true
                )
            }
        }
    }

    private fun onUploadStatusUpdate(upload: Upload) {
        val index = Utils.findIndexById(uploadsData, upload.getObjectId())
        if (index != -1) {
            view?.notifyUploadItemChanged(
                index
            )
        }
    }

    private fun onUploadsAdded(added: List<Upload>) {
        var count = 0
        val cur = uploadsData.size
        for (u in added) {
            if (listOf(Method.STORY, Method.PHOTO_TO_PROFILE).contains(u.destination.method)) {
                val index = Utils.findIndexById(uploadsData, u.getObjectId())
                if (index == -1) {
                    uploadsData.add(u)
                    count++
                }
            }
        }
        if (count > 0) {
            view?.notifyUploadItemsAdded(cur, count)
        }
        resolveUploadDataVisibility()
    }

    private fun onUploadDeleted(ids: IntArray) {
        for (id in ids) {
            val index = Utils.findIndexById(uploadsData, id)
            if (index != -1) {
                uploadsData.removeAt(index)
                view?.notifyUploadItemRemoved(
                    index
                )
            }
        }
        resolveUploadDataVisibility()
    }

    private fun checkPostsForAudio(
        toFirst: Boolean,
        position: Int,
        audiosList: ArrayList<Audio>,
        post: Post
    ): Int {
        var tmpPosition = position
        post.attachments?.audios.nonNullNoEmpty {
            if (toFirst) {
                tmpPosition += it.size
                audiosList.addAll(0, it)
            } else {
                audiosList.addAll(it)
            }
        }
        post.getCopyHierarchy()?.nonNullNoEmpty {
            for (i in it) {
                tmpPosition = checkPostsForAudio(toFirst, position, audiosList, i)
            }
        }
        return tmpPosition
    }

    fun fireAudioPlayClick(position: Int, audiosList: ArrayList<Audio>, holderPosition: Int?) {
        if (holderPosition == null) {
            view?.playAudioList(accountId, position, audiosList)
            return
        }
        var tmpPos = position
        val comboAudios = ArrayList<Audio>()
        comboAudios.addAll(audiosList)
        for (i in (holderPosition + 1)..<wall.size.coerceAtMost(100)) {
            tmpPos = checkPostsForAudio(false, tmpPos, comboAudios, wall[i])
        }
        if (holderPosition - 1 >= 0) {
            for (i in (holderPosition - 1) downTo 0) {
                tmpPos = checkPostsForAudio(true, tmpPos, comboAudios, wall[i])
            }
        }
        view?.playAudioList(accountId, tmpPos, comboAudios)
    }

    init {
        wallFilter = WallCriteria.MODE_ALL
        walls = Repository.walls
        ownersRepository = owners
        storiesInteractor = InteractorFactory.createStoriesInteractor()
        loadWallCachedData()
        if (!Settings.get().main().isDisable_history) {
            appendJob(
                storiesInteractor.getStories(
                    accountId,
                    if (accountId == ownerId) null else ownerId
                )
                    .fromIOToMain {
                        if (it.nonNullNoEmpty()) {
                            stories.clear()
                            stories.addAll(it)
                            view?.updateStory(stories)
                        }
                    })
        }
        appendJob(
            walls
                .observeMinorChanges()
                .filter { it.accountId == accountId && it.ownerId == ownerId }
                .sharedFlowToMain { onPostChange(it) })
        appendJob(
            walls
                .observeChanges()
                .filter { it.ownerId == ownerId }
                .sharedFlowToMain { onPostChange(it) })
        appendJob(
            walls
                .observePostInvalidation()
                .filter { it.ownerId == ownerId }
                .sharedFlowToMain { onPostInvalid(it.id) })
        firePrepared()
    }
}