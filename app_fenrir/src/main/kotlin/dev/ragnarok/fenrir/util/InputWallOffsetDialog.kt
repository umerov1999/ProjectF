package dev.ragnarok.fenrir.util

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

class InputWallOffsetDialog internal constructor(
    private val context: Context,
    private val accountId: Int,
    private val ownerId: Int,
    private val wallFilter: Int
) {
    private var titleRes = 0
    private var value: Int = 0
    private var callback: Callback? = null
    private var disposable: Disposable = Disposable.disposed()
    private var onHasResult: Boolean = false
    fun show() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(titleRes)
        val view = View.inflate(context, R.layout.dialog_enter_offset, null)
        val input: TextInputEditText = view.findViewById(R.id.editText)
        val dt: TextView = view.findViewById(R.id.datePost)
        input.setText(value.toString())
        input.setSelection(input.text?.length ?: 0)
        builder.setView(view)
        builder.setPositiveButton(R.string.button_ok) { dialog: DialogInterface, _: Int ->
            input.error = null
            var newValue: Int = -1
            try {
                newValue = input.text.toString().trim { it <= ' ' }.toInt()
            } catch (e: Exception) {
                input.error = e.localizedMessage
                input.requestFocus()
            }
            if (newValue < 0) {
                input.error = context.getString(R.string.field_is_required)
                input.requestFocus()
            } else {
                try {
                    callback?.onChanged(newValue)
                    disposable.dispose()
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
        input.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                disposable.dispose()
                try {
                    val mQuery = s.toString().trim().toInt()
                    if (mQuery >= 0) {
                        disposable = Repository.walls.getWall(
                            accountId,
                            ownerId,
                            mQuery,
                            1,
                            wallFilter,
                            false
                        ).delay(1, TimeUnit.SECONDS).fromIOToMain()
                            .subscribe({
                                if (it.isEmpty()) {
                                    dt.setText(R.string.list_is_empty)
                                } else {
                                    dt.text =
                                        AppTextUtils.getDateFromUnixTimeShorted(context, it[0].date)
                                }
                            }, RxUtils.ignore())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    interface Callback {
        fun onChanged(newValue: Int)
        fun onCanceled()
    }

    class Builder(
        private val context: Context,
        private val accountId: Int,
        private val ownerId: Int,
        private val wallFilter: Int
    ) {
        private var titleRes = 0
        private var value: Int = 0
        private var callback: Callback? = null

        fun setTitleRes(titleRes: Int): Builder {
            this.titleRes = titleRes
            return this
        }

        fun setValue(value: Int): Builder {
            this.value = value
            return this
        }

        fun setCallback(callback: Callback?): Builder {
            this.callback = callback
            return this
        }

        fun create(): InputWallOffsetDialog {
            val inputTextDialog = InputWallOffsetDialog(context, accountId, ownerId, wallFilter)
            inputTextDialog.titleRes = titleRes
            inputTextDialog.value = value
            inputTextDialog.callback = callback
            return inputTextDialog
        }

        fun show() {
            create().show()
        }
    }
}
