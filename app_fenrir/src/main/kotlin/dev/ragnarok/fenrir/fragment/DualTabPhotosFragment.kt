package dev.ragnarok.fenrir.fragment

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.filemanagerselect.FileManagerSelectFragment
import dev.ragnarok.fenrir.fragment.localimagealbums.LocalImageAlbumsFragment
import dev.ragnarok.fenrir.fragment.localphotos.LocalPhotosFragment
import dev.ragnarok.fenrir.fragment.localvideos.LocalVideosFragment
import dev.ragnarok.fenrir.fragment.vkphotoalbums.VKPhotoAlbumsFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.model.selection.*
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform

class DualTabPhotosFragment : BaseFragment(), BackPressCallback {
    private lateinit var mSources: Sources
    private var mPagerAdapter: Adapter? = null
    private var mCurrentTab = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSources = (requireArguments().getParcelableCompat(Extra.SOURCES) ?: return)
        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt("mCurrentTab")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mCurrentTab", mCurrentTab)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.activity_dual_tab_photos, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val viewPager: ViewPager2 = root.findViewById(R.id.view_pager)
        mPagerAdapter = Adapter(this, mSources)
        viewPager.adapter = mPagerAdapter
        viewPager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mCurrentTab = position
            }
        })
        TabLayoutMediator(
            root.findViewById(R.id.tablayout),
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = mPagerAdapter?.getPageTitle(position)
        }.attach()
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.multiply_poll)
            actionBar.subtitle = null
        }
    }

    override fun onBackPressed(): Boolean {
        if (mPagerAdapter != null) {
            val fragment = mPagerAdapter?.findByPosition(mCurrentTab)
            return fragment !is BackPressCallback || (fragment as BackPressCallback).onBackPressed()
        }
        return true
    }

    private inner class Adapter(fm: Fragment, private val mSources: Sources) :
        FragmentStateAdapter(fm) {
        fun getPageTitle(position: Int): CharSequence {
            when (mSources[position].type) {
                Types.LOCAL_PHOTOS -> return getString(R.string.local_photos_tab_title)
                Types.LOCAL_GALLERY -> return getString(R.string.local_gallery_tab_title)
                Types.VIDEOS -> return getString(R.string.videos)
                Types.VK_PHOTOS -> return getString(R.string.vk_photos_tab_title)
                Types.FILES -> return getString(R.string.files_tab_title)
            }
            throw UnsupportedOperationException()
        }

        @Suppress("DEPRECATION")
        override fun createFragment(position: Int): Fragment {
            val source = mSources[position]
            if (source is LocalPhotosSelectableSource) {
                val args = Bundle()
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true)
                val fragment = LocalImageAlbumsFragment()
                fragment.arguments = args
                return fragment
            }
            if (source is LocalGallerySelectableSource) {
                return LocalPhotosFragment.newInstance(10, null, true)
            }
            if (source is LocalVideosSelectableSource) {
                val args = Bundle()
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true)
                val fragment = LocalVideosFragment.newInstance()
                fragment.arguments = args
                return fragment
            }
            if (source is VKPhotosSelectableSource) {
                return VKPhotoAlbumsFragment.newInstance(
                    source.accountId,
                    source.ownerId,
                    null,
                    null,
                    true
                )
            }
            if (source is FileManagerSelectableSource) {
                val args = Bundle()
                args.putString(Extra.PATH, Environment.getExternalStorageDirectory().absolutePath)
                args.putBoolean(Extra.HIDE_TITLE, true)
                val fileManagerFragment = FileManagerSelectFragment()
                fileManagerFragment.arguments = args
                return fileManagerFragment
            }
            throw UnsupportedOperationException()
        }

        override fun getItemCount(): Int {
            return mSources.count()
        }
    }

    companion object {
        fun newInstance(sources: Sources?): DualTabPhotosFragment {
            val args = Bundle()
            args.putParcelable(Extra.SOURCES, sources)
            val fragment = DualTabPhotosFragment()
            fragment.arguments = args
            return fragment
        }
    }
}