package dev.ragnarok.fenrir.activity

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.zoomhelper.ZoomHelper.Companion.getInstance

abstract class NoMainActivity : AppCompatActivity() {
    private var mToolbar: Toolbar? = null
    private val mBackStackListener =
        FragmentManager.OnBackStackChangedListener { resolveToolbarNavigationIcon() }
    private var isZoomPhoto = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        isZoomPhoto = Settings.get().other().isDo_zoom_photo
        setContentView(getNoMainContentView())
        val w = window
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.statusBarColor = CurrentTheme.getStatusBarColor(this)
        w.navigationBarColor = CurrentTheme.getNavigationBarColor(this)
        supportFragmentManager.addOnBackStackChangedListener(mBackStackListener)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = supportFragmentManager
                val front = fm.findFragmentById(
                    getMainContainerViewId()
                )
                if (front is BackPressCallback) {
                    if (!(front as BackPressCallback).onBackPressed()) {
                        return
                    }
                }
                if (fm.backStackEntryCount <= 1) {
                    supportFinishAfterTransition()
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return if (!isZoomPhoto) {
            super.dispatchTouchEvent(ev)
        } else getInstance()?.dispatchTouchEvent(ev, this) == true || super.dispatchTouchEvent(ev)
    }

    @LayoutRes
    protected open fun getNoMainContentView(): Int {
        return R.layout.activity_no_main
    }

    @IdRes
    protected open fun getMainContainerViewId(): Int {
        return R.id.fragment
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        mToolbar = toolbar
        resolveToolbarNavigationIcon()
    }

    private fun resolveToolbarNavigationIcon() {
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 1) {
            mToolbar?.setNavigationIcon(R.drawable.arrow_left)
        } else {
            mToolbar?.setNavigationIcon(R.drawable.close)
        }
        mToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(mBackStackListener)
        super.onDestroy()
    }
}