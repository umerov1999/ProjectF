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
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID


class EditTextPreference(key: String, fragmentManager: FragmentManager) :
    DialogPreference(key, fragmentManager) {

    var currentInput: CharSequence? = null
        private set

    /**
     * The [InputType] applied to the contained [EditText][AppCompatEditText]
     */
    var textInputType: Int = InputType.TYPE_NULL

    @StringRes
    var textInputHintRes: Int = DISABLED_RESOURCE_ID
    var textInputHint: CharSequence? = null

    @StringRes
    var messageRes: Int = DISABLED_RESOURCE_ID
    var message: CharSequence? = null
    var defaultValue: String? = null
    var isTrim = false
    var textChangeBeforeListener: OnTextChangeBeforeListener? = null
    var textChangeAfterListener: OnTextChangeAfterListener? = null

    override fun onAttach() {
        super.onAttach()
        if (currentInput == null) currentInput = getString() ?: defaultValue
    }

    fun copyEdit(o: EditTextPreference): EditTextPreference {
        textInputType = o.textInputType
        textInputHintRes = o.textInputHintRes
        messageRes = o.messageRes
        message = o.message
        defaultValue = o.defaultValue
        textChangeBeforeListener = o.textChangeBeforeListener
        textChangeAfterListener = o.textChangeAfterListener
        isTrim = o.isTrim
        return this
    }

    fun reload() {
        currentInput = getString() ?: defaultValue
        requestRebind()
    }

    fun persist(input: CharSequence?) {
        if (textChangeBeforeListener?.onTextBeforeChange(this, input) != false) {
            currentInput = if (input != null && isTrim) {
                input.trim()
            } else {
                input
            }
            commitString(currentInput?.toString())
            requestRebind()
            textChangeAfterListener?.onTextAfterChange(this, currentInput)
        }
    }

    override fun resolveSummary(context: Context): CharSequence? {
        return currentInput ?: super.resolveSummary(context)
    }

    override fun createAndShowDialogFragment() {
        EditTextDialog.newInstance(
            title,
            titleRes,
            currentInput,
            textInputType,
            textInputHintRes,
            textInputHint,
            messageRes,
            message,
            key,
            parent?.key
        ).show(fragmentManager, "EditTextDialog")
    }

    class EditTextDialog : DialogFragment() {
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
                key: String,
                screenKey: String?
            ): EditTextDialog {
                val args = Bundle()
                args.putInt(PreferencesExtra.TITLE_RES, titleRes)
                args.putCharSequence(PreferencesExtra.TITLE, title)
                args.putCharSequence(PreferencesExtra.CURRENT_INPUT, currentInput)
                args.putInt(PreferencesExtra.TEXT_INPUT_TYPE, textInputType)
                args.putCharSequence(PreferencesExtra.TEXT_INPUT_HINT, textInputHint)
                args.putInt(PreferencesExtra.TEXT_INPUT_HINT_RES, textInputHintRes)
                args.putCharSequence(PreferencesExtra.MESSAGE, message)
                args.putInt(PreferencesExtra.MESSAGE_RES, messageRes)
                args.putString(PreferencesExtra.PREFERENCE_KEY, key)
                args.putString(PreferencesExtra.PREFERENCE_SCREEN_KEY, screenKey)
                val dialog = EditTextDialog()
                dialog.arguments = args
                return dialog
            }
        }

        private var title: CharSequence? = null

        @StringRes
        private var titleRes: Int = DISABLED_RESOURCE_ID
        private var currentInput: CharSequence? = null
        private var textInputType: Int = InputType.TYPE_NULL

        @StringRes
        private var textInputHintRes: Int = DISABLED_RESOURCE_ID
        private var textInputHint: CharSequence? = null

        @StringRes
        private var messageRes: Int = DISABLED_RESOURCE_ID
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
                if (titleRes != DISABLED_RESOURCE_ID) setTitle(titleRes) else setTitle(title)
                val editText = view.findViewById<TextInputEditText>(R.id.preference_edit).apply {
                    if (textInputType != InputType.TYPE_NULL) {
                        inputType = textInputType
                    }
                    when {
                        textInputHintRes != DISABLED_RESOURCE_ID -> setHint(textInputHintRes)
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

                        messageRes != DISABLED_RESOURCE_ID -> {
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
                        PreferencesExtra.EDIT_DIALOG_REQUEST,
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

    fun interface OnTextChangeAfterListener {
        fun onTextAfterChange(preference: EditTextPreference, text: CharSequence?)
    }

    fun interface OnTextChangeBeforeListener {
        /**
         * Notified when the value of the connected [EditTextPreference] changes,
         * meaning after the user closes the dialog by pressing "ok".
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param text the new value
         *
         * @return true to commit the new value to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onTextBeforeChange(preference: EditTextPreference, text: CharSequence?): Boolean
    }
}
