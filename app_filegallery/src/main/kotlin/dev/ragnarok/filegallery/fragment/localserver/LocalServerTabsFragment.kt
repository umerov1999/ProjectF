package dev.ragnarok.filegallery.fragment.localserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.ActivityFeatures
import dev.ragnarok.filegallery.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.filegallery.fragment.base.BaseFragment
import dev.ragnarok.filegallery.fragment.localserver.audioslocalserver.AudiosLocalServerFragment
import dev.ragnarok.filegallery.fragment.localserver.filemanagerremote.FileManagerRemoteFragment
import dev.ragnarok.filegallery.fragment.localserver.photoslocalserver.PhotosLocalServerFragment
import dev.ragnarok.filegallery.fragment.localserver.videoslocalserver.VideosLocalServerFragment
import dev.ragnarok.filegallery.listener.BackPressCallback
import dev.ragnarok.filegallery.listener.OnSectionResumeCallback
import dev.ragnarok.filegallery.model.SectionItem
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.Utils

class LocalServerTabsFragment : BaseFragment(), BackPressCallback {
    private var mPagerAdapter: Adapter? = null
    private var mCurrentTab = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(
            R.layout.fragment_local_media_server_tabs,
            container,
            false
        ) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt("mCurrentTab")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mCurrentTab", mCurrentTab)
    }

    override fun onBackPressed(): Boolean {
        if (mPagerAdapter != null) {
            val fragment = mPagerAdapter?.findByPosition(mCurrentTab)
            return fragment !is BackPressCallback || (fragment as BackPressCallback).onBackPressed()
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewPager: ViewPager2 = view.findViewById(R.id.fragment_tabs_pager)
        viewPager.offscreenPageLimit = 1
        viewPager.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().getViewpager_page_transform()
            )
        )
        mPagerAdapter = Adapter(this)
        setupViewPager(viewPager, mPagerAdapter ?: return)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mCurrentTab = position
            }
        })
        TabLayoutMediator(
            view.findViewById(R.id.fragment_local_media_server_tabs),
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            when ((mPagerAdapter ?: return@TabLayoutMediator).tFragments[position]) {
                LOCAL_SERVER_AUDIO -> tab.text =
                    getString(R.string.local_server_audio)
                LOCAL_SERVER_PHOTO -> tab.text =
                    getString(R.string.local_server_photo)
                LOCAL_SERVER_VIDEO -> tab.text =
                    getString(R.string.local_server_video)
                LOCAL_SERVER_FS -> tab.text =
                    getString(R.string.files)
            }
        }.attach()
    }

    internal fun dCreateFragment(option_menu: Int): Fragment {
        return when (option_menu) {
            LOCAL_SERVER_AUDIO -> {
                AudiosLocalServerFragment()
            }
            LOCAL_SERVER_PHOTO -> PhotosLocalServerFragment()
            LOCAL_SERVER_VIDEO -> {
                VideosLocalServerFragment()
            }
            LOCAL_SERVER_FS -> {
                FileManagerRemoteFragment()
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun setupViewPager(viewPager: ViewPager2, adapter: Adapter) {
        adapter.addFragment(LOCAL_SERVER_AUDIO)
        adapter.addFragment(LOCAL_SERVER_PHOTO)
        adapter.addFragment(LOCAL_SERVER_VIDEO)
        adapter.addFragment(LOCAL_SERVER_FS)
        viewPager.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.local_media_server)
        actionBar?.subtitle = null
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(SectionItem.LOCAL_SERVER)
        }
        ActivityFeatures.Builder()
            .begin()
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    private inner class Adapter(fragmentActivity: Fragment) :
        FragmentStateAdapter(fragmentActivity) {
        val tFragments: MutableList<Int> = ArrayList()
        fun addFragment(fragment: Int) {
            tFragments.add(fragment)
        }

        override fun createFragment(position: Int): Fragment {
            return dCreateFragment(tFragments[position])
        }

        override fun getItemCount(): Int {
            return tFragments.size
        }
    }

    companion object {
        const val LOCAL_SERVER_AUDIO = 0
        const val LOCAL_SERVER_PHOTO = 1
        const val LOCAL_SERVER_VIDEO = 2
        const val LOCAL_SERVER_FS = 3
    }
}