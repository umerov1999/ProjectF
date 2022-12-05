package dev.ragnarok.fenrir.dialog.base

import androidx.fragment.app.DialogFragment
import dev.ragnarok.fenrir.util.ViewUtils.keyboardHide

abstract class BaseDialogFragment : DialogFragment() {
    override fun onDestroy() {
        super.onDestroy()
        keyboardHide(requireActivity())
    }
}