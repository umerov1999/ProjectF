package dev.ragnarok.fenrir.fragment.localserver.filemanagerremote

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.listener.UpdatableNavigation
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackService
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.FileRemote
import dev.ragnarok.fenrir.model.FileType
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView

class FileManagerRemoteFragment :
    BaseMvpFragment<FileManagerRemotePresenter, IFileManagerRemoteView>(),
    IFileManagerRemoteView, FileManagerRemoteAdapter.ClickListener, BackPressCallback {
    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: GridLayoutManager? = null
    private var empty: TextView? = null
    private var loading: ThorVGLottieView? = null
    private var tvCurrentDir: TextView? = null
    private var mAdapter: FileManagerRemoteAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var animationDispose = CancelableJob()
    private var mAnimationLoaded = false
    private var animLoad: ObjectAnimator? = null
    private var mySearchView: MySearchView? = null
    private var musicButton: FloatingActionButton? = null

    override fun onBackPressed(): Boolean {
        if (presenter?.canLoadUp() == true) {
            mLayoutManager?.onSaveInstanceState()?.let { presenter?.backupDirectoryScroll(it) }
            presenter?.loadUp()
            mySearchView?.clear()
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        animationDispose.cancel()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?) =
        FileManagerRemotePresenter(saveInstanceState)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_file_remote_explorer, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mRecyclerView = root.findViewById(R.id.list)
        empty = root.findViewById(R.id.empty)

        mySearchView = root.findViewById(R.id.searchview)
        mySearchView?.setRightButtonVisibility(false)
        mySearchView?.setLeftIcon(R.drawable.magnify)
        mySearchView?.setOnBackButtonClickListener(object : MySearchView.OnBackButtonClickListener {
            override fun onBackButtonClick() {
                presenter?.doSearch(mySearchView?.text.toString())
            }
        })

        mySearchView?.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.doSearch(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.doSearch(newText)
                return false
            }
        })

        val columns = resources.getInteger(R.integer.files_column_count)
        mLayoutManager = GridLayoutManager(requireActivity(), columns, RecyclerView.VERTICAL, false)
        mRecyclerView?.layoutManager = mLayoutManager
        PicassoPauseOnScrollListener.addListener(mRecyclerView)
        tvCurrentDir = root.findViewById(R.id.current_path)
        loading = root.findViewById(R.id.loading)

        animLoad = ObjectAnimator.ofFloat(loading, View.ALPHA, 0.0f).setDuration(1000)
        animLoad?.addListener(object : StubAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                loading?.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
                loading?.visibility = View.GONE
                loading?.alpha = 1f
            }

            override fun onAnimationCancel(animation: Animator) {
                loading?.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
                loading?.visibility = View.GONE
                loading?.alpha = 1f
            }
        })

        mSwipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            mSwipeRefreshLayout?.isRefreshing = false
            if (presenter?.canRefresh() == true) {
                mLayoutManager?.onSaveInstanceState()?.let { presenter?.backupDirectoryScroll(it) }
                presenter?.loadFiles()
            }
        }
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)

        mAdapter = FileManagerRemoteAdapter(requireActivity(), emptyList())
        mAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAdapter

        musicButton = root.findViewById(R.id.music_button)
        musicButton?.setOnLongClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                PlaceFactory.getPlayerPlace(Settings.get().accounts().current)
                    .tryOpenWith(requireActivity())
            } else customToast?.showToastError(R.string.null_audio)
            false
        }
        musicButton?.setOnClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                if (presenter?.scrollTo(curr.id, curr.ownerId) != true) {
                    customToast?.showToastError(R.string.audio_not_found)
                }
            } else customToast?.showToastError(R.string.null_audio)
        }
        return root
    }

    override fun onClick(position: Int, item: FileRemote) {
        if (item.type == FileType.folder) {
            mLayoutManager?.onSaveInstanceState()?.let { presenter?.backupDirectoryScroll(it) }
            presenter?.goFolder(item)
        } else {
            presenter?.onClickFile(item)
        }
    }

    override fun displayData(items: ArrayList<FileRemote>) {
        mAdapter?.setItems(items)
    }

    override fun resolveEmptyText(visible: Boolean) {
        empty?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun resolveLoading(visible: Boolean) {
        animationDispose.cancel()
        if (mAnimationLoaded && !visible) {
            mAnimationLoaded = false
            animLoad?.start()
        } else if (!mAnimationLoaded && visible) {
            animLoad?.end()
            animationDispose += delayTaskFlow(300).toMain {
                mAnimationLoaded = true
                loading?.visibility = View.VISIBLE
                loading?.alpha = 1f
                loading?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.s_loading,
                    intArrayOf(
                        0x333333,
                        CurrentTheme.getColorPrimary(requireActivity()),
                        0x777777,
                        CurrentTheme.getColorSecondary(requireActivity())
                    )
                )
                loading?.startAnimation()
            }
        }
    }

    override fun onError(throwable: Throwable) {
        CustomSnackbars.createCustomSnackbars(mRecyclerView)?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.coloredSnack(
                ErrorLocalizer.localizeThrowable(requireActivity(), throwable),
                Color.RED, false
            )
            ?.show()
    }

    override fun showMessage(@StringRes res: Int) {
        CustomSnackbars.createCustomSnackbars(mRecyclerView)?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.defaultSnack(res, false)?.show()
    }

    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            lazyPresenter {
                result.data?.extras?.let {
                    val p = ParcelNative.fromNative(it.getLong(Extra.PTR))
                        .readParcelableList(Photo.NativeCreator)
                        ?.get(
                            it.getInt(
                                Extra.POSITION
                            )
                        ) ?: return@lazyPresenter

                    scrollTo(p.getObjectId(), p.ownerId)
                } ?: return@lazyPresenter
            }
        }
    }

    override fun displayGalleryUnSafe(parcelNativePointer: Long, position: Int, reversed: Boolean) {
        PlaceFactory.getPhotoAlbumGalleryPlace(
            Settings.get().accounts().current,
            -311,
            Settings.get().accounts().current,
            parcelNativePointer,
            position,
            true,
            reversed
        )
            .setActivityResultLauncher(
                requestPhotoUpdate
            ).tryOpenWith(requireActivity())
    }

    override fun displayVideo(video: Video) {
        PlaceFactory.getVkInternalPlayerPlace(video, InternalVideoSize.SIZE_720, true)
            .tryOpenWith(requireActivity())
    }

    override fun startPlayAudios(audios: ArrayList<Audio>, position: Int) {
        MusicPlaybackService.startForPlayList(requireActivity(), audios, position, false)
        if (!Settings.get().main().isShow_mini_player)
            PlaceFactory.getPlayerPlace(Settings.get().accounts().current)
                .tryOpenWith(requireActivity())
    }

    override fun notifyAllChanged() {
        musicButton?.show()
        mAdapter?.notifyDataSetChanged()
    }

    override fun updatePathString(file: String?) {
        if (file.isNullOrEmpty()) {
            tvCurrentDir?.setText(R.string.root_dir)
        } else {
            tvCurrentDir?.text = file
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
    }

    override fun restoreScroll(scroll: Parcelable) {
        mLayoutManager?.onRestoreInstanceState(scroll)
    }

    override fun onScrollTo(pos: Int) {
        mLayoutManager?.scrollToPosition(pos)
    }

    override fun notifyItemChanged(pos: Int) {
        mAdapter?.notifyItemChanged(pos)
    }

    override fun onResume() {
        super.onResume()
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.files_tab_title)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }
}
