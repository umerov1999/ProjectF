package de.maxr1998.modernpreferences

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.ArrayRes
import androidx.fragment.app.Fragment
import de.maxr1998.modernpreferences.preferences.ColorPickPreference
import de.maxr1998.modernpreferences.preferences.CustomTextPreference
import de.maxr1998.modernpreferences.preferences.EditTextPreference
import de.maxr1998.modernpreferences.preferences.MultiLineEditTextPreference
import de.maxr1998.modernpreferences.preferences.SeparatorSpaceTextPreference
import de.maxr1998.modernpreferences.preferences.choice.MultiChoiceDialogPreference
import de.maxr1998.modernpreferences.preferences.choice.SelectionItem
import de.maxr1998.modernpreferences.preferences.choice.SingleChoiceDialogPreference


abstract class AbsPreferencesFragment : Fragment() {
    protected var preferencesAdapter: PreferencesAdapter? = null
    protected abstract val keyInstanceState: String
    private var currentSaved: PreferencesAdapter.SavedState? = null

    @Suppress("deprecation")
    private inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, T::class.java)
        } else {
            getParcelable(key)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSaved = savedInstanceState?.getParcelableCompat(keyInstanceState)

        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.EDIT_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as EditTextPreference).persist(result.getCharSequence(PreferencesExtra.RESULT_VALUE))
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.SEPARATOR_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as SeparatorSpaceTextPreference).persist(result.getCharSequence(PreferencesExtra.RESULT_VALUE))
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.MULTI_LINE_EDIT_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as MultiLineEditTextPreference).persist(result.getCharSequence(PreferencesExtra.RESULT_VALUE))
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.CUSTOM_TEXT_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as CustomTextPreference).persist(result.getCharSequence(PreferencesExtra.RESULT_VALUE))
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.MULTI_CHOOSE_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as MultiChoiceDialogPreference).persistSelection(
                    result.getStringArrayList(
                        PreferencesExtra.RESULT_VALUE
                    ) ?: return@applyToPreferenceInScreen
                )
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.SINGLE_CHOOSE_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as SingleChoiceDialogPreference).persistSelection(
                    result.getParcelableCompat(
                        PreferencesExtra.RESULT_VALUE
                    )
                )
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.COLOR_DIALOG_REQUEST, this
        ) { _: String?, result: Bundle ->
            preferencesAdapter?.applyToPreferenceInScreen(
                result.getString(PreferencesExtra.PREFERENCE_SCREEN_KEY),
                result.getString(PreferencesExtra.PREFERENCE_KEY)
                    ?: return@setFragmentResultListener
            ) {
                (it as ColorPickPreference).persist(result.getInt(PreferencesExtra.RESULT_VALUE))
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PreferencesExtra.RECREATE_ACTIVITY_REQUEST, this
        ) { _: String?, _: Bundle ->
            requireActivity().recreate()
        }
    }

    protected fun createPreferenceAdapter(): Boolean {
        val ret = preferencesAdapter == null
        if (ret) {
            preferencesAdapter = PreferencesAdapter()
        }
        return ret
    }

    protected fun goBack(): Boolean {
        preferencesAdapter?.let {
            return it.goBack()
        }
        return true
    }

    protected fun canGoBack(): Boolean {
        preferencesAdapter?.let {
            return it.canGoBack()
        }
        return false
    }

    protected fun loadInstanceState(screen: MakeScreen, view: View?) {
        currentSaved
            ?.let {
                preferencesAdapter?.loadSavedState(
                    requireActivity(),
                    it,
                    screen.create(), view
                )
            }
            ?: preferencesAdapter?.setRootScreen(screen.create())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = preferencesAdapter?.getSavedState() ?: currentSaved
        state?.let {
            outState.putParcelable(
                keyInstanceState,
                state
            )
        }
    }

    fun interface MakeScreen {
        fun create(): PreferenceScreen
    }

    protected fun selItems(@ArrayRes names: Int, @ArrayRes values: Int): ArrayList<SelectionItem> {
        val nameData = resources.getTextArray(names)
        val valueData = resources.getTextArray(values)
        return selItems(nameData, valueData)
    }

    protected fun selItems(
        names: Array<CharSequence>,
        values: Array<CharSequence>
    ): ArrayList<SelectionItem> {
        if (names.size != values.size) {
            throw UnsupportedOperationException("SelectionItem names and values has wrong count")
        }
        val ret = ArrayList<SelectionItem>(names.size)
        for (i in names.indices) {
            ret.add(SelectionItem(values[i].toString(), names[i], null))
        }
        return ret
    }
}
