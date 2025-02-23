package dev.ragnarok.filegallery.activity.photopager

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Rotatable
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.StubAnimatorListener
import dev.ragnarok.filegallery.activity.ActivityFeatures
import dev.ragnarok.filegallery.activity.BaseMvpActivity
import dev.ragnarok.filegallery.activity.VideoPlayerActivity
import dev.ragnarok.filegallery.activity.slidr.Slidr
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.model.SlidrListener
import dev.ragnarok.filegallery.activity.slidr.model.SlidrPosition
import dev.ragnarok.filegallery.fragment.AudioPlayerFragment
import dev.ragnarok.filegallery.fragment.base.horizontal.ImageListAdapter
import dev.ragnarok.filegallery.listener.AppStyleable
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.place.Place
import dev.ragnarok.filegallery.place.PlaceFactory
import dev.ragnarok.filegallery.place.PlaceProvider
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.DownloadWorkUtils
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.filegallery.view.TouchImageView
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieView
import dev.ragnarok.filegallery.view.pager.WeakPicassoLoadCallback

class PhotoPagerActivity : BaseMvpActivity<PhotoPagerPresenter, IPhotoPagerView>(), IPhotoPagerView,
    PlaceProvider, AppStyleable, MenuProvider {
    companion object {
        private const val EXTRA_PHOTOS = "photos"
        private const val ACTION_OPEN =
            "dev.ragnarok.filegallery.activity.photopager.PhotoPagerActivity"

        fun buildArgsForAlbum(
            photos: Long,
            position: Int,
            invert: Boolean
        ): Bundle {
            val args = Bundle()
            args.putInt(Extra.INDEX, position)
            args.putBoolean(Extra.INVERT, invert)
            args.putLong(
                EXTRA_PHOTOS,
                photos
            )
            return args
        }

        var mLastBackPressedTime: Long = 0

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
    }

    private var mViewPager: ViewPager2? = null
    private var mContentRoot: RelativeLayout? = null
    private var mLoadingProgressBar: ThorVGLottieView? = null
    private var mLoadingProgressBarDispose = CancelableJob()
    private var mLoadingProgressBarLoaded = false
    private var mToolbar: Toolbar? = null
    private var mPreviewsRecycler: RecyclerView? = null
    private var mPagerAdapter: Adapter? = null
    private val bShowPhotosLine = Settings.get().main().isShow_photos_line
    private val mAdapterRecycler = ImageListAdapter()
    private var isLocalPhoto = false

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
        mPreviewsRecycler = findViewById(R.id.previews_photos)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mViewPager = findViewById(R.id.view_pager)
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )

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
        val args: Bundle = place.prepareArguments()
        when (place.type) {
            Place.AUDIO_PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                AudioPlayerFragment().show(supportFragmentManager, "audio_player")
            }

            Place.VIDEO_PLAYER -> {
                val videoActivity = VideoPlayerActivity.newInstance(this, args)
                place.launchActivityForResult(
                    this,
                    videoActivity
                )
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.save_on_drive).isVisible = !isLocalPhoto
        if (!isLocalPhoto) {
            menu.findItem(R.id.start_select_mode).isVisible = false
            menu.findItem(R.id.end_select_mode).isVisible = false
            menu.findItem(R.id.send_action).isVisible = false
        } else {
            menu.findItem(R.id.start_select_mode).isVisible = !Utils.shouldSelectPhoto
            menu.findItem(R.id.end_select_mode).isVisible = Utils.shouldSelectPhoto
            menu.findItem(R.id.send_action).isVisible =
                Utils.shouldSelectPhoto && Utils.listSelected.isNotEmpty()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.photo_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.save_on_drive -> {
                presenter?.fireSaveOnDriveClick()
                return true
            }

            R.id.detect_qr -> {
                presenter?.fireDetectQRClick(this)
                return true
            }

            R.id.start_select_mode -> {
                Utils.shouldSelectPhoto = true
                return true
            }

            R.id.end_select_mode -> {
                Utils.shouldSelectPhoto = false
                Utils.listSelected.clear()
                mPagerAdapter?.notifyDataSetChanged()
                return true
            }

            R.id.send_action -> {
                val intent_send = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent_send.type = "image/*"
                val listImage: ArrayList<Uri> = ArrayList(Utils.listSelected.size)
                for (i in Utils.listSelected) {
                    listImage.add(
                        FileProvider.getUriForFile(
                            this,
                            Constants.FILE_PROVIDER_AUTHORITY,
                            i.toUri().toFile()
                        )
                    )
                }
                intent_send.putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM, listImage
                ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent_send)
                return true
            }
        }
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): PhotoPagerPresenter {
        when (requireArguments().getInt(Extra.PLACE_TYPE)) {
            Place.PHOTO_LOCAL_SERVER -> {
                var source: Long = requireArguments().getLong(EXTRA_PHOTOS)
                requireArguments().putLong(EXTRA_PHOTOS, 0)
                if (!Utils.isParcelNativeRegistered(source)) {
                    source = 0
                }
                Utils.unregisterParcelNative(source)
                return PhotoAlbumPagerPresenter(
                    requireArguments().getInt(Extra.INDEX),
                    source,
                    requireArguments().getBoolean(Extra.INVERT)
                )
            }

            Place.PHOTO_LOCAL -> {
                var source: Long = requireArguments().getLong(EXTRA_PHOTOS)
                requireArguments().putLong(EXTRA_PHOTOS, 0)
                if (!Utils.isParcelNativeRegistered(source)) {
                    source = 0
                }
                Utils.unregisterParcelNative(source)
                return TmpGalleryPagerPresenter(
                    source,
                    requireArguments().getInt(Extra.INDEX)
                )
            }
        }
        throw UnsupportedOperationException()
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

    override fun setToolbarTitle(currentIndex: Int, count: Int) {
        supportActionBar?.title = getString(R.string.image_number, currentIndex, count)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoadingProgressBarDispose.cancel()
    }

    override fun displayPhotoListLoading(loading: Boolean) {
        mLoadingProgressBarDispose.cancel()
        if (loading) {
            mLoadingProgressBarDispose.set(delayTaskFlow(300).toMain {
                mLoadingProgressBarLoaded = true
                mLoadingProgressBar?.visibility = View.VISIBLE
                mLoadingProgressBar?.fromRes(
                    R.raw.loading,
                    intArrayOf(
                        0x000000,
                        Color.WHITE,
                        0x777777,
                        Color.WHITE
                    )
                )
                mLoadingProgressBar?.startAnimation()
            })
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
        mPreviewsRecycler?.visibility = if (visible && bShowPhotosLine) View.VISIBLE else View.GONE
    }

    override fun setToolbarVisible(visible: Boolean) {
        mToolbar?.visibility = if (visible) View.VISIBLE else View.GONE
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

    override fun returnFileInfo(path: String) {
        setResult(
            RESULT_OK,
            Intent().putExtra(Extra.PATH, path)
        )
        Utils.finishActivityImmediate(this)
    }

    override fun displayVideo(video: Video) {
        PlaceFactory.getInternalPlayerPlace(video).tryOpenWith(this)
    }

    override fun setupOptionMenu(isLocal: Boolean) {
        isLocalPhoto = isLocal
        this.invalidateOptionsMenu()
    }

    override fun downloadPhoto(url: String, dir: String, file: String) {
        DownloadWorkUtils.doDownloadPhoto(this, url, dir, file)
    }

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val w = window
        if (!hasVanillaIceCreamTarget()) {
            w.statusBarColor =
                if (colored) getStatusBarColor(this) else getStatusBarNonColored(
                    this
                )
            w.navigationBarColor =
                if (colored) getNavigationBarColor(this) else Color.BLACK
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setBarsColored(colored = false, invertIcons = false)
            .build()
            .apply(this)
    }

    private inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view), Callback {
        val reload: FloatingActionButton
        private val mPicassoLoadCallback: WeakPicassoLoadCallback
        val photo: TouchImageView
        val selected: View
        val progress: ThorVGLottieView
        var animationDispose = CancelableJob()
        private var mAnimationLoaded = false
        private var mLoadingNow = false
        fun bindTo(photo_image: Photo) {
            selected.visibility =
                if (Utils.shouldSelectPhoto && photo_image.inLocal() && Utils.listSelected.contains(
                        photo_image.photo_url
                    )
                ) View.VISIBLE else View.GONE
            photo.setOnLongClickListener {
                if (Utils.shouldSelectPhoto) {
                    if (photo_image.inLocal()) {
                        if (Utils.listSelected.contains(photo_image.photo_url)) {
                            Utils.listSelected.remove(photo_image.photo_url)
                            selected.visibility = View.GONE
                        } else {
                            photo_image.photo_url?.let { it1 -> Utils.listSelected.add(it1) }
                            selected.visibility = View.VISIBLE
                        }
                    }
                    true
                } else {
                    val o = presenter?.fireSaveOnDriveClick()
                    if (o == true && photo.drawable is Rotatable) {
                        var rot = (photo.drawable as Rotatable).getRotation() + 45f
                        if (rot >= 360f) {
                            rot = 0f
                        }
                        (photo.drawable as Rotatable).rotate(rot)
                        photo.fitImageToView()
                        photo.invalidate()
                    }
                    true
                }
            }
            photo.resetZoom()
            reload.setOnClickListener {
                reload.visibility = View.INVISIBLE
                loadImage(photo_image)
            }
            loadImage(photo_image)
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
                animationDispose.set(delayTaskFlow(300).toMain {
                    mAnimationLoaded = true
                    progress.visibility = View.VISIBLE
                    progress.fromRes(
                        R.raw.loading,
                        intArrayOf(
                            0x000000,
                            CurrentTheme.getColorPrimary(this@PhotoPagerActivity),
                            0x777777,
                            CurrentTheme.getColorSecondary(this@PhotoPagerActivity)
                        )
                    )
                    progress.startAnimation()
                })
            }
        }

        private fun loadImage(image: Photo) {
            if (image.photo_url.isNullOrEmpty()) {
                PicassoInstance.with().cancelRequest(photo)
                createCustomToast(
                    this@PhotoPagerActivity,
                    mViewPager
                )?.showToastError(R.string.empty_url)
                return
            }
            mLoadingNow = true
            resolveProgressVisibility(true)
            PicassoInstance.with().cancelRequest(photo)
            if (!image.isAnimation) {
                PicassoInstance.with()
                    .load(image.photo_url)
                    .into(photo, mPicassoLoadCallback)
            } else {
                image.photo_url?.toUri()?.toFile()?.let {
                    photo.fromAnimationFile(it, true)
                }
                mLoadingNow = false
                resolveProgressVisibility(true)
            }
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
        }

        override fun onError(t: Throwable) {
            mLoadingNow = false
            resolveProgressVisibility(true)
            reload.visibility = View.VISIBLE
        }

        init {
            photo = view.findViewById(idOfImageView())
            selected = view.findViewById(R.id.selected_view)
            photo.maxZoom = 8f
            photo.doubleTapScale = 2f
            photo.doubleTapMaxZoom = 4f
            progress = view.findViewById(idOfProgressBar())
            reload = view.findViewById(R.id.goto_button)
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
            ret.photo.setOnTouchListener { view, event ->
                if (event.pointerCount >= 2 || view is TouchImageView && view.isZoomed) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            container.requestDisallowInterceptTouchEvent(true)
                            return@setOnTouchListener false
                        }

                        MotionEvent.ACTION_UP -> {
                            container.requestDisallowInterceptTouchEvent(false)
                            return@setOnTouchListener true
                        }
                    }
                }
                true
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
