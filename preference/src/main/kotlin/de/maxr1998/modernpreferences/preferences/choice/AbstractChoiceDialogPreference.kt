package de.maxr1998.modernpreferences.preferences.choice

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID
import de.maxr1998.modernpreferences.helpers.getParcelableArrayListCompat
import de.maxr1998.modernpreferences.preferences.DialogPreference

abstract class AbstractChoiceDialogPreference(
    key: String,
    protected val items: ArrayList<SelectionItem>,
    fragmentManager: FragmentManager
) : DialogPreference(key, fragmentManager) {

    /**
     * Whether the summary should be auto-generated from the current selection.
     * If true, [summary] and [summaryRes] are ignored.
     *
     * Default true, set to false to turn off this feature.
     */
    var autoGeneratedSummary = true

    fun cloneItems(): ArrayList<SelectionItem> {
        return ArrayList(items)
    }

    init {
        require(items.isNotEmpty()) { "Supplied list of items may not be empty!" }
    }

    protected fun baseDialogInit(args: Bundle) {
        args.putInt(PreferencesExtra.TITLE_RES, titleRes)
        args.putCharSequence(PreferencesExtra.TITLE, title)
        args.putString(PreferencesExtra.PREFERENCE_KEY, key)
        args.putString(PreferencesExtra.PREFERENCE_SCREEN_KEY, parent?.key)
        args.putParcelableArrayList(PreferencesExtra.REQUEST_VALUE, items)
    }

    abstract fun resetSelection()

    abstract class AbsChooseDialog : DialogFragment() {
        protected abstract fun commit()
        protected abstract val allowMultiSelect: Boolean

        internal fun shouldSelect(item: SelectionItem): Boolean {
            return onItemClickListener?.onItemSelected(item) != false
        }

        internal abstract fun select(item: SelectionItem)
        abstract fun isSelected(item: SelectionItem): Boolean

        private var title: CharSequence? = null
        private lateinit var items: ArrayList<SelectionItem>

        @StringRes
        private var titleRes: Int = DISABLED_RESOURCE_ID
        protected var selectionAdapter: SelectionAdapter? = null

        var onItemClickListener: OnItemClickListener? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            titleRes = requireArguments().getInt(PreferencesExtra.TITLE_RES)
            title = requireArguments().getCharSequence(PreferencesExtra.TITLE)
            items = (requireArguments().getParcelableArrayListCompat(PreferencesExtra.REQUEST_VALUE)
                ?: return)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireActivity()).apply {
                if (titleRes != DISABLED_RESOURCE_ID) setTitle(titleRes) else setTitle(title)
                setView(RecyclerView(context).apply {
                    selectionAdapter =
                        SelectionAdapter(this@AbsChooseDialog, items, allowMultiSelect)
                    adapter = selectionAdapter
                    layoutManager = LinearLayoutManager(context)
                })
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    commit()
                    dismiss()
                }
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    dismiss()
                }
            }.create()
        }
    }
}