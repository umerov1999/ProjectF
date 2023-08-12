package dev.ragnarok.filegallery.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.maxr1998.modernpreferences.AbsPreferencesFragment
import de.maxr1998.modernpreferences.PreferenceScreen
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.helpers.DEFAULT_RES_ID
import de.maxr1998.modernpreferences.helpers.onCheckedBeforeChange
import de.maxr1998.modernpreferences.helpers.onClick
import de.maxr1998.modernpreferences.helpers.pref
import de.maxr1998.modernpreferences.helpers.screen
import de.maxr1998.modernpreferences.helpers.switch
import de.maxr1998.modernpreferences.preferences.TwoStatePreference
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.ActivityFeatures
import dev.ragnarok.filegallery.activity.ActivityUtils
import dev.ragnarok.filegallery.activity.CreatePinActivity
import dev.ragnarok.filegallery.fragment.pin.createpin.CreatePinFragment
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.listener.BackPressCallback
import dev.ragnarok.filegallery.listener.CanBackPressedCallback
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.SecuritySettings
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.trimmedNonNullNoEmpty
import dev.ragnarok.filegallery.util.rxutils.RxUtils
import dev.ragnarok.filegallery.view.MySearchView
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit


class SecurityPreferencesFragment : AbsPreferencesFragment(),
    PreferencesAdapter.OnScreenChangeListener,
    BackPressCallback, CanBackPressedCallback {
    private var preferencesView: RecyclerView? = null
    private var layoutManager: LinearLayoutManager? = null
    private var searchView: MySearchView? = null
    private var sleepDataDisposable = Disposable.disposed()
    private val SEARCH_DELAY = 2000
    override val keyInstanceState: String = "security_preferences"

    private val requestChangePin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val values = CreatePinFragment.extractValueFromIntent(result.data)
            Settings.get()
                .security()
                .setPin(values)
        }
    }
    private val requestCreatePin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val values = CreatePinFragment.extractValueFromIntent(result.data)
            Settings.get()
                .security()
                .setPin(values)
            preferencesAdapter?.applyToPreferenceInScreen(
                result.data?.getStringExtra(CreatePinFragment.EXTRA_PREF_SCREEN),
                result.data?.getStringExtra(CreatePinFragment.EXTRA_PREF_KEY)
                    ?: return@registerForActivityResult,
            ) {
                (it as TwoStatePreference).checked = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root =
            inflater.inflate(R.layout.preference_file_gallery_list_fragment, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        searchView = root.findViewById(R.id.searchview)
        searchView?.setRightButtonVisibility(false)
        searchView?.setLeftIcon(R.drawable.magnify)
        searchView?.setQuery("", true)
        layoutManager = LinearLayoutManager(requireActivity())
        val isNull = createPreferenceAdapter()
        preferencesView = (root.findViewById<RecyclerView>(R.id.recycler_view)).apply {
            layoutManager = this@SecurityPreferencesFragment.layoutManager
            adapter = preferencesAdapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireActivity(),
                R.anim.preference_layout_fall_down
            )
        }
        if (isNull) {
            preferencesAdapter?.onScreenChangeListener = this
            loadInstanceState({ createRootScreen() }, root)
        }

        searchView?.let {
            it.setOnBackButtonClickListener(object : MySearchView.OnBackButtonClickListener {
                override fun onBackButtonClick() {
                    if (it.text.nonNullNoEmpty() && it.text.trimmedNonNullNoEmpty()) {
                        preferencesAdapter?.findPreferences(
                            requireActivity(),
                            (it.text ?: return).toString(),
                            root
                        )
                    }
                }
            })
            it.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    sleepDataDisposable.dispose()
                    if (query.nonNullNoEmpty() && query.trimmedNonNullNoEmpty()) {
                        preferencesAdapter?.findPreferences(requireActivity(), query, root)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    sleepDataDisposable.dispose()
                    sleepDataDisposable = Single.just(Any())
                        .delay(SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                        .fromIOToMain()
                        .subscribe({
                            if (newText.nonNullNoEmpty() && newText.trimmedNonNullNoEmpty()) {
                                preferencesAdapter?.findPreferences(
                                    requireActivity(),
                                    newText,
                                    root
                                )
                            }
                        }, { RxUtils.dummy() })
                    return false
                }
            })
        }
        return root
    }

    override fun onBackPressed(): Boolean {
        return !goBack()
    }

    override fun canBackPressed(): Boolean {
        return canGoBack()
    }

    override fun beforeScreenChange(screen: PreferenceScreen): Boolean {
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        return true
    }

    override fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean, animation: Boolean) {
        searchView?.visibility = if (screen.getSearchQuery() == null) View.VISIBLE else View.GONE
        if (animation) {
            preferencesView?.scheduleLayoutAnimation()
        }
        preferencesView?.let { preferencesAdapter?.restoreAndObserveScrollPosition(it) }
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            if (screen.key == "root" || screen.title.isEmpty() && screen.titleRes == DEFAULT_RES_ID) {
                actionBar.setTitle(R.string.settings)
            } else if (screen.titleRes != DEFAULT_RES_ID) {
                actionBar.setTitle(screen.titleRes)
            } else {
                actionBar.title = screen.title
            }
            actionBar.setSubtitle(R.string.security)
        }
    }

    private fun startCreatePinActivity(preference: TwoStatePreference) {
        val o = Intent(requireActivity(), CreatePinActivity::class.java)
        o.putExtra(CreatePinFragment.EXTRA_PREF_SCREEN, preference.parent?.key)
        o.putExtra(CreatePinFragment.EXTRA_PREF_KEY, preference.key)
        requestCreatePin.launch(o)
    }

    @Suppress("DEPRECATION")
    private fun createRootScreen() = screen(requireActivity()) {
        collapseIcon = true

        switch(SecuritySettings.KEY_USE_PIN_FOR_ENTRANCE) {
            defaultValue = false
            summaryRes = R.string.ask_for_pin_on_application_start_summary
            titleRes = R.string.ask_for_pin_on_application_start_title
            onCheckedBeforeChange {
                if (it) {
                    if (!Settings.get().security().hasPinHash) {
                        startCreatePinActivity(this)
                        false
                    } else {
                        // при вызове mUsePinForSecurityPreference.setChecked(true) мы опять попадем в этот блок
                        true
                    }
                } else {
                    Settings.get().security().setPin(null)
                    true
                }
            }
        }

        switch("allow_fingerprint") {
            dependency = SecuritySettings.KEY_USE_PIN_FOR_ENTRANCE
            defaultValue = false
            titleRes = R.string.allow_fingerprint_title
        }

        pref("change_pin") {
            dependency = SecuritySettings.KEY_USE_PIN_FOR_ENTRANCE
            titleRes = R.string.change_pin_title
            onClick {
                requestChangePin.launch(
                    Intent(
                        requireActivity(),
                        CreatePinActivity::class.java
                    )
                )
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
    }

    override fun onResume() {
        super.onResume()
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            if (preferencesAdapter?.currentScreen?.key == "root" || preferencesAdapter?.currentScreen?.title.isNullOrEmpty() && (preferencesAdapter?.currentScreen?.titleRes == DEFAULT_RES_ID || preferencesAdapter?.currentScreen?.titleRes == 0)) {
                actionBar.setTitle(R.string.settings)
            } else if (preferencesAdapter?.currentScreen?.titleRes != DEFAULT_RES_ID && preferencesAdapter?.currentScreen?.titleRes != 0) {
                preferencesAdapter?.currentScreen?.titleRes?.let { actionBar.setTitle(it) }
            } else {
                actionBar.title = preferencesAdapter?.currentScreen?.title
            }
            actionBar.setSubtitle(R.string.security)
        }
        searchView?.visibility =
            if (preferencesAdapter?.currentScreen?.getSearchQuery() == null) View.VISIBLE else View.GONE
        ActivityFeatures.Builder()
            .begin()
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onDestroy() {
        sleepDataDisposable.dispose()
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        preferencesAdapter?.onScreenChangeListener = null
        preferencesView?.adapter = null
        super.onDestroy()
    }
}
