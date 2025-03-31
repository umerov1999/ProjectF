package de.maxr1998.modernpreferences.preferences.choice

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.FragmentManager
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID

class SingleChoiceDialogPreference(
    key: String,
    items: ArrayList<SelectionItem>,
    fragmentManager: FragmentManager
) :
    AbstractChoiceDialogPreference(key, items, fragmentManager) {

    /**
     * The initial selection if no choice has been made and no value persisted to [SharedPreferences][android.content.SharedPreferences] yet.
     *
     * Must match a [SelectionItem.key] in [items].
     */
    var initialSelection: String? = null

    private var currentSelection: SelectionItem? = null

    var selectionBeforeChangeListener: OnSelectionBeforeChangeListener? = null
    var selectionAfterChangeListener: OnSelectionAfterChangeListener? = null

    fun copySingleChoice(o: SingleChoiceDialogPreference): SingleChoiceDialogPreference {
        selectionBeforeChangeListener = o.selectionBeforeChangeListener
        selectionAfterChangeListener = o.selectionAfterChangeListener
        initialSelection = o.initialSelection
        autoGeneratedSummary = o.autoGeneratedSummary
        return this
    }

    override fun onAttach() {
        super.onAttach()
        if (currentSelection == null) {
            resetSelection()
        }
    }

    fun persistSelection(sel: SelectionItem?) {
        currentSelection = sel
        currentSelection?.let { (key1, _, _, _, _) ->
            if (selectionBeforeChangeListener?.onSelectionBeforeChange(
                    this,
                    key1
                ) != false
            ) {
                commitString(key1)
                requestRebind()
                selectionAfterChangeListener?.onSelectionAfterChange(this, key1)
            }
        }
    }

    override fun resetSelection() {
        val persisted = getString() ?: initialSelection
        currentSelection = persisted?.let { items.find { (key1, _, _, _, _) -> key1 == persisted } }
    }

    override fun createAndShowDialogFragment() {
        val args = Bundle()
        baseDialogInit(args)
        SingleChooseDialog
            .newInstance(args, currentSelection)
            .show(fragmentManager, "SingleChooseDialog")
    }

    class SingleChooseDialog : AbsChooseDialog() {
        private inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelable(key, T::class.java)
            } else {
                @Suppress("deprecation")
                getParcelable(key)
            }
        }

        private var currentSelection: SelectionItem? = null

        companion object {
            fun newInstance(args: Bundle, obj: SelectionItem?): SingleChooseDialog {
                args.putParcelable(PreferencesExtra.DEFAULT_VALUE, obj)
                val dialog = SingleChooseDialog()
                dialog.arguments = args
                return dialog
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            savedInstanceState?.getParcelableCompat<SelectionItem>("single_selections")?.let {
                currentSelection = it
            } ?: run {
                currentSelection =
                    requireArguments().getParcelableCompat(PreferencesExtra.DEFAULT_VALUE)
            }
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putParcelable("single_selections", currentSelection)
        }

        override fun commit() {
            val intent = Bundle()
            intent.putParcelable(PreferencesExtra.RESULT_VALUE, currentSelection)
            intent.putString(
                PreferencesExtra.PREFERENCE_KEY,
                requireArguments().getString(PreferencesExtra.PREFERENCE_KEY)
            )
            intent.putString(
                PreferencesExtra.PREFERENCE_SCREEN_KEY,
                requireArguments().getString(PreferencesExtra.PREFERENCE_SCREEN_KEY)
            )
            parentFragmentManager.setFragmentResult(
                PreferencesExtra.SINGLE_CHOOSE_DIALOG_REQUEST,
                intent
            )
        }

        override val allowMultiSelect: Boolean
            get() = false

        override fun select(item: SelectionItem) {
            currentSelection = item
            selectionAdapter?.notifySelectionChanged()
        }

        override fun isSelected(item: SelectionItem): Boolean = item == currentSelection

    }

    override fun resolveSummary(context: Context): CharSequence? {
        val selection = currentSelection
        return when {
            autoGeneratedSummary && selection != null -> when {
                selection.titleRes != DISABLED_RESOURCE_ID -> context.resources.getText(selection.titleRes)
                else -> selection.title
            }

            else -> super.resolveSummary(context)
        }
    }

    fun interface OnSelectionAfterChangeListener {
        fun onSelectionAfterChange(preference: SingleChoiceDialogPreference, selection: String)
    }

    fun interface OnSelectionBeforeChangeListener {
        /**
         * Notified when the selection of the connected [SingleChoiceDialogPreference] changes,
         * meaning after the user closes the dialog by pressing "ok".
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param selection the new selection
         *
         * @return true to commit the new selection to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onSelectionBeforeChange(
            preference: SingleChoiceDialogPreference,
            selection: String
        ): Boolean
    }
}
