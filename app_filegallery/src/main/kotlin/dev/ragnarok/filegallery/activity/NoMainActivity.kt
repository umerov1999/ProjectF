package dev.ragnarok.filegallery.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.listener.BackPressCallback
import dev.ragnarok.filegallery.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.filegallery.settings.theme.ThemesController.currentStyle
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.Utils.hasVanillaIceCreamTarget

abstract class NoMainActivity : AppCompatActivity() {
    private var mToolbar: Toolbar? = null
    private val mBackStackListener =
        FragmentManager.OnBackStackChangedListener { resolveToolbarNavigationIcon() }

    @get:LayoutRes
    protected open val noMainContentView: Int
        get() = R.layout.activity_no_main

    @get:IdRes
    protected open val noMainContainerViewId: Int
        get() = R.id.fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        setContentView(noMainContentView)
        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            val w = window
            w.statusBarColor = getStatusBarColor(this)
            w.navigationBarColor = getNavigationBarColor(this)
        }
        supportFragmentManager.addOnBackStackChangedListener(mBackStackListener)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = supportFragmentManager
                val front = fm.findFragmentById(noMainContainerViewId)
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
