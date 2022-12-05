package de.maxr1998.modernpreferences.preferences.choice

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.helpers.DEFAULT_RES_ID

class MultiChoiceDialogPreference(
    key: String,
    items: ArrayList<SelectionItem>,
    fragmentManager: FragmentManager
) :
    AbstractChoiceDialogPreference(key, items, fragmentManager) {

    /**
     * The initial selections if no choice has been made yet and no value
     * was persisted to [SharedPreferences][android.content.SharedPreferences]
     */
    var initialSelections: Set<String>? = null

    private val selections: MutableSet<SelectionItem> = HashSet()

    val currentSelections: Set<SelectionItem>
        get() = HashSet(selections)

    var selectionBeforeChangeListener: OnSelectionBeforeChangeListener? = null
    var selectionAfterChangeListener: OnSelectionAfterChangeListener? = null

    fun copyMultiChoice(o: MultiChoiceDialogPreference): MultiChoiceDialogPreference {
        selectionBeforeChangeListener = o.selectionBeforeChangeListener
        selectionAfterChangeListener = o.selectionAfterChangeListener
        initialSelections = o.initialSelections
        autoGeneratedSummary = o.autoGeneratedSummary
        return this
    }

    override fun onAttach() {
        super.onAttach()
        if (selections.isEmpty()) {
            resetSelection()
        }
    }

    fun persistSelection(p: ArrayList<String>) {
        val resultSet = HashSet<String>(p)
        if (selectionBeforeChangeListener?.onSelectionBeforeChange(this, resultSet) != false) {
            commitStringSet(resultSet)
            selections.clear()
            selections += p.mapNotNull { key -> items.find { (key1, _, _, _, _) -> key1 == key } }
            requestRebind()
            selectionAfterChangeListener?.onSelectionAfterChange(this, resultSet)
        }
    }

    override fun resetSelection() {
        val persisted = getStringSet() ?: initialSelections?.toList() ?: emptyList()
        selections.clear()
        selections += persisted.mapNotNull { key -> items.find { (key1, _, _, _, _) -> key1 == key } }
    }

    override fun createAndShowDialogFragment() {
        val args = Bundle()
        baseDialogInit(args)
        MultiChooseDialog.newInstance(
            args,
            ArrayList(getStringSet()?.toList() ?: initialSelections?.toList() ?: emptyList())
        )
            .show(fragmentManager, "MultiChooseDialog")
    }

    override fun resolveSummary(context: Context): CharSequence? = when {
        autoGeneratedSummary && selections.isNotEmpty() -> selections.joinToString(
            limit = 3,
            truncated = "…"
        ) { (_, titleRes1, title1, _, _) ->
            when {
                titleRes1 != DEFAULT_RES_ID -> context.resources.getText(titleRes1)
                else -> title1
            }
        }
        else -> super.resolveSummary(context)
    }

    class MultiChooseDialog : AbsChooseDialog() {
        private var selections: MutableSet<String> = HashSet()

        companion object {
            fun newInstance(args: Bundle, list: ArrayList<String>): MultiChooseDialog {
                args.putStringArrayList(PreferencesExtra.DEFAULT_VALUE, list)
                val dialog = MultiChooseDialog()
                dialog.arguments = args
                return dialog
            }
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putStringArrayList("multi_selections", ArrayList(selections.toList()))
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            savedInstanceState?.getStringArrayList("multi_selections")?.let {
                selections = it.toHashSet()
            } ?: run {
                selections =
                    (requireArguments().getStringArrayList(PreferencesExtra.DEFAULT_VALUE)
                        ?: return@run)
                        .toHashSet()
            }
        }

        override fun commit() {
            val intent = Bundle()
            intent.putStringArrayList(PreferencesExtra.RESULT_VALUE, ArrayList(selections.toList()))
            intent.putString(
                PreferencesExtra.PREFERENCE_KEY,
                requireArguments().getString(PreferencesExtra.PREFERENCE_KEY)
            )
            intent.putString(
                PreferencesExtra.PREFERENCE_SCREEN_KEY,
                requireArguments().getString(PreferencesExtra.PREFERENCE_SCREEN_KEY)
            )
            parentFragmentManager.setFragmentResult(
                PreferencesExtra.MULTI_CHOOSE_DIALOG_REQUEST,
                intent
            )
        }

        override val allowMultiSelect: Boolean
            get() = true

        override fun select(item: SelectionItem) {
            if (!selections.add(item.key)) {
                selections.remove(item.key)
            }
            selectionAdapter?.notifySelectionChanged()
        }

        override fun isSelected(item: SelectionItem): Boolean = item.key in selections

    }

    fun interface OnSelectionAfterChangeListener {
        fun onSelectionAfterChange(
            preference: MultiChoiceDialogPreference,
            selection: Set<String>
        )
    }

    fun interface OnSelectionBeforeChangeListener {
        /**
         * Notified when the selection of the connected [MultiChoiceDialogPreference] changes,
         * meaning after the user closes the dialog by pressing "ok".
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param selection the new selection
         *
         * @return true to commit the new selection to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onSelectionBeforeChange(
            preference: MultiChoiceDialogPreference,
            selection: Set<String>
        ): Boolean
    }
}
