package dev.ragnarok.fenrir.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.hasFlag

class DialogNotifOptionsDialog : BottomSheetDialogFragment() {
    private var mask = 0
    private var peerId = 0L
    private var accountId = 0L
    private var scEnable: MaterialSwitch? = null
    private var scHighPriority: MaterialSwitch? = null
    private var scSound: MaterialSwitch? = null
    private var scVibro: MaterialSwitch? = null
    private var scLed: MaterialSwitch? = null
    private var listener: Listener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        peerId = requireArguments().getLong(Extra.PEER_ID)
        mask = Settings.get()
            .notifications()
            .getNotifPref(accountId, peerId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = View.inflate(requireActivity(), R.layout.dialog_dialog_options, null)
        scEnable = root.findViewById(R.id.enable)
        scHighPriority = root.findViewById(R.id.priority)
        scSound = root.findViewById(R.id.sound)
        scVibro = root.findViewById(R.id.vibro)
        scLed = root.findViewById(R.id.led)
        val save: MaterialButton = root.findViewById(R.id.buttonSave)
        val restore: MaterialButton = root.findViewById(R.id.button_restore)
        scEnable?.isChecked = hasFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)
        scEnable?.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean -> resolveOtherSwitches() }
        scSound?.isChecked = hasFlag(mask, INotificationSettings.FLAG_SOUND)
        scHighPriority?.isChecked = hasFlag(mask, INotificationSettings.FLAG_HIGH_PRIORITY)
        scVibro?.isChecked = hasFlag(mask, INotificationSettings.FLAG_VIBRO)
        scLed?.isChecked = hasFlag(mask, INotificationSettings.FLAG_LED)
        save.setOnClickListener {
            onSaveClick()
            listener?.onSelected()
            dismiss()
        }
        restore.setOnClickListener {
            Settings.get()
                .notifications()
                .setDefault(accountId, peerId)
            listener?.onSelected()
            dismiss()
        }
        resolveOtherSwitches()
        return root
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    private fun onSaveClick() {
        var newMask = 0
        if (scEnable?.isChecked == true) {
            newMask += INotificationSettings.FLAG_SHOW_NOTIF
        }
        if (scHighPriority?.isChecked == true) {
            newMask += INotificationSettings.FLAG_HIGH_PRIORITY
        }
        if (scSound?.isChecked == true) {
            newMask += INotificationSettings.FLAG_SOUND
        }
        if (scVibro?.isChecked == true) {
            newMask += INotificationSettings.FLAG_VIBRO
        }
        if (scLed?.isChecked == true) {
            newMask += INotificationSettings.FLAG_LED
        }
        Settings.get()
            .notifications()
            .setNotifPref(accountId, peerId, newMask)
    }

    private fun resolveOtherSwitches() {
        val enable = scEnable?.isChecked == true
        scHighPriority?.isEnabled = enable
        scSound?.isEnabled = enable
        scVibro?.isEnabled = enable
        scLed?.isEnabled = enable
    }

    interface Listener {
        fun onSelected()
    }

    companion object {
        fun newInstance(aid: Long, peerId: Long, listener: Listener?): DialogNotifOptionsDialog {
            val args = Bundle()
            args.putLong(Extra.PEER_ID, peerId)
            args.putLong(Extra.ACCOUNT_ID, aid)
            val dialog = DialogNotifOptionsDialog()
            dialog.listener = listener
            dialog.arguments = args
            return dialog
        }
    }
}