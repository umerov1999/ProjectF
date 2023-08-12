package dev.ragnarok.fenrir.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
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
import de.maxr1998.modernpreferences.helpers.DEFAULT_RES_ID
import de.maxr1998.modernpreferences.helpers.onClick
import de.maxr1998.modernpreferences.helpers.pref
import de.maxr1998.modernpreferences.helpers.screen
import de.maxr1998.modernpreferences.helpers.singleChoice
import de.maxr1998.modernpreferences.helpers.subScreen
import de.maxr1998.modernpreferences.helpers.switch
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.CanBackPressedCallback
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.UpdatableNavigation
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit


class NotificationPreferencesFragment : AbsPreferencesFragment(),
    PreferencesAdapter.OnScreenChangeListener,
    BackPressCallback, CanBackPressedCallback {
    private var preferencesView: RecyclerView? = null
    private var layoutManager: LinearLayoutManager? = null
    private var searchView: MySearchView? = null
    private var sleepDataDisposable = Disposable.disposed()
    private val SEARCH_DELAY = 2000
    override val keyInstanceState: String = "notification_preferences"

    private val requestRingTone = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data?.path
            Settings.get()
                .notifications()
                .setNotificationRingtoneUri(uri)
        }
    }
    private var current: Ringtone? = null
    private var selection = 0

    private fun <T, E> getKeyByValue(map: Map<T, E>, value: E): T? {
        for ((key, value1) in map) {
            if (value == value1) {
                return key
            }
        }
        return null
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
            layoutManager = this@NotificationPreferencesFragment.layoutManager
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
                            it.text.toString(),
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
            actionBar.setSubtitle(R.string.notif_setting_title)
        }
        if (requireActivity() is UpdatableNavigation) {
            (requireActivity() as UpdatableNavigation).onUpdateNavigation()
        }
    }


    @Suppress("DEPRECATION")
    private fun createRootScreen() = screen(requireActivity()) {
        collapseIcon = true
        subScreen("security_preferences") {
            collapseIcon = true
            titleRes = R.string.general_settings
            switch("high_notif_priority") {
                defaultValue = false
                summaryRes = R.string.high_notif_priority_summary
                titleRes = R.string.high_notif_priority_title
            }

            switch("quick_reply_immediately") {
                defaultValue = false
                summaryRes = R.string.quick_reply_summary
                titleRes = R.string.quick_reply
            }

            pref("notif_sound") {
                titleRes = R.string.select_ringtone_title
                onClick {
                    showAlertDialog()
                    true
                }
            }

            singleChoice(
                "vibration_length",
                selItems(
                    R.array.array_vibration_length_names,
                    R.array.array_vibration_length_value
                ),
                parentFragmentManager
            ) {
                initialSelection = "4"
                titleRes = R.string.vibration_length
            }

        }

        subScreen("messages_in_dialogs_tite_section") {
            collapseIcon = true
            titleRes = R.string.messages_in_dialogs_tite
            switch("new_dialog_message_notif_enable") {
                defaultValue = true
                titleRes = R.string.enable_dialogs_notif_title
            }

            switch("new_dialog_message_notif_sound") {
                defaultValue = true
                dependency = "new_dialog_message_notif_enable"
                titleRes = R.string.enable_dialogs_sound_title
            }

            switch("new_dialog_message_notif_vibration") {
                defaultValue = true
                dependency = "new_dialog_message_notif_enable"
                titleRes = R.string.enable_dialogs_vibro_title
            }

            switch("new_dialog_message_notif_led") {
                defaultValue = true
                dependency = "new_dialog_message_notif_enable"
                titleRes = R.string.enable_dialogs_led_title
            }

        }

        subScreen("messages_in_groupchat_tite_section") {
            collapseIcon = true
            titleRes = R.string.messages_in_groupchat_tite
            switch("new_groupchat_message_notif_enable") {
                defaultValue = true
                titleRes = R.string.enable_groupchat_notif_title
            }

            switch("new_groupchat_message_notif_sound") {
                defaultValue = true
                dependency = "new_groupchat_message_notif_enable"
                titleRes = R.string.enable_groupchat_sound_title
            }

            switch("new_groupchat_message_notif_vibration") {
                defaultValue = true
                dependency = "new_groupchat_message_notif_enable"
                titleRes = R.string.enable_groupchat_vibro_title
            }

            switch("new_groupchat_message_notif_led") {
                defaultValue = true
                dependency = "new_groupchat_message_notif_enable"
                titleRes = R.string.enable_groupchat_led_title
            }

        }

        subScreen("other_notifications_settings_section") {
            collapseIcon = true
            titleRes = R.string.other_notifications_settings
            switch("other_notifications_enable") {
                defaultValue = true
                titleRes = R.string.enable_dialogs_notif_title
            }

            switch("other_notif_sound") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.enable_groupchat_sound_title
            }

            switch("other_notif_vibration") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.enable_groupchat_vibro_title
            }

            switch("other_notif_led") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.enable_groupchat_led_title
            }

        }

        subScreen("new_comment_notification_section") {
            collapseIcon = true
            titleRes = R.string.other_notifications_types
            switch("new_comment_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.new_comment_notification
            }

            switch("friend_request_accepted_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.friend_request_accepted_notification
            }

            switch("new_follower_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.new_follower_notification
            }

            switch("group_invited_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.group_invited_notification
            }

            switch("reply_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.reply_notification
            }

            switch("new_wall_post_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.new_wall_post_notification
            }

            switch("wall_publish_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.wall_publish_notification
            }

            switch("new_posts_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.new_posts_of_users_and_communities
            }

            switch("likes_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.likes
            }

            switch("birtday_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.birthdays
            }

            switch("mention_notification") {
                defaultValue = true
                dependency = "other_notifications_enable"
                titleRes = R.string.mentions
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
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
            actionBar.setSubtitle(R.string.notif_setting_title)
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

    private fun stopRingtoneIfExist() {
        if (current != null && current?.isPlaying == true) {
            current?.stop()
        }
    }

    private fun showAlertDialog() {
        val ringtones: Map<String, String> = getNotifications()
        val keys = ringtones.keys
        val array = keys.toTypedArray()
        val selectionKey = getKeyByValue(
            ringtones, Settings.get()
                .notifications()
                .notificationRingtone
        )
        selection = array.indexOf(selectionKey)
        MaterialAlertDialogBuilder(requireActivity()).setSingleChoiceItems(
            array, selection
        ) { _: DialogInterface?, which: Int ->
            selection = which
            stopRingtoneIfExist()
            val title = array[which]
            val uri = ringtones[title]
            val r = RingtoneManager.getRingtone(requireActivity(), Uri.parse(uri))
            current = r
            r.play()
        }.setPositiveButton(R.string.button_ok) { _, _ ->
            if (selection == -1) {
                CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                    .showToastError(R.string.ringtone_not_selected)
            } else {
                val title = array[selection]
                Settings.get()
                    .notifications()
                    .setNotificationRingtoneUri(ringtones[title])
                stopRingtoneIfExist()
            }
        }
            .setNegativeButton(
                R.string.cancel
            ) { _: DialogInterface?, _: Int -> stopRingtoneIfExist() }
            .setNeutralButton(R.string.ringtone_custom) { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "file/audio"
                requestRingTone.launch(intent)
            }
            .setOnDismissListener { stopRingtoneIfExist() }
            .show()
    }

    private fun getNotifications(): Map<String, String> {
        val manager = RingtoneManager(requireActivity())
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor: Cursor = manager.cursor
        val list: MutableMap<String, String> = HashMap()
        while (cursor.moveToNext()) {
            val notificationTitle: String = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val notificationUri = manager.getRingtoneUri(cursor.position)
            list[notificationTitle] = notificationUri.toString()
        }
        list[getString(R.string.ringtone_vk)] = Settings.get()
            .notifications()
            .defNotificationRingtone
        return list
    }

    override fun onDestroy() {
        sleepDataDisposable.dispose()
        preferencesView?.let { preferencesAdapter?.stopObserveScrollPosition(it) }
        preferencesAdapter?.onScreenChangeListener = null
        preferencesView?.adapter = null
        super.onDestroy()
    }
}
