package dev.ragnarok.fenrir.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.disposables.Disposable

class MySearchView : LinearLayout {
    private var mQuery: String? = null
    private var mInput: MaterialAutoCompleteTextView? = null
    private var mButtonBack: ImageView? = null
    private var mButtonClear: ImageView? = null
    private var mButtonAdditional: ImageView? = null
    private var mOnQueryChangeListener: OnQueryTextListener? = null
    private var mQueryDisposable = Disposable.disposed()
    private lateinit var listQueries: ArrayAdapter<String>
    private var searchId = 0
    private val mOnEditorActionListener = OnEditorActionListener { _, actionId, event ->

        /**
         * Called when the input method default action key is pressed.
         */
        /**
         * Called when the input method default action key is pressed.
         */
        Logger.d(TAG, "onEditorAction, actionId: $actionId, event: $event")
        onSubmitQuery()
        true
    }
    private var mOnBackButtonClickListener: OnBackButtonClickListener? = null
    private var mOnAdditionalButtonClickListener: OnAdditionalButtonClickListener? = null
    private var mOnAdditionalButtonLongClickListener: OnAdditionalButtonLongClickListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mQueryDisposable.dispose()
    }

    private fun loadQueries() {
        mQueryDisposable.dispose()
        mQueryDisposable = Stores.instance.tempStore().getSearchQueries(searchId)
            .fromIOToMain()
            .subscribe({ s ->
                listQueries.clear()
                listQueries.addAll(s)
            }, RxUtils.ignore())
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        LayoutInflater.from(getContext()).inflate(R.layout.custom_searchview, this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.MySearchView)
        searchId = try {
            a.getInt(R.styleable.MySearchView_search_source_id, id)
        } finally {
            a.recycle()
        }
        mInput = findViewById(R.id.input)
        mInput?.setOnEditorActionListener(mOnEditorActionListener)
        listQueries = ArrayAdapter(getContext(), R.layout.search_dropdown_item)
        mInput?.setAdapter(listQueries)
        if (!isInEditMode) {
            loadQueries()
        }
        mButtonBack = findViewById(R.id.button_back)
        mButtonClear = findViewById(R.id.clear)
        mButtonAdditional = findViewById(R.id.additional)
        mInput?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                mQuery = s.toString()
                mOnQueryChangeListener?.onQueryTextChange(s.toString())
                resolveCloseButton()
            }
        })
        mButtonClear?.setOnClickListener { clear() }
        mButtonBack?.setOnClickListener {
            mOnBackButtonClickListener?.onBackButtonClick()
        }
        mButtonAdditional?.setOnClickListener {
            mOnAdditionalButtonClickListener?.onAdditionalButtonClick()
        }
        mButtonAdditional?.setOnLongClickListener {
            mOnAdditionalButtonLongClickListener?.onAdditionalButtonLongClick()
            true
        }
        resolveCloseButton()
    }

    val text: Editable?
        get() = mInput?.text

    fun clear() {
        mInput?.text?.clear()
    }

    private fun onSubmitQuery() {
        val query: CharSequence? = mInput?.text
        if (query.trimmedNonNullNoEmpty()) {
            mQueryDisposable.dispose()
            mQueryDisposable =
                Stores.instance.tempStore().insertSearchQuery(searchId, query.toString())
                    .fromIOToMain()
                    .subscribe({ loadQueries() }, RxUtils.ignore())
            if (mOnQueryChangeListener != null && mOnQueryChangeListener?.onQueryTextSubmit(query.toString()) == true) {
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    fun setRightButtonVisibility(visible: Boolean) {
        mButtonAdditional?.visibility =
            if (visible) VISIBLE else GONE
    }

    fun activateKeyboard() {
        mInput?.requestFocus()
        mInput?.postDelayed({
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager?.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT)
        }, 500)
    }

    internal fun resolveCloseButton() {
        val empty = mQuery.isNullOrEmpty()
        Logger.d(TAG, "resolveCloseButton, empty: $empty")
        mButtonClear?.visibility =
            if (mQuery.isNullOrEmpty()) GONE else VISIBLE
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = Bundle()
        state.putParcelable("PARENT", superState)
        state.putString("query", mQuery)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as Bundle
        val superState = savedState.getParcelableCompat<Parcelable>("PARENT")
        super.onRestoreInstanceState(superState)
        mQuery = savedState.getString("query")
        mInput?.setText(mQuery)
    }

    fun setOnQueryTextListener(onQueryChangeListener: OnQueryTextListener?) {
        mOnQueryChangeListener = onQueryChangeListener
    }

    fun setOnBackButtonClickListener(onBackButtonClickListener: OnBackButtonClickListener?) {
        mOnBackButtonClickListener = onBackButtonClickListener
    }

    fun setOnAdditionalButtonClickListener(onAdditionalButtonClickListener: OnAdditionalButtonClickListener?) {
        mOnAdditionalButtonClickListener = onAdditionalButtonClickListener
    }

    fun setOnAdditionalButtonLongClickListener(onAdditionalButtonLongClickListener: OnAdditionalButtonLongClickListener?) {
        mOnAdditionalButtonLongClickListener = onAdditionalButtonLongClickListener
    }

    fun setQuery(query: String?, quetly: Boolean) {
        val tmp = mOnQueryChangeListener
        if (quetly) {
            mOnQueryChangeListener = null
        }
        setQuery(query)
        if (quetly) {
            mOnQueryChangeListener = tmp
        }
    }

    fun setQuery(query: String?) {
        mInput?.setText(query)
    }

    fun setSelection(start: Int, end: Int) {
        mInput?.setSelection(start, end)
    }

    fun setSelection(position: Int) {
        mInput?.setSelection(position)
    }

    fun setLeftIcon(@DrawableRes drawable: Int) {
        mButtonBack?.setImageResource(drawable)
    }

    fun setLeftIconTint(@ColorInt color: Int) {
        Utils.setTint(mButtonBack, color)
    }

    fun setRightIconTint(@ColorInt color: Int) {
        Utils.setTint(mButtonAdditional, color)
    }

    fun setLeftIcon(drawable: Drawable?) {
        mButtonBack?.setImageDrawable(drawable)
    }

    fun setRightIcon(drawable: Drawable?) {
        mButtonAdditional?.setImageDrawable(drawable)
    }

    fun setRightIcon(@DrawableRes drawable: Int) {
        mButtonAdditional?.setImageResource(drawable)
    }

    /**
     * Callbacks for changes to the query text.
     */
    interface OnQueryTextListener {
        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        fun onQueryTextSubmit(query: String?): Boolean

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        fun onQueryTextChange(newText: String?): Boolean
    }

    interface OnBackButtonClickListener {
        fun onBackButtonClick()
    }

    interface OnAdditionalButtonClickListener {
        fun onAdditionalButtonClick()
    }

    interface OnAdditionalButtonLongClickListener {
        fun onAdditionalButtonLongClick()
    }

    companion object {
        private val TAG = MySearchView::class.java.simpleName
    }
}
