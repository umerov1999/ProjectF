package dev.ragnarok.fenrir.activity.gifpager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.absdocumentpreview.AbsDocumentPreviewActivity
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView

class GifPagerActivity : AbsDocumentPreviewActivity<GifPagerPresenter, IGifPagerView>(),
    IGifPagerView, PlaceProvider, AppStyleable {
    private var mViewPager: ViewPager2? = null
    private var mToolbar: Toolbar? = null
    private var mButtonsRoot: View? = null
    private var mButtonAddOrDelete: CircleCounterButton? = null
    private var mFullscreen = false

    @LayoutRes
    override fun getNoMainContentView(): Int {
        return R.layout.fragment_gif_pager
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreen = savedInstanceState?.getBoolean("mFullscreen") ?: false
        mToolbar = findViewById(R.id.toolbar)
        val mContentRoot = findViewById<RelativeLayout>(R.id.gif_pager_root)
        setSupportActionBar(mToolbar)
        mButtonsRoot = findViewById(R.id.buttons)
        mButtonAddOrDelete = findViewById(R.id.button_add_or_delete)
        mButtonAddOrDelete?.setOnClickListener {
            presenter?.fireAddDeleteButtonClick()
        }
        mViewPager = findViewById(R.id.view_pager)
        mViewPager?.offscreenPageLimit = 1
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        mViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                presenter?.selectPage(
                    position
                )
            }
        })
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
                        if (Utils.hasOreo()) {
                            mContentRoot?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                        } else {
                            mContentRoot?.setBackgroundColor(
                                Color.argb(
                                    (tmp * 255).toInt(),
                                    0,
                                    0,
                                    0
                                )
                            )
                        }
                        mButtonsRoot?.alpha = tmp
                        mToolbar?.alpha = tmp
                        mViewPager?.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {

                    }

                    override fun onSlideClosed(): Boolean {
                        finish()
                        overridePendingTransition(0, 0)
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

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        if (Utils.hasMarshmallow()) {
            var flags = window.decorView.systemUiVisibility
            flags = if (invertIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Utils.hasOreo()) {
            var flags = window.decorView.systemUiVisibility
            if (invertIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                w.decorView.systemUiVisibility = flags
                w.navigationBarColor = Color.WHITE
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<GifPagerPresenter> {
        return object : IPresenterFactory<GifPagerPresenter> {
            override fun create(): GifPagerPresenter {
                val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
                val index = requireArguments().getInt(Extra.INDEX)
                val documents: ArrayList<Document> =
                    requireArguments().getParcelableArrayListCompat(Extra.DOCS)!!
                return GifPagerPresenter(aid, documents, index, saveInstanceState)
            }
        }
    }

    override fun displayData(mDocuments: List<Document>, selectedIndex: Int) {
        if (mViewPager != null) {
            val adapter = Adapter(mDocuments)
            mViewPager?.adapter = adapter
            mViewPager?.setCurrentItem(selectedIndex, false)
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
            ret.mGifView.setOnTouchListener { view: View, event: MotionEvent ->
                if (event.pointerCount >= 2 || view.canScrollHorizontally(1) && view.canScrollHorizontally(
                        -1
                    )
                ) {
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
            holder.mGifView.fromNet(
                data[position].ownerId.toString() + "_" + data[position].id.toString(),
                data[position].videoPreview?.src,
                data[position].getPreviewWithSize(PhotoSize.W, false), Utils.createOkHttp(5, true)
            )
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    companion object {
        const val ACTION_OPEN =
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
            args.getLong(Extra.ACCOUNT_ID, aid)
            args.putInt(Extra.INDEX, index)
            args.putParcelableArrayList(Extra.DOCS, documents)
            return args
        }
    }
}