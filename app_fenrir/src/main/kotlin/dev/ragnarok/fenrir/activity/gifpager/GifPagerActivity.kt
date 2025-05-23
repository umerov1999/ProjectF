package dev.ragnarok.fenrir.activity.gifpager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.docs.absdocumentpreview.AbsDocumentPreviewActivity
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView

class GifPagerActivity : AbsDocumentPreviewActivity<GifPagerPresenter, IGifPagerView>(),
    IGifPagerView, PlaceProvider, AppStyleable {
    private var mViewPager: ViewPager2? = null
    private var mToolbar: Toolbar? = null
    private var mButtonsRoot: View? = null
    private var mButtonAddOrDelete: CircleCounterButton? = null
    private var mFullscreen = false

    @get:LayoutRes
    override val noMainContentView: Int
        get() = R.layout.activity_gif_pager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreen = savedInstanceState?.getBoolean("mFullscreen") == true
        mToolbar = findViewById(R.id.toolbar)
        val mContentRoot = findViewById<RelativeLayout>(R.id.gif_pager_root)
        setSupportActionBar(mToolbar)
        mButtonsRoot = findViewById(R.id.buttons)
        mButtonAddOrDelete = findViewById(R.id.button_add_or_delete)
        mButtonAddOrDelete?.setOnClickListener {
            presenter?.fireAddDeleteButtonClick()
        }
        mViewPager = findViewById(R.id.view_pager)
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        attach(
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
                        mViewPager?.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {

                    }

                    override fun onSlideClosed(): Boolean {
                        Utils.finishActivityImmediate(this@GifPagerActivity)
                        return true
                    }

                }).build()
        )

        findViewById<View>(R.id.button_share).setOnClickListener {
            presenter?.fireShareButtonClick()
        }
        findViewById<View>(R.id.button_download).setOnClickListener {
            presenter?.fireDownloadButtonClick(
                this,
                mContentRoot
            )
        }
        resolveFullscreenViews()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("mFullscreen", mFullscreen)
    }

    override val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter {
            fireWritePermissionResolved(
                this@GifPagerActivity,
                this@GifPagerActivity.findViewById(R.id.gif_pager_root)
            )
        }
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                AudioPlayerFragment.newInstance(args).show(supportFragmentManager, "audio_player")
            }

            else -> Utils.openPlaceWithSwipebleActivity(this, place)
        }
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

    internal fun toggleFullscreen() {
        mFullscreen = !mFullscreen
        resolveFullscreenViews()
    }

    private fun resolveFullscreenViews() {
        mToolbar?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
        mButtonsRoot?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): GifPagerPresenter {
        val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
        val index = requireArguments().getInt(Extra.INDEX)
        val documents: ArrayList<Document> =
            if (FenrirNative.isNativeLoaded && Settings.get()
                    .main().isNative_parcel_docs
            ) {
                var pointer = requireArguments().getLong(Extra.DOCS)
                requireArguments().putLong(Extra.DOCS, 0)
                if (!Utils.isParcelNativeRegistered(pointer)) {
                    pointer = 0
                }
                Utils.unregisterParcelNative(pointer)
                ParcelNative.loadParcelableArrayList(
                    pointer,
                    Document.NativeCreator,
                    ParcelFlags.EMPTY_LIST
                )!!
            } else {
                requireArguments().getParcelableArrayListCompat(Extra.DOCS)!!
            }
        return GifPagerPresenter(aid, documents, index, saveInstanceState)
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            presenter?.selectPage(
                position
            )
        }
    }

    override fun displayData(mDocuments: List<Document>, selectedIndex: Int) {
        if (mViewPager != null) {
            mViewPager?.unregisterOnPageChangeCallback(pageChangeListener)
            val adapter = Adapter(mDocuments)
            mViewPager?.adapter = adapter
            mViewPager?.setCurrentItem(selectedIndex, false)
            mViewPager?.registerOnPageChangeCallback(pageChangeListener)
        }
    }

    override fun setupAddRemoveButton(addEnable: Boolean) {
        mButtonAddOrDelete?.setIcon(if (addEnable) R.drawable.plus else R.drawable.ic_outline_delete)
    }

    inner class Holder internal constructor(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val mGifView: TouchImageView = rootView.findViewById(R.id.gif_view)

        init {
            mGifView.setOnClickListener { toggleFullscreen() }
        }
    }

    override fun toolbarTitle(@StringRes titleRes: Int, vararg params: Any?) {
        supportActionBar?.title = getString(titleRes, *params)
    }

    override fun toolbarSubtitle(@StringRes titleRes: Int, vararg params: Any?) {
        supportActionBar?.subtitle = getString(titleRes, *params)
    }

    private inner class Adapter(private var data: List<Document>) :
        RecyclerView.Adapter<Holder>() {
        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): Holder {
            val ret = Holder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.content_gif_page, container, false)
            )
            ret.mGifView.orientationLocked = TouchImageView.OrientationLocked.HORIZONTAL
            ret.mGifView.setOnTouchListener { view, event ->
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

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.mGifView.fromAnimationNet(
                data[position].ownerId.toString() + "_" + data[position].id.toString(),
                data[position].videoPreview?.src,
                data[position].getPreviewWithSize(PhotoSize.W, false),
                Utils.createOkHttp(Constants.GIF_TIMEOUT, true),
                true
            )
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    companion object {
        private const val ACTION_OPEN =
            "dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity"

        fun newInstance(context: Context, args: Bundle?): Intent {
            val ph = Intent(context, GifPagerActivity::class.java)
            val targetArgs = Bundle()
            targetArgs.putAll(args)
            ph.action = ACTION_OPEN
            ph.putExtras(targetArgs)
            return ph
        }

        fun buildArgs(aid: Long, documents: ArrayList<Document>, index: Int): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putInt(Extra.INDEX, index)
            if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_docs) {
                val pointer = ParcelNative.createParcelableList(documents, ParcelFlags.NULL_LIST)
                Utils.registerParcelNative(pointer)
                args.putLong(
                    Extra.DOCS,
                    pointer
                )
            } else {
                args.putParcelableArrayList(Extra.DOCS, documents)
            }
            return args
        }
    }
}
