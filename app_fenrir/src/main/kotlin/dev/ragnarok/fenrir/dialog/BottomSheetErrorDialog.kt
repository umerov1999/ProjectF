package dev.ragnarok.fenrir.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.toast.CustomToast

class BottomSheetErrorDialog : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            View.inflate(requireActivity(), R.layout.dialog_bottomsheet_error, null)
        val title = requireArguments().getString(Extra.TITLE)
        val description = requireArguments().getString(Extra.DATA)

        val mTitle = view.findViewById<MaterialTextView>(R.id.item_error_title)
        val mStacktrace = view.findViewById<MaterialTextView>(R.id.item_error_stacktrace)
        val mButtonCopy = view.findViewById<ExtendedFloatingActionButton>(R.id.item_button_copy)

        mTitle.text = getString(R.string.error_title, title)
        mStacktrace.text = getString(R.string.error_stacktrace, "\n" + description)

        mButtonCopy.setOnClickListener {
            val clipboard =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            if (clipboard != null) {
                val clip = ClipData.newPlainText(
                    "errors",
                    "$title: {$description}"
                )
                clipboard.setPrimaryClip(clip)
                CustomToast.createCustomToast(requireActivity(), null)
                    ?.showToastInfo(R.string.crash_error_activity_error_details_copied)
            }
            dismiss()
        }
        return view
    }
}