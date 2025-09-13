package dev.ragnarok.fenrir.activity.photopager

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.util.size
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Rotatable
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.BaseMvpActivity
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.activity.SwipebleActivity
import dev.ragnarok.fenrir.activity.slidr.Slidr
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.base.horizontal.ImageListAdapter
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.PhotoTags
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.place.PlaceUtil
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import dev.ragnarok.fenrir.view.pager.WeakPicassoLoadCallback

class PhotoPagerActivity : BaseMvpActivity<PhotoPagerPresenter, IPhotoPagerView>(), IPhotoPagerView,
    PlaceProvider, AppStyleable, MenuProvider {
    companion object {
        private const val EXTRA_PHOTOS = "photos"
        private const val EXTRA_NEED_UPDATE = "need_update"
        private val SIZES = SparseIntArray()
        private const val DEFAULT_PHOTO_SIZE = PhotoSize.W
        private const val ACTION_OPEN =
            "dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity"

        fun buildArgsForSimpleGallery(
            aid: Long, index: Int, photos: ArrayList<Photo>,
            needUpdate: Boolean
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putParcelableArrayList(EXTRA_PHOTOS, photos)
            args.putInt(Extra.INDEX, index)
            args.putBoolean(EXTRA_NEED_UPDATE, needUpdate)
            return args
        }

        fun buildArgsForSimpleGallery(
            aid: Long, index: Int, parcelNativePointer: Long,
            needUpdate: Boolean
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putInt(Extra.INDEX, index)
            args.putBoolean(EXTRA_NEED_UPDATE, needUpdate)
            args.putLong(EXTRA_PHOTOS, parcelNativePointer)
            Utils.registerParcelNative(parcelNativePointer)
            return args
        }

        fun buildArgsForAlbum(
            aid: Long,
            albumId: Int,
            ownerId: Long,
            source: TmpSource,
            position: Int,
            readOnly: Boolean,
            invert: Boolean
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ALBUM_ID, albumId)
            args.putInt(Extra.INDEX, position)
            args.putBoolean(Extra.READONLY, readOnly)
            args.putBoolean(Extra.INVERT, invert)
            args.putParcelable(Extra.SOURCE, source)
            return args
        }

        fun buildArgsForAlbum(
            aid: Long,
            albumId: Int,
            ownerId: Long,
            photos: ArrayList<Photo>,
            position: Int,
            readOnly: Boolean,
            invert: Boolean
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ALBUM_ID, albumId)
            args.putInt(Extra.INDEX, position)
            args.putBoolean(Extra.READONLY, readOnly)
            args.putBoolean(Extra.INVERT, invert)
            args.putParcelableArrayList(EXTRA_PHOTOS, photos)
            return args
        }

        fun buildArgsForAlbum(
            aid: Long,
            albumId: Int,
            ownerId: Long,
            parcelNativePointer: Long,
            position: Int,
            readOnly: Boolean,
            invert: Boolean
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ALBUM_ID, albumId)
            args.putInt(Extra.INDEX, position)
            args.putBoolean(Extra.READONLY, readOnly)
            args.putBoolean(Extra.INVERT, invert)
            args.putLong(EXTRA_PHOTOS, parcelNativePointer)
            Utils.registerParcelNative(parcelNativePointer)
            return args
        }

        fun buildArgsForFave(aid: Long, photos: ArrayList<Photo>, index: Int): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putParcelableArrayList(EXTRA_PHOTOS, photos)
            args.putInt(Extra.INDEX, index)
            return args
        }

        fun buildArgsForFave(aid: Long, photos: Long, index: Int): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putLong(EXTRA_PHOTOS, photos)
            args.putInt(Extra.INDEX, index)
            return args
        }

        private var mLastBackPressedTime: Long = 0

        fun newInstance(context: Context, placeType: Int, args: Bundle?): Intent? {
            if (mLastBackPressedTime + 1000 > System.currentTimeMillis()) {
                return null
            }
            mLastBackPressedTime = System.currentTimeMillis()
            val ph = Intent(context, PhotoPagerActivity::class.java)
            val targetArgs = Bundle()
            targetArgs.putAll(args)
            targetArgs.putInt(Extra.PLACE_TYPE, placeType)
            ph.action = ACTION_OPEN
            ph.putExtras(targetArgs)
            return ph
        }

        internal fun addPhotoSizeToMenu(menu: PopupMenu, id: Int, size: Int, selectedItem: Int) {
            menu.menu
                .add(0, id, 0, getTitleForPhotoSize(size)).isChecked = selectedItem == size
        }

        internal fun getTitleForPhotoSize(size: Int): String {
            return when (size) {
                PhotoSize.X -> 604.toString() + "px"
                PhotoSize.Y -> 807.toString() + "px"
                PhotoSize.Z -> 1024.toString() + "px"
                PhotoSize.W -> 2048.toString() + "px"
                else -> throw IllegalArgumentException("Unsupported size")
            }
        }

        init {
            SIZES.put(1, PhotoSize.X)
            SIZES.put(2, PhotoSize.Y)
            SIZES.put(3, PhotoSize.Z)
            SIZES.put(4, PhotoSize.W)
        }
    }

    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter { fireWriteExternalStoragePermissionResolved(this@PhotoPagerActivity) }
    }

    private var mViewPager: ViewPager2? = null
    private var mContentRoot: RelativeLayout? = null
    private var mButtonWithUser: CircleCounterButton? = null
    private var mButtonLike: CircleCounterButton? = null
    private var mButtonComments: CircleCounterButton? = null
    private var buttonShare: CircleCounterButton? = null
    private var mLoadingProgressBar: ThorVGLottieView? = null
    private var mLoadingProgressBarDispose = CancelableJob()
    private var mLoadingProgressBarLoaded = false
    private var mToolbar: Toolbar? = null
    private var mButtonsRoot: View? = null
    private var mPreviewsRecycler: RecyclerView? = null
    private var mButtonRestore: MaterialButton? = null
    private var mPagerAdapter: Adapter? = null
    private var mCanSaveYourself = false
    private var mCanDelete = false
    private val bShowPhotosLine = Settings.get().main().isShow_photos_line
    private val mAdapterRecycler = ImageListAdapter()

    @get:LayoutRes
    override val noMainContentView: Int
        get() = R.layout.activity_photo_pager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Slidr.attach(
            this,
            SlidrConfig.Builder().setAlphaForView(false).fromUnColoredToColoredStatusBar(true)
                .position(SlidrPosition.VERTICAL)
                .listener(object : SlidrListener {
                    override fun onSlideStateChanged(state: Int) {

                    }

                    @SuppressLint("Range")
                    override fun onSlideChange(percent: Float) {
                        var tmp = 1f - percent
                        tmp *= 4
                        tmp = Utils.clamp(1f - tmp, 0f, 1f)
                        mContentRoot?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                        mButtonsRoot?.alpha = tmp
                        mToolbar?.alpha = tmp
                        mPreviewsRecycler?.alpha = tmp
                        mViewPager?.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {

                    }

                    override fun onSlideClosed(): Boolean {
                        presenter?.close()
                        return true
                    }

                }).build()
        )
        mContentRoot = findViewById(R.id.photo_pager_root)
        mLoadingProgressBar = findViewById(R.id.loading_progress_bar)
        mButtonRestore = findViewById(R.id.button_restore)
        mButtonsRoot = findViewById(R.id.buttons)
        mPreviewsRecycler = findViewById(R.id.previews_photos)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mViewPager = findViewById(R.id.view_pager)
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        mButtonLike = findViewById(R.id.like_button)
        mButtonLike?.setOnClickListener { presenter?.fireLikeClick() }
        mButtonLike?.setOnLongClickListener {
            presenter?.fireLikeLongClick()
            false
        }
        mButtonWithUser = findViewById(R.id.with_user_button)
        mButtonWithUser?.setOnClickListener { presenter?.fireWithUserClick() }
        mButtonWithUser?.setOnLongClickListener {
            presenter?.fireWithUserLongClick(this)
            true
        }
        mButtonComments = findViewById(R.id.comments_button)
        mButtonComments?.setOnClickListener { presenter?.fireCommentsButtonClick() }
        buttonShare = findViewById(R.id.share_button)
        buttonShare?.setOnClickListener { presenter?.fireShareButtonClick() }
        mButtonRestore?.setOnClickListener { presenter?.fireButtonRestoreClick() }

        if (bShowPhotosLine) {
            mPreviewsRecycler?.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            mAdapterRecycler.setListener(object : ImageListAdapter.OnRecyclerImageClickListener {
                override fun onRecyclerImageClick(index: Int) {
                    mViewPager?.currentItem = index
                }
            })
            mPreviewsRecycler?.adapter = mAdapterRecycler
        } else {
            mPreviewsRecycler?.visibility = View.GONE
        }

        addMenuProvider(this, this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                presenter?.close()
            }
        })
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                AudioPlayerFragment.newInstance(args).show(supportFragmentManager, "audio_player")
            }

            else -> {
                val intent = Intent(this, SwipebleActivity::class.java)
                intent.action = MainActivity.ACTION_OPEN_PLACE
                intent.putExtra(Extra.PLACE, place)
                SwipebleActivity.start(this, intent)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.vkphoto_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.photo_size -> onPhotoSizeClicked()
            R.id.save_on_drive -> {
                presenter?.fireSaveOnDriveClick(this)
                return true
            }

            R.id.save_yourself -> presenter?.fireSaveYourselfClick()
            R.id.action_delete -> presenter?.fireDeleteClick()
            R.id.info -> presenter?.fireInfoButtonClick(this)
            R.id.detect_qr -> presenter?.fireDetectQRClick(this)
        }
        return false
    }

    override fun goToLikesList(accountId: Long, ownerId: Long, photoId: Int) {
        PlaceFactory.getLikesCopiesPlace(
            accountId,
            "photo",
            ownerId,
            photoId,
            ILikesInteractor.FILTER_LIKES
        ).tryOpenWith(this)
    }

    override fun onPrepareMenu(menu: Menu) {
        if (!Utils.isHiddenCurrent) {
            menu.findItem(R.id.save_yourself).isVisible = mCanSaveYourself
            menu.findItem(R.id.action_delete).isVisible = mCanDelete
        } else {
            menu.findItem(R.id.save_yourself).isVisible = false
            menu.findItem(R.id.action_delete).isVisible = false
        }
        val imageSize = photoSizeFromPrefs
        menu.findItem(R.id.photo_size).title = getTitleForPhotoSize(imageSize)
    }

    private fun onPhotoSizeClicked() {
        val view = findViewById<View>(R.id.photo_size)
        val current = photoSizeFromPrefs
        val popupMenu = PopupMenu(this, view)
        for (i in 0 until SIZES.size) {
            val key = SIZES.keyAt(i)
            val value = SIZES[key]
            addPhotoSizeToMenu(popupMenu, key, value, current)
        }
        popupMenu.menu.setGroupCheckable(0, true, true)
        popupMenu.setOnMenuItemClickListener {
            val key = it.itemId
            Settings.get()
                .main()
                .setPrefDisplayImageSize(SIZES[key])
            invalidateOptionsMenu()
            true
        }
        popupMenu.show()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): PhotoPagerPresenter {
        val placeType = requireArguments().getInt(Extra.PLACE_TYPE)
        val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
        when (placeType) {
            Place.SIMPLE_PHOTO_GALLERY -> {
                val index = requireArguments().getInt(Extra.INDEX)
                val needUpdate = requireArguments().getBoolean(EXTRA_NEED_UPDATE)
                val photos: ArrayList<Photo> =
                    requireArguments().getParcelableArrayListCompat(EXTRA_PHOTOS)!!
                return SimplePhotoPresenter(
                    photos,
                    index,
                    needUpdate,
                    aid,
                    saveInstanceState
                )
            }

            Place.SIMPLE_PHOTO_GALLERY_NATIVE -> {
                val index = requireArguments().getInt(Extra.INDEX)
                val needUpdate = requireArguments().getBoolean(EXTRA_NEED_UPDATE)
                if (!FenrirNative.isNativeLoaded || !Settings.get()
                        .main().isNative_parcel_photo
                ) {
                    val photos: ArrayList<Photo> =
                        requireArguments().getParcelableArrayListCompat(EXTRA_PHOTOS)!!
                    return SimplePhotoPresenter(
                        photos,
                        index,
                        needUpdate,
                        aid,
                        saveInstanceState
                    )
                } else {
                    var source: Long = requireArguments().getLong(EXTRA_PHOTOS)
                    requireArguments().putLong(EXTRA_PHOTOS, 0)
                    if (!Utils.isParcelNativeRegistered(source)) {
                        source = 0
                    }
                    Utils.unregisterParcelNative(source)
                    return SimplePhotoPresenter(
                        source,
                        index,
                        needUpdate,
                        aid,
                        saveInstanceState
                    )
                }
            }

            Place.VK_PHOTO_ALBUM_GALLERY_SAVED -> {
                val indexx = requireArguments().getInt(Extra.INDEX)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val albumId = requireArguments().getInt(Extra.ALBUM_ID)
                val readOnly = requireArguments().getBoolean(Extra.READONLY)
                val invert = requireArguments().getBoolean(Extra.INVERT)
                val source: TmpSource =
                    requireArguments().getParcelableCompat(Extra.SOURCE)!!
                return PhotoAlbumPagerPresenter(
                    indexx,
                    aid,
                    ownerId,
                    albumId,
                    source,
                    readOnly,
                    invert,
                    saveInstanceState
                )
            }

            Place.VK_PHOTO_ALBUM_GALLERY_NATIVE -> {
                val indexx = requireArguments().getInt(Extra.INDEX)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val albumId = requireArguments().getInt(Extra.ALBUM_ID)
                val readOnly = requireArguments().getBoolean(Extra.READONLY)
                val invert = requireArguments().getBoolean(Extra.INVERT)
                var nativePointer = requireArguments().getLong(
                    EXTRA_PHOTOS
                )
                if (!Utils.isParcelNativeRegistered(nativePointer)) {
                    nativePointer = 0
                }
                Utils.unregisterParcelNative(nativePointer)
                requireArguments().putLong(EXTRA_PHOTOS, 0)
                if (FenrirNative.isNativeLoaded && Settings.get()
                        .main().isNative_parcel_photo && nativePointer != 0L
                ) {
                    return PhotoAlbumPagerPresenter(
                        indexx,
                        aid,
                        ownerId,
                        albumId,
                        nativePointer,
                        readOnly,
                        invert,
                        saveInstanceState
                    )
                }
                return PhotoAlbumPagerPresenter(
                    indexx,
                    aid,
                    ownerId,
                    albumId,
                    ArrayList(),
                    readOnly,
                    invert,
                    saveInstanceState
                )
            }

            Place.VK_PHOTO_ALBUM_GALLERY -> {
                val indexx = requireArguments().getInt(Extra.INDEX)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val albumId = requireArguments().getInt(Extra.ALBUM_ID)
                val readOnly = requireArguments().getBoolean(Extra.READONLY)
                val invert = requireArguments().getBoolean(Extra.INVERT)
                val photos_album: ArrayList<Photo> =
                    requireArguments().getParcelableArrayListCompat(EXTRA_PHOTOS)!!
                return PhotoAlbumPagerPresenter(
                    indexx,
                    aid,
                    ownerId,
                    albumId,
                    photos_album,
                    readOnly,
                    invert,
                    saveInstanceState
                )
            }

            Place.FAVE_PHOTOS_GALLERY -> {
                val findex = requireArguments().getInt(Extra.INDEX)
                if (!FenrirNative.isNativeLoaded || !Settings.get()
                        .main().isNative_parcel_photo
                ) {
                    val favePhotos: ArrayList<Photo> =
                        requireArguments().getParcelableArrayListCompat(EXTRA_PHOTOS)!!
                    return FavePhotoPagerPresenter(
                        favePhotos,
                        findex,
                        aid,
                        saveInstanceState
                    )
                } else {
                    var source: Long = requireArguments().getLong(EXTRA_PHOTOS)
                    requireArguments().putLong(EXTRA_PHOTOS, 0)
                    if (!Utils.isParcelNativeRegistered(source)) {
                        source = 0
                    }
                    Utils.unregisterParcelNative(source)
                    return FavePhotoPagerPresenter(
                        source,
                        findex,
                        aid,
                        saveInstanceState
                    )
                }
            }

            Place.VK_PHOTO_TMP_SOURCE -> {
                if (!FenrirNative.isNativeLoaded || !Settings.get()
                        .main().isNative_parcel_photo
                ) {
                    val source: TmpSource =
                        requireArguments().getParcelableCompat(Extra.SOURCE)!!
                    return TmpGalleryPagerPresenter(
                        aid,
                        source,
                        requireArguments().getInt(Extra.INDEX),
                        saveInstanceState
                    )
                } else {
                    var source: Long = requireArguments().getLong(Extra.SOURCE)
                    requireArguments().putLong(Extra.SOURCE, 0)
                    if (!Utils.isParcelNativeRegistered(source)) {
                        source = 0
                    }
                    Utils.unregisterParcelNative(source)
                    return TmpGalleryPagerPresenter(
                        aid,
                        source,
                        requireArguments().getInt(Extra.INDEX),
                        saveInstanceState
                    )
                }
            }
        }
        throw UnsupportedOperationException()
    }

    override fun setupLikeButton(visible: Boolean, like: Boolean, likes: Int) {
        mButtonLike?.visibility = if (visible) View.VISIBLE else View.GONE
        mButtonLike?.isActive = like
        mButtonLike?.count = likes
        mButtonLike?.setIcon(if (like) R.drawable.heart_filled else R.drawable.heart)
    }

    override fun setupWithUserButton(users: Int) {
        mButtonWithUser?.visibility = if (users > 0) View.VISIBLE else View.GONE
        mButtonWithUser?.count = users
    }

    override fun setupShareButton(visible: Boolean, reposts: Int) {
        buttonShare?.visibility = if (visible) View.VISIBLE else View.GONE
        buttonShare?.count = reposts
    }

    override fun setupCommentsButton(visible: Boolean, count: Int) {
        mButtonComments?.visibility = if (visible) View.VISIBLE else View.GONE
        mButtonComments?.count = count
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            presenter?.firePageSelected(position)

            if (bShowPhotosLine && mAdapterRecycler.getSize() > 1) {
                val currentSelected = mAdapterRecycler.getSelectedItem()
                if (currentSelected != position) {
                    mAdapterRecycler.selectPosition(position)
                    mPreviewsRecycler?.scrollToPosition(position)
                }
            }
        }
    }

    override fun displayPhotos(photos: List<Photo>, initialIndex: Int) {
        if (bShowPhotosLine) {
            if (photos.size <= 1) {
                mAdapterRecycler.setData(emptyList())
                mAdapterRecycler.notifyDataSetChanged()
            } else {
                mAdapterRecycler.setData(photos)
                mAdapterRecycler.notifyDataSetChanged()
                mAdapterRecycler.selectPosition(initialIndex)
                mPreviewsRecycler?.scrollToPosition(initialIndex)
            }
        }
        mViewPager?.unregisterOnPageChangeCallback(pageChangeListener)
        mPagerAdapter = Adapter(photos)
        mViewPager?.adapter = mPagerAdapter
        mViewPager?.setCurrentItem(initialIndex, false)
        mViewPager?.registerOnPageChangeCallback(pageChangeListener)
    }

    override fun sharePhoto(accountId: Long, photo: Photo) {
        val items = arrayOf(
            getString(R.string.share_link),
            getString(R.string.repost_send_message),
            getString(R.string.repost_to_wall)
        )
        MaterialAlertDialogBuilder(this)
            .setItems(items) { _, i ->
                when (i) {
                    0 -> Utils.shareLink(this, photo.generateWebLink(), photo.text)
                    1 -> SendAttachmentsActivity.startForSendAttachments(
                        this,
                        accountId,
                        photo
                    )

                    2 -> presenter?.firePostToMyWallClick()
                }
            }
            .setCancelable(true)
            .setTitle(R.string.share_photo_title)
            .show()
    }

    override fun postToMyWall(photo: Photo, accountId: Long) {
        PlaceUtil.goToPostCreation(
            this,
            accountId,
            accountId,
            EditingPostType.TEMP,
            listOf(photo)
        )
    }

    override fun requestWriteToExternalStoragePermission() {
        requestWritePermission.launch()
    }

    override fun setButtonRestoreVisible(visible: Boolean) {
        mButtonRestore?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setupOptionMenu(canSaveYourself: Boolean, canDelete: Boolean) {
        mCanSaveYourself = canSaveYourself
        mCanDelete = canDelete
        this.invalidateOptionsMenu()
    }

    override fun goToComments(accountId: Long, commented: Commented) {
        PlaceFactory.getCommentsPlace(accountId, commented, null).tryOpenWith(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoadingProgressBarDispose.cancel()
    }

    override fun displayPhotoListLoading(loading: Boolean) {
        mLoadingProgressBarDispose.cancel()
        if (loading) {
            mLoadingProgressBarDispose += delayTaskFlow(300).toMain {
                mLoadingProgressBarLoaded = true
                mLoadingProgressBar?.visibility = View.VISIBLE
                mLoadingProgressBar?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    intArrayOf(
                        0x000000,
                        Color.WHITE,
                        0x777777,
                        Color.WHITE
                    )
                )
                mLoadingProgressBar?.startAnimation()
            }
        } else if (mLoadingProgressBarLoaded) {
            mLoadingProgressBarLoaded = false
            mLoadingProgressBar?.visibility = View.GONE
            mLoadingProgressBar?.clearAnimationDrawable(
                callSuper = true, clearState = true,
                cancelTask = true
            )
        }
    }

    override fun setButtonsBarVisible(visible: Boolean) {
        mButtonsRoot?.visibility = if (visible) View.VISIBLE else View.GONE
        mPreviewsRecycler?.visibility = if (visible && bShowPhotosLine) View.VISIBLE else View.GONE
    }

    override fun setToolbarVisible(visible: Boolean) {
        mToolbar?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setToolbarTitle(currentIndex: Int, count: Int) {
        supportActionBar?.title = getString(R.string.image_number, currentIndex, count)
    }

    override fun rebindPhotoAtPartial(position: Int) {
        mPagerAdapter?.notifyItemChanged(position)
    }

    override fun rebindPhotoAt(position: Int) {
        mPagerAdapter?.notifyItemChanged(position)
        if (bShowPhotosLine && mAdapterRecycler.getSize() > 1) {
            mAdapterRecycler.notifyItemChanged(position)
        }
    }

    override fun closeOnly() {
        Utils.finishActivityImmediate(this)
    }

    override fun returnInfo(position: Int, parcelNativePtr: Long) {
        setResult(
            RESULT_OK,
            Intent().putExtra(Extra.PTR, parcelNativePtr).putExtra(Extra.POSITION, position)
        )
        Utils.finishActivityImmediate(this)
    }

    override fun returnOnlyPos(position: Int) {
        setResult(
            RESULT_OK,
            Intent().putExtra(Extra.POSITION, position)
        )
        Utils.finishActivityImmediate(this)
    }

    override fun hideMenu(hide: Boolean) {}

    override fun openMenu(open: Boolean) {}

    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val w = window
        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            w.statusBarColor =
                if (colored) getStatusBarColor(this) else getStatusBarNonColored(
                    this
                )
            w.navigationBarColor =
                if (colored) getNavigationBarColor(this) else Color.BLACK
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                w.isNavigationBarContrastEnforced = colored
            }
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(colored = false, invertIcons = false)
            .build()
            .apply(this)
    }

    @get:PhotoSize
    val photoSizeFromPrefs: Int
        get() = Settings.get()
            .main()
            .getPrefDisplayImageSize(DEFAULT_PHOTO_SIZE)

    private inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view), Callback {
        val reload: FloatingActionButton
        private val mPicassoLoadCallback: WeakPicassoLoadCallback
        val photo: TouchImageView
        val tagsPlaceholder: FrameLayout
        val progress: ThorVGLottieView
        var animationDispose = CancelableJob()
        private var mAnimationLoaded = false
        private var mLoadingNow = false
        private var tagCleared = true
        private var currentPhoto: Photo? = null

        fun clearTags() {
            if (tagCleared) {
                return
            }
            tagsPlaceholder.removeAllViews()
            tagsPlaceholder.visibility = View.GONE
            tagCleared = true
        }

        @SuppressLint("InflateParams")
        fun addTags() {
            if (!tagCleared) {
                return
            }
            val currPhoto = currentPhoto ?: return
            if (!currPhoto.showPhotoTags || currPhoto.photoTags.isNullOrEmpty()) {
                return
            }
            val tags = currPhoto.photoTags
            val stateValues = photo.getStateValues() ?: return
            if (stateValues.viewWidth <= 0 || stateValues.viewHeight <= 0 || stateValues.bitmapWidth <= 0 || stateValues.bitmapHeight <= 0) {
                return
            }
            val scaleX: Double = stateValues.matrix[Matrix.MSCALE_X].toDouble()
            val bitmapWidth: Double = stateValues.bitmapWidth.toDouble()
            val preScaledBitmapWidth = scaleX * bitmapWidth / 100.0
            val scaleY: Double = stateValues.matrix[Matrix.MSCALE_Y].toDouble()
            val bitmapHeight: Double = stateValues.bitmapHeight.toDouble()
            val preScaledBitmapHeight = scaleY * bitmapHeight / 100.0
            val transX: Double = stateValues.matrix[Matrix.MTRANS_X].toDouble()
            val transY: Double = stateValues.matrix[Matrix.MTRANS_Y].toDouble()

            var has = false
            for (i in tags.orEmpty()) {
                val leftMargin = transX + i.x * preScaledBitmapWidth
                val topMargin = transY + i.y * preScaledBitmapHeight
                val layoutWidth = ((i.x2 - i.x) * preScaledBitmapWidth)
                val layoutHeight = ((i.y2 - i.y) * preScaledBitmapHeight)

                if (leftMargin > stateValues.viewWidth || leftMargin < -50.0 || topMargin > stateValues.viewHeight || topMargin < -50.0) {
                    continue
                } else {
                    val layoutParams = FrameLayout.LayoutParams(-2, -2)
                    layoutParams.leftMargin = leftMargin.toInt()
                    layoutParams.topMargin = topMargin.toInt()
                    val inflate =
                        layoutInflater.inflate(R.layout.photo_tag_item, null)
                    inflate.findViewById<TextView>(R.id.tv_ph_tag_name)?.let { txt ->
                        txt.visibility =
                            if (i.tagged_name.nonNullNoEmpty()) View.VISIBLE else View.GONE
                        txt.text = i.tagged_name
                        txt.tag = i
                        txt.setOnClickListener {
                            val photoTag = txt.tag as? PhotoTags
                            if (photoTag != null) {
                                PlaceFactory.getOwnerWallPlace(
                                    Settings.get().accounts().current, photoTag.user_id, null
                                ).tryOpenWith(this@PhotoPagerActivity)
                            }
                        }
                    }
                    inflate.findViewById<ImageView>(R.id.iv_ph_tag_border).layoutParams =
                        LinearLayout.LayoutParams(layoutWidth.toInt(), layoutHeight.toInt())
                    tagsPlaceholder.addView(inflate, layoutParams)
                    if (!has) {
                        has = true
                    }
                }
            }
            if (has) {
                tagCleared = false
                tagsPlaceholder.visibility = View.VISIBLE
            }
        }

        fun bindTo(photo_image: Photo) {
            clearTags()
            photo.resetZoom()
            photo.orientationLocked = TouchImageView.OrientationLocked.HORIZONTAL
            val size: Int = photoSizeFromPrefs
            currentPhoto = photo_image
            val url = photo_image.getUrlForSize(size, true)
            reload.setOnClickListener {
                reload.visibility = View.INVISIBLE
                if (url.nonNullNoEmpty()) {
                    loadImage(url)
                } else PicassoInstance.with().cancelRequest(photo)
            }
            if (url.nonNullNoEmpty()) {
                loadImage(url)
            } else {
                PicassoInstance.with().cancelRequest(photo)
                createCustomToast(this@PhotoPagerActivity).showToastError(R.string.empty_url)
            }
        }

        private fun resolveProgressVisibility(forceStop: Boolean) {
            animationDispose.cancel()
            if (mAnimationLoaded && !mLoadingNow && !forceStop) {
                mAnimationLoaded = false
                val k = ObjectAnimator.ofFloat(progress, View.ALPHA, 0.0f).setDuration(1000)
                k.addListener(object : StubAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        progress.clearAnimationDrawable(
                            callSuper = true, clearState = true,
                            cancelTask = true
                        )
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        progress.clearAnimationDrawable(
                            callSuper = true, clearState = true,
                            cancelTask = true
                        )
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }
                })
                k.start()
            } else if (mAnimationLoaded && !mLoadingNow) {
                mAnimationLoaded = false
                progress.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
                progress.visibility = View.GONE
            } else if (mLoadingNow) {
                animationDispose += delayTaskFlow(300).toMain {
                    mAnimationLoaded = true
                    progress.visibility = View.VISIBLE
                    progress.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.loading,
                        intArrayOf(
                            0x000000,
                            CurrentTheme.getColorPrimary(this@PhotoPagerActivity),
                            0x777777,
                            CurrentTheme.getColorSecondary(this@PhotoPagerActivity)
                        )
                    )
                    progress.startAnimation()
                }
            }
        }

        private fun loadImage(url: String) {
            PicassoInstance.with().cancelRequest(photo)
            mLoadingNow = true
            resolveProgressVisibility(true)
            PicassoInstance.with()
                .load(url)
                .into(photo, mPicassoLoadCallback)
        }

        @IdRes
        private fun idOfImageView(): Int {
            return R.id.image_view
        }

        @IdRes
        private fun idOfProgressBar(): Int {
            return R.id.progress_bar
        }

        override fun onSuccess() {
            mLoadingNow = false
            resolveProgressVisibility(false)
            reload.visibility = View.INVISIBLE

            clearTags()
            photo.doOnPreDraw {
                addTags()
            }
        }

        override fun onError(t: Throwable) {
            mLoadingNow = false
            resolveProgressVisibility(true)
            reload.visibility = View.VISIBLE
        }

        init {
            photo = view.findViewById(idOfImageView())
            photo.maxZoom = 8f
            photo.doubleTapScale = 2f
            photo.doubleTapMaxZoom = 4f
            progress = view.findViewById(idOfProgressBar())
            reload = view.findViewById(R.id.goto_button)
            tagsPlaceholder = view.findViewById(R.id.tags_placeholder)
            mPicassoLoadCallback = WeakPicassoLoadCallback(this)
            photo.setOnClickListener { presenter?.firePhotoTap() }
        }
    }

    private inner class Adapter(val mPhotos: List<Photo>) :
        RecyclerView.Adapter<PhotoViewHolder>() {
        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): PhotoViewHolder {
            val ret = PhotoViewHolder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.content_photo_page, container, false)
            )
            ret.photo.setOnLongClickListener {
                if (Settings.get().main().isDownload_photo_tap) {
                    presenter?.fireSaveOnDriveClick(this@PhotoPagerActivity)
                } else if (ret.photo.drawable is Rotatable) {
                    var rot = (ret.photo.drawable as Rotatable).getRotation() + 45
                    if (rot >= 360f) {
                        rot = 0f
                    }
                    if (rot == 0f) {
                        ret.clearTags()
                        ret.addTags()
                    } else {
                        ret.clearTags()
                    }
                    (ret.photo.drawable as Rotatable).rotate(rot)
                    ret.photo.fitImageToView()
                    ret.photo.invalidate()
                }
                true
            }
            ret.photo.setOnStateChangeListener(object : TouchImageView.StateListener {
                override fun onChangeState(
                    imageActionState: TouchImageView.ImageActionState,
                    zoomed: Boolean
                ) {
                    when (imageActionState) {
                        TouchImageView.ImageActionState.NONE -> {
                            ret.addTags()
                        }

                        TouchImageView.ImageActionState.CANT_MOVE, TouchImageView.ImageActionState.MOVE, TouchImageView.ImageActionState.FLING -> {
                            if (zoomed) {
                                ret.clearTags()
                            }
                        }

                        TouchImageView.ImageActionState.ZOOM, TouchImageView.ImageActionState.ANIMATE_ZOOM -> {
                            ret.clearTags()
                        }

                        else -> {}
                    }
                }
            })
            ret.photo.setOnTouchListener { view, event ->
                if (event.pointerCount >= 2 || view is TouchImageView && view.isZoomed) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            container.requestDisallowInterceptTouchEvent(true)
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            container.requestDisallowInterceptTouchEvent(false)
                            true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }
            return ret
        }

        /*
        override fun onViewDetachedFromWindow(holder: PhotoViewHolder) {
            super.onViewDetachedFromWindow(holder)
            PicassoInstance.with().cancelRequest(holder.photo)
        }
         */

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photo = mPhotos[position]
            holder.bindTo(photo)
        }

        override fun getItemCount(): Int {
            return mPhotos.size
        }
    }
}
