package dev.ragnarok.filegallery.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import dev.ragnarok.filegallery.R

class DialogLocalServerOptionDialog : BottomSheetDialogFragment() {
    private var isDiscography = false
    private var isReverse = false
    private var toggleDiscography: MaterialButton? = null
    private var listener: DialogLocalServerOptionListener? = null
    override fun onPause() {
        super.onPause()
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = View.inflate(requireActivity(), R.layout.dialog_local_server_options, null)
        val scReverse: MaterialSwitch = root.findViewById(R.id.reverse_time)
        toggleDiscography = root.findViewById(R.id.go_discography)
        scReverse.isChecked = isReverse
        scReverse.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            isReverse = isChecked
            listener?.onReverse(isChecked)
        }
        toggleDiscography?.setOnClickListener {
            isDiscography = !isDiscography
            listener?.onDiscography(isDiscography)
            resolve()
        }
        resolve()
        return root
    }

    private fun resolve() {
        toggleDiscography?.setText(if (isDiscography) R.string.return_away else R.string.go_discography)
    }

    interface DialogLocalServerOptionListener {
        fun onReverse(reverse: Boolean)
        fun onDiscography(discography: Boolean)
    }

    companion object {
        fun newInstance(
            isDiscography: Boolean,
            isReverse: Boolean,
            listener: DialogLocalServerOptionListener?
        ): DialogLocalServerOptionDialog {
            val dialog = DialogLocalServerOptionDialog()
            dialog.listener = listener
            dialog.isDiscography = isDiscography
            dialog.isReverse = isReverse
            return dialog
        }
    }
}