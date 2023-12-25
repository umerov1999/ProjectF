package de.maxr1998.modernpreferences.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID


class MultiLineEditTextPreference(key: String, fragmentManager: FragmentManager) :
    DialogPreference(key, fragmentManager) {

    var currentInput: CharSequence? = null
        private set

    private var textInputType: Int =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE

    @StringRes
    var textInputHintRes: Int = DISABLED_RESOURCE_ID
    var textInputHint: CharSequence? = null
    var isSpace: Boolean = false

    @StringRes
    var messageRes: Int = DISABLED_RESOURCE_ID
    var message: CharSequence? = null
    var defaultValue: Set<String>? = null
    var multiLineTextChangeBeforeListener: OnMultiLineChangeBeforeListener? = null
    var multiLineTextChangeAfterListener: OnMultiLineChangeAfterListener? = null

    override fun onAttach() {
        super.onAttach()
        val c = getStringSet() ?: defaultValue
        if (currentInput == null && c != null) {
            var first = true
            val str: StringBuilder = StringBuilder()
            for (i in c) {
                if (first) {
                    first = false
                } else {
                    str.append(if (!isSpace) "\r\n" else " ")
                }
                str.append(i)
            }
            currentInput = str.toString()
        }
    }

    fun copyMultiLine(o: MultiLineEditTextPreference): MultiLineEditTextPreference {
        textInputType = o.textInputType
        textInputHintRes = o.textInputHintRes
        messageRes = o.messageRes
        message = o.message
        defaultValue = o.defaultValue
        multiLineTextChangeAfterListener = o.multiLineTextChangeAfterListener
        multiLineTextChangeBeforeListener = o.multiLineTextChangeBeforeListener
        isSpace = o.isSpace
        return this
    }

    fun reload() {
        val c = getStringSet() ?: defaultValue
        if (currentInput == null && c != null) {
            var first = true
            val str: StringBuilder = StringBuilder()
            for (i in c) {
                if (first) {
                    first = false
                } else {
                    str.append(if (!isSpace) "\r\n" else " ")
                }
                str.append(i)
            }
            currentInput = str.toString()
        }
        requestRebind()
    }

    fun persist(input: CharSequence?) {
        var p: Set<String>? = null
        if (input != null) {
            var l = input.trim().toString()
            for (i in input.trim().toString()) {
                if (i < ' ' && isSpace || i < ' ' && i != '\n') {
                    l = l.replace(i, if (isSpace) ' ' else '\n')
                }
            }
            val tmp: ArrayList<String> = ArrayList()
            for (i in l.split(if (isSpace) ' ' else '\n')) {
                if (i.trim { it <= ' ' }.isNotEmpty()) {
                    tmp.add(i.trim { it <= ' ' })
                }
            }
            p = tmp.toHashSet()
        }
        if (multiLineTextChangeBeforeListener?.onMultiLineTextBeforeChange(this, p) != false) {
            commitStringSet(p)
            currentInput = null
            reload()
            requestRebind()
            multiLineTextChangeAfterListener?.onMultiLineTextAfterChange(this, p)
        }
    }

    override fun resolveSummary(context: Context): CharSequence? {
        return currentInput ?: super.resolveSummary(context)
    }

    override fun createAndShowDialogFragment() {
        MultiLineEditTextDialog.newInstance(
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
        ).show(fragmentManager, "MultiLineEditTextDialog")
    }

    class MultiLineEditTextDialog : DialogFragment() {
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
            ): MultiLineEditTextDialog {
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
                val dialog = MultiLineEditTextDialog()
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
                        PreferencesExtra.MULTI_LINE_EDIT_DIALOG_REQUEST,
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

    fun interface OnMultiLineChangeAfterListener {
        fun onMultiLineTextAfterChange(
            preference: MultiLineEditTextPreference,
            data: Set<String>?
        )
    }

    fun interface OnMultiLineChangeBeforeListener {
        /**
         * Notified when the value of the connected [MultiLineEditTextPreference] changes,
         * meaning after the user closes the dialog by pressing "ok".
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param data the new value
         *
         * @return true to commit the new value to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onMultiLineTextBeforeChange(
            preference: MultiLineEditTextPreference,
            data: Set<String>?
        ): Boolean
    }
}
