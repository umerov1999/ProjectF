package de.maxr1998.modernpreferences.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import de.maxr1998.modernpreferences.Preference
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.helpers.DEFAULT_RES_ID


class CustomTextPreference(key: String, val fragmentManager: FragmentManager) :
    Preference(key) {

    var currentInput: CharSequence? = null
        private set

    /**
     * The [InputType] applied to the contained [EditText][AppCompatEditText]
     */
    var textInputType: Int = InputType.TYPE_NULL

    @StringRes
    var textInputHintRes: Int = DEFAULT_RES_ID
    var textInputHint: CharSequence? = null

    @StringRes
    var messageRes: Int = DEFAULT_RES_ID
    var message: CharSequence? = null

    var defaultValue: String? = null
    var customTextChangeBeforeListener: OnCustomTextBeforeChangeListener? = null
    var customTextChangeAfterListener: OnCustomTextAfterChangeListener? = null

    override fun onAttach() {
        super.onAttach()
        if (currentInput == null) currentInput = getString() ?: defaultValue
    }

    override fun onLongClick(holder: PreferencesAdapter.ViewHolder): Boolean {
        CustomTextDialog.newInstance(
            title,
            titleRes,
            currentInput,
            textInputType,
            textInputHintRes,
            textInputHint,
            messageRes,
            message,
            defaultValue,
            key,
            parent?.key
        ).show(fragmentManager, "CustomTextDialog")
        return true
    }

    fun persist(input: CharSequence?) {
        if (customTextChangeBeforeListener?.onCustomTextBeforeChange(this, input) != false) {
            currentInput = input?.trim() ?: input
            commitString(currentInput?.toString())
            requestRebind()
            customTextChangeAfterListener?.onCustomTextAfterChange(this, currentInput)
        }
    }

    fun copyCustomText(o: CustomTextPreference): CustomTextPreference {
        defaultValue = o.defaultValue
        customTextChangeAfterListener = o.customTextChangeAfterListener
        customTextChangeBeforeListener = o.customTextChangeBeforeListener
        textInputType = o.textInputType
        textInputHintRes = o.textInputHintRes
        messageRes = o.messageRes
        message = o.message
        return this
    }

    fun reload() {
        currentInput = getString() ?: defaultValue
        requestRebind()
    }

    override fun resolveSummary(context: Context): CharSequence? {
        return currentInput ?: super.resolveSummary(context)
    }

    class CustomTextDialog : DialogFragment() {
        companion object {
            fun newInstance(
                title: CharSequence?,
                @StringRes titleRes: Int,
                currentInput: CharSequence?,
                textInputType: Int,
                @StringRes textInputHintRes: Int,
                textInputHint: CharSequence?,
                @StringRes messageRes: Int,
                message: CharSequence?,
                defaultValue: String?,
                key: String,
                screenKey: String?
            ): CustomTextDialog {
                val args = Bundle()
                args.putInt(PreferencesExtra.TITLE_RES, titleRes)
                args.putCharSequence(PreferencesExtra.TITLE, title)
                args.putCharSequence(PreferencesExtra.CURRENT_INPUT, currentInput)
                args.putInt(PreferencesExtra.TEXT_INPUT_TYPE, textInputType)
                args.putCharSequence(PreferencesExtra.TEXT_INPUT_HINT, textInputHint)
                args.putInt(PreferencesExtra.TEXT_INPUT_HINT_RES, textInputHintRes)
                args.putCharSequence(PreferencesExtra.MESSAGE, message)
                args.putInt(PreferencesExtra.MESSAGE_RES, messageRes)
                args.putString(PreferencesExtra.DEFAULT_VALUE, defaultValue)
                args.putString(PreferencesExtra.PREFERENCE_KEY, key)
                args.putString(PreferencesExtra.PREFERENCE_SCREEN_KEY, screenKey)
                val dialog = CustomTextDialog()
                dialog.arguments = args
                return dialog
            }
        }

        private var title: CharSequence? = null

        @StringRes
        private var titleRes: Int = DEFAULT_RES_ID
        private var currentInput: CharSequence? = null
        private var textInputType: Int = InputType.TYPE_NULL

        @StringRes
        private var textInputHintRes: Int = DEFAULT_RES_ID
        private var textInputHint: CharSequence? = null

        @StringRes
        private var messageRes: Int = DEFAULT_RES_ID
        private var message: CharSequence? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            titleRes = requireArguments().getInt(PreferencesExtra.TITLE_RES)
            title = requireArguments().getCharSequence(PreferencesExtra.TITLE)
            currentInput = requireArguments().getCharSequence(PreferencesExtra.CURRENT_INPUT)
            textInputType = requireArguments().getInt(PreferencesExtra.TEXT_INPUT_TYPE)
            textInputHint = requireArguments().getCharSequence(PreferencesExtra.TEXT_INPUT_HINT)
            textInputHintRes = requireArguments().getInt(PreferencesExtra.TEXT_INPUT_HINT_RES)
            message = requireArguments().getCharSequence(PreferencesExtra.MESSAGE)
            messageRes = requireArguments().getInt(PreferencesExtra.MESSAGE_RES)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireActivity()).apply {
                val view = View.inflate(context, R.layout.map_preference_dialog_edittext, null)
                if (titleRes != DEFAULT_RES_ID) setTitle(titleRes) else setTitle(title)
                val editText = view.findViewById<TextInputEditText>(R.id.preference_edit).apply {
                    if (textInputType != InputType.TYPE_NULL) {
                        inputType = textInputType
                    }
                    when {
                        textInputHintRes != DEFAULT_RES_ID -> setHint(textInputHintRes)
                        textInputHint != null -> hint = textInputHint
                    }
                    setText(currentInput)
                }
                view.findViewById<MaterialTextView>(R.id.preference_message).apply {
                    when {
                        message != null -> {
                            visibility = View.VISIBLE
                            text = message
                        }
                        messageRes != DEFAULT_RES_ID -> {
                            visibility = View.VISIBLE
                            setText(messageRes)
                        }
                        else -> {
                            visibility = View.GONE
                        }
                    }
                }
                setView(view)
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Bundle()
                    intent.putCharSequence(PreferencesExtra.RESULT_VALUE, editText.text)
                    intent.putString(
                        PreferencesExtra.PREFERENCE_KEY,
                        requireArguments().getString(PreferencesExtra.PREFERENCE_KEY)
                    )
                    intent.putString(
                        PreferencesExtra.PREFERENCE_SCREEN_KEY,
                        requireArguments().getString(PreferencesExtra.PREFERENCE_SCREEN_KEY)
                    )
                    parentFragmentManager.setFragmentResult(
                        PreferencesExtra.CUSTOM_TEXT_DIALOG_REQUEST,
                        intent
                    )
                    dismiss()
                }
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    dismiss()
                }
            }.create()
        }
    }

    fun interface OnCustomTextAfterChangeListener {
        fun onCustomTextAfterChange(preference: CustomTextPreference, text: CharSequence?)
    }

    fun interface OnCustomTextBeforeChangeListener {
        fun onCustomTextBeforeChange(preference: CustomTextPreference, text: CharSequence?): Boolean
    }
}
