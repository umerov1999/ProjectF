package dev.ragnarok.fenrir.util

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R

class InputTextDialog internal constructor(val context: Context) {
    private var inputType = 0
    private var titleRes = 0
    private var value: String? = null
    private var allowEmpty = false
    private var target: TextView? = null
    private var callback: Callback? = null
    private var validator: Validator? = null
    private var hint: Int? = null
    private var onHasResult: Boolean = false
    fun show() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(titleRes)
        val view = View.inflate(context, R.layout.dialog_enter_text, null)
        val input: TextInputEditText = view.findViewById(R.id.editText)
        input.setText(value)
        input.setSelection(input.text?.length ?: 0)
        hint?.let {
            input.setHint(it)
        }
        input.inputType = inputType
        builder.setView(view)
        builder.setPositiveButton(R.string.button_ok) { dialog: DialogInterface, _: Int ->
            input.error = null
            val newValue = input.text.toString().trim { it <= ' ' }
            if (newValue.isEmpty() && !allowEmpty) {
                input.error = context.getString(R.string.field_is_required)
                input.requestFocus()
            } else {
                try {
                    validator?.validate(newValue)
                    callback?.onChanged(newValue)
                    target?.text = newValue
                    onHasResult = true
                    dialog.dismiss()
                } catch (e: IllegalArgumentException) {
                    input.error = e.message
                    input.requestFocus()
                }
            }
        }
        builder.setNegativeButton(R.string.button_cancel) { dialog: DialogInterface, _: Int ->
            callback?.onCanceled()
            dialog.dismiss()
        }
        builder.setOnDismissListener {
            if (!onHasResult) {
                callback?.onCanceled()
            }
        }

        builder.show()
        input.requestFocus()
        input.postDelayed({
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 500)
    }

    interface Callback {
        fun onChanged(newValue: String?)
        fun onCanceled()
    }

    interface Validator {
        @Throws(IllegalArgumentException::class)
        fun validate(value: String?)
    }

    class Builder(private val context: Context) {
        private var inputType = 0
        private var titleRes = 0
        private var value: String? = null
        private var allowEmpty = false
        private var target: TextView? = null
        private var callback: Callback? = null
        private var validator: Validator? = null
        private var hint: Int? = null
        fun setInputType(inputType: Int): Builder {
            this.inputType = inputType
            return this
        }

        fun setTitleRes(titleRes: Int): Builder {
            this.titleRes = titleRes
            return this
        }

        fun setValue(value: String?): Builder {
            this.value = value
            return this
        }

        fun setAllowEmpty(allowEmpty: Boolean): Builder {
            this.allowEmpty = allowEmpty
            return this
        }

        fun setTarget(target: TextView?): Builder {
            this.target = target
            return this
        }

        fun setCallback(callback: Callback?): Builder {
            this.callback = callback
            return this
        }

        fun setValidator(validator: Validator?): Builder {
            this.validator = validator
            return this
        }

        fun create(): InputTextDialog {
            val inputTextDialog = InputTextDialog(context)
            inputTextDialog.inputType = inputType
            inputTextDialog.titleRes = titleRes
            inputTextDialog.value = value
            inputTextDialog.allowEmpty = allowEmpty
            inputTextDialog.target = target
            inputTextDialog.callback = callback
            inputTextDialog.validator = validator
            inputTextDialog.hint = hint
            return inputTextDialog
        }

        fun setHint(hint: Int?): Builder {
            this.hint = hint
            return this
        }

        fun show() {
            create().show()
        }
    }
}