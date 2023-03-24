package dev.ragnarok.fenrir.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.maxr1998.modernpreferences.AbsPreferencesFragment
import de.maxr1998.modernpreferences.PreferenceScreen
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.helpers.*
import de.maxr1998.modernpreferences.preferences.TwoStatePreference
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.activity.CreatePinActivity
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.createpin.CreatePinFragment
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.CanBackPressedCallback
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.UpdatableNavigation
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.SecuritySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView
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
        val root = inflater.inflate(R.layout.preference_fenrir_list_fragment, container, false)
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
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
    }


    @Suppress("DEPRECATION")
    private fun createRootScreen() = screen(requireActivity()) {
        collapseIcon = true
        collapse("security_preferences") {
            titleRes = R.string.general_settings
            switch(SecuritySettings.KEY_USE_PIN_FOR_SECURITY) {
                defaultValue = false
                titleRes = R.string.use_pin_for_security_title
                onCheckedBeforeChange {
                    if (it) {
                        if (!Settings.get().security().hasPinHash()) {
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

            switch(SecuritySettings.KEY_USE_PIN_FOR_ENTRANCE) {
                defaultValue = false
                dependency = SecuritySettings.KEY_USE_PIN_FOR_SECURITY
                summaryRes = R.string.ask_for_pin_on_application_start_summary
                titleRes = R.string.ask_for_pin_on_application_start_title
            }

            switch(SecuritySettings.DELAYED_PIN_FOR_ENTRANCE) {
                defaultValue = false
                dependency = SecuritySettings.KEY_USE_PIN_FOR_SECURITY
                summaryRes = R.string.delayed_pin_for_entrance_summary
                titleRes = R.string.delayed_pin_for_entrance_title
            }

            switch("allow_fingerprint") {
                defaultValue = false
                dependency = SecuritySettings.KEY_USE_PIN_FOR_SECURITY
                titleRes = R.string.allow_fingerprint_title
            }

            pref(SecuritySettings.KEY_CHANGE_PIN) {
                dependency = SecuritySettings.KEY_USE_PIN_FOR_SECURITY
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
        collapse("secured_messages_section") {
            titleRes = R.string.secured_messages

            pref("encryption_terms_of_use") {
                titleRes = R.string.encryption_terms_of_use_title
                onClick {
                    val view = View.inflate(
                        requireActivity(),
                        R.layout.content_encryption_terms_of_use,
                        null
                    )
                    MaterialAlertDialogBuilder(requireActivity())
                        .setView(view)
                        .setTitle(R.string.fenrir_encryption)
                        .setNegativeButton(R.string.button_cancel, null)
                        .show()
                    true
                }
            }

            pref(SecuritySettings.KEY_DELETE_KEYS) {
                summaryRes = R.string.clear_storage_of_encryption_keys
                titleRes = R.string.delete_keys
                onClick {
                    onClearKeysClick()
                    true
                }
            }
        }
        collapse("other_section") {
            titleRes = R.string.other

            switch("hide_notif_message_body") {
                summaryRes = R.string.hide_notif_message_body_summary
                titleRes = R.string.hide_notif_message_body_title
            }

            singleChoice(
                "crypt_version",
                selItems(R.array.crypt_version_names, R.array.crypt_version_list),
                parentFragmentManager
            ) {
                initialSelection = "1"
                titleRes = R.string.crypt_version
            }

            switch("disable_encryption") {
                defaultValue = false
                titleRes = R.string.disable_encryption
            }

            switch("show_hidden_accounts") {
                defaultValue = true
                titleRes = R.string.show_hidden_accounts
            }

            switch("disable_hidden_accounts") {
                defaultValue = false
                titleRes = R.string.disable_hidden_accounts
                onCheckedBeforeChange {
                    !Settings.get().accounts().currentHidden
                }
                onCheckedChange {
                    Settings.get().accounts().loadAccounts(true)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
    }

    private fun onClearKeysClick() {
        val items = arrayOf(
            getString(R.string.for_the_current_account),
            getString(R.string.for_all_accounts)
        )
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _: DialogInterface?, which: Int ->
                onClearKeysClick(
                    which == 1
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun onClearKeysClick(allAccount: Boolean) {
        if (allAccount) {
            val accountIds: Collection<Long> = Settings.get()
                .accounts()
                .registered
            for (accountId in accountIds) {
                removeKeysFor(accountId)
            }
        } else {
            val currentAccountId = Settings.get()
                .accounts()
                .current
            if (ISettings.IAccountsSettings.INVALID_ID != currentAccountId) {
                removeKeysFor(currentAccountId)
            }
        }
        CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
            .showToastSuccessBottom(R.string.deleted)
    }

    private fun removeKeysFor(accountId: Long) {
        Stores.instance
            .keys(KeyLocationPolicy.PERSIST)
            .deleteAll(accountId)
            .blockingAwait()
        Stores.instance
            .keys(KeyLocationPolicy.RAM)
            .deleteAll(accountId)
            .blockingAwait()
    }

    private fun startCreatePinActivity(preference: TwoStatePreference) {
        val o = Intent(requireActivity(), CreatePinActivity::class.java)
        o.putExtra(CreatePinFragment.EXTRA_PREF_SCREEN, preference.parent?.key)
        o.putExtra(CreatePinFragment.EXTRA_PREF_KEY, preference.key)
        requestCreatePin.launch(o)
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.PREFERENCES)
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
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_SETTINGS)
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
        searchView?.visibility =
            if (preferencesAdapter?.currentScreen?.getSearchQuery() == null) View.VISIBLE else View.GONE
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
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
