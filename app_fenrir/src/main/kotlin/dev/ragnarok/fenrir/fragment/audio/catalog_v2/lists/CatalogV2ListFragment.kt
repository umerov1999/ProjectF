package dev.ragnarok.fenrir.fragment.audio.catalog_v2.lists

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.audio.audioplaylists.AudioPlaylistsFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.audio.audiosrecommendation.AudiosRecommendationFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.CatalogV2SectionFragment
import dev.ragnarok.fenrir.fragment.audio.local.audioslocal.AudiosLocalFragment
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.localserver.audioslocalserver.AudiosLocalServerFragment
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2List
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_CATALOG
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_SERVER_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_PLAYLIST
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_RECOMMENDATIONS
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView

class CatalogV2ListFragment : BaseMvpFragment<CatalogV2ListPresenter, ICatalogV2ListView>(),
    ICatalogV2ListView, MenuProvider {
    private var viewPager: ViewPager2? = null
    private var mAdapter: Adapter? = null
    private var loading: ThorVGLottieView? = null
    private var animLoad: ObjectAnimator? = null
    private var animationDispose = CancelableJob()
    private var mAnimationLoaded = false

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (requireArguments().getString(Extra.ARTIST).nonNullNoEmpty()) {
            menuInflater.inflate(R.menu.menu_share_main, menu)
        } else {
            menuInflater.inflate(R.menu.menu_catalog_v2_list, menu)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isMain() || requireArguments().getString(Extra.ARTIST).nonNullNoEmpty()) {
            requireActivity().addMenuProvider(this, viewLifecycleOwner)
        }
        AppPerms.ignoreBattery(requireActivity())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_share -> {
                presenter?.fireRepost(
                    requireActivity()
                )
                return true
            }

            R.id.action_catalog_v2_find_friends -> {
                PlaceFactory.getCatalogV2AudioCatalogPlace(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    null,
                    null,
                    "https://vk.com/audio?section=recoms_friends"
                ).tryOpenWith(requireActivity())
                return true
            }

            R.id.action_catalog_v2_find_groups -> {
                PlaceFactory.getCatalogV2AudioCatalogPlace(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    null,
                    null,
                    "https://vk.com/audio?section=recommended_groups"
                ).tryOpenWith(requireActivity())
                return true
            }

            R.id.action_catalog_v2_recent -> {
                PlaceFactory.getCatalogV2AudioCatalogPlace(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    null,
                    null,
                    "https://vk.com/audio?section=recent"
                ).tryOpenWith(requireActivity())
                return true
            }

            R.id.action_catalog_v2_artists -> {
                PlaceFactory.getCatalogV2AudioCatalogPlace(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    null,
                    null,
                    "https://vk.com/audio?section=artist_recoms"
                ).tryOpenWith(requireActivity())
                return true
            }

            else -> return false
        }
    }

    internal fun isMain(): Boolean {
        return requireArguments().getString(Extra.ARTIST)
            .isNullOrEmpty() && requireArguments().getString(Extra.QUERY)
            .isNullOrEmpty() && requireArguments().getString(Extra.URL)
            .isNullOrEmpty() && requireArguments().getLong(Extra.ACCOUNT_ID) == requireArguments().getLong(
            Extra.OWNER_ID
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_catalog_v2_list, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        viewPager = root.findViewById(R.id.fragment_audios_pager)
        mAdapter = Adapter(this)
        viewPager?.adapter = mAdapter
        viewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        TabLayoutMediator(
            root.findViewById(R.id.fragment_audios_tabs),
            viewPager ?: return null
        ) { tab, position ->
            tab.text = mAdapter?.getTitle(position)
        }.attach()
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
        return root
    }

    override fun displayData(sections: List<CatalogV2List.CatalogV2ListItem>) {
        if (mAdapter != null) {
            mAdapter?.pFragments = sections
            mAdapter?.notifyDataSetChanged()
        }
    }

    override fun setSection(position: Int) {
        viewPager?.setCurrentItem(position, false)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        animationDispose.cancel()
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

    override fun getPresenterFactory(saveInstanceState: Bundle?) = CatalogV2ListPresenter(
        requireArguments().getLong(Extra.ACCOUNT_ID),
        requireArguments().getLong(Extra.OWNER_ID),
        requireArguments().getString(Extra.ARTIST),
        requireArguments().getString(Extra.QUERY),
        requireArguments().getString(Extra.URL),
        requireActivity(),
        saveInstanceState
    )

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.audio_catalog)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    inner class Adapter(fragmentActivity: Fragment) :
        FragmentStateAdapter(fragmentActivity) {
        var pFragments: List<CatalogV2List.CatalogV2ListItem> =
            emptyList()

        fun getTitle(position: Int): String? {
            return pFragments[position].title
        }

        override fun createFragment(position: Int): Fragment {
            if (pFragments[position].customType == TYPE_CATALOG) {
                return CatalogV2SectionFragment.newInstance(
                    Settings.get().accounts().current, pFragments[position].id.orEmpty(),
                    isHideToolbar = true, supportSearch = isMain()
                )
            } else when (pFragments[position].customType) {
                TYPE_LOCAL_AUDIO -> return AudiosLocalFragment.newInstance(
                    requireArguments().getLong(Extra.ACCOUNT_ID)
                )

                TYPE_LOCAL_SERVER_AUDIO -> return AudiosLocalServerFragment.newInstance(
                    requireArguments().getLong(Extra.ACCOUNT_ID)
                )

                TYPE_AUDIO -> {
                    val args = AudiosFragment.buildArgs(
                        requireArguments().getLong(Extra.ACCOUNT_ID),
                        requireArguments().getLong(Extra.OWNER_ID),
                        null,
                        null
                    )
                    args.putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                    return AudiosFragment.newInstance(args)
                }

                TYPE_PLAYLIST -> {
                    val fragment = AudioPlaylistsFragment.newInstance(
                        requireArguments().getLong(Extra.ACCOUNT_ID),
                        requireArguments().getLong(Extra.OWNER_ID)
                    )
                    fragment.requireArguments()
                        .putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                    return fragment
                }

                TYPE_RECOMMENDATIONS -> {
                    val fragment = AudiosRecommendationFragment.newInstance(
                        requireArguments().getLong(Extra.ACCOUNT_ID),
                        requireArguments().getLong(Extra.OWNER_ID), false, 0
                    )
                    fragment.requireArguments()
                        .putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                    return fragment
                }
            }
            throw UnsupportedOperationException()
        }

        override fun getItemCount(): Int {
            return pFragments.size
        }
    }

    companion object {
        fun newInstance(
            accountId: Long,
            ownerId: Long,
        ): CatalogV2ListFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putString(Extra.ARTIST, null)
            val fragment = CatalogV2ListFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            artistId: String?,
            query: String?,
            url: String?
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putString(Extra.ARTIST, artistId)
            args.putString(Extra.QUERY, query)
            args.putString(Extra.URL, url)
            return args
        }

        fun newInstance(args: Bundle?): CatalogV2ListFragment {
            val fragment = CatalogV2ListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
