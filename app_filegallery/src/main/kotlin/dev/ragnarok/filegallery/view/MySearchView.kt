package dev.ragnarok.filegallery.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dev.ragnarok.filegallery.Includes.stores
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.getParcelableCompat
import dev.ragnarok.filegallery.listener.TextWatcherAdapter
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.trimmedNonNullNoEmpty
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.ViewUtils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain

class MySearchView : FrameLayout {
    private var mQuery: String? = null
    private var mInput: MaterialAutoCompleteTextView? = null
    private var mSearchRoot: RoundCornerLinearView? = null
    private var mButtonBack: ImageView? = null
    private var mButtonClear: ImageView? = null
    private var mButtonAdditional: ImageView? = null
    private var mOnQueryChangeListener: OnQueryTextListener? = null
    private var mQueryDisposable = CancelableJob()
    private var listQueries = ArrayList<String?>()
    private var isFetchedListQueries = false
    private var searchId = 0
    private val mOnEditorActionListener = OnEditorActionListener { _, _, _ ->
        ViewUtils.keyboardHide(context)
        mInput?.clearFocus()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) {
            return
        }
        if (searchId > 0) {
            if (!isFetchedListQueries) {
                loadQueries()
            } else {
                updateQueriesAdapter()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mQueryDisposable.cancel()
    }

    private fun loadQueries() {
        mQueryDisposable.set(
            stores.searchQueriesStore().getQueries(searchId)
                .fromIOToMain({ s ->
                    isFetchedListQueries = true
                    listQueries.clear()
                    listQueries.addAll(s)
                    updateQueriesAdapter()
                }, { Log.e(TAG, it.localizedMessage.orEmpty()) })
        )
    }

    private fun updateQueriesAdapter() {
        val array = Array(listQueries.size) { listQueries[it] }
        val spinnerItems = ArrayAdapter(
            context,
            R.layout.search_dropdown_item,
            array
        )
        mInput?.setAdapter(spinnerItems)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        LayoutInflater.from(getContext()).inflate(R.layout.custom_searchview, this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.MySearchView)
        searchId = try {
            a.getInt(R.styleable.MySearchView_search_source_id, 0)
        } finally {
            a.recycle()
        }
        mSearchRoot = findViewById(R.id.search_root)
        mInput = findViewById(R.id.input)
        mInput?.setOnEditorActionListener(mOnEditorActionListener)
        mInput?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                mSearchRoot?.setStrokeWidth(Utils.dpf2(2f))
                mSearchRoot?.setViewColor(CurrentTheme.getColorPrimary(context))
            } else {
                mSearchRoot?.setStrokeWidth(Utils.dpf2(1f))
                mSearchRoot?.setViewColor(CurrentTheme.getDividerColorColor(context))
            }
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
        mButtonClear?.setOnClickListener {
            mInput?.clearFocus()
            ViewUtils.keyboardHide(context)
            clear()
        }
        mButtonBack?.setOnClickListener {
            mInput?.clearFocus()
            ViewUtils.keyboardHide(context)
            mOnBackButtonClickListener?.onBackButtonClick()
        }
        mButtonAdditional?.setOnClickListener {
            mInput?.clearFocus()
            ViewUtils.keyboardHide(context)
            mOnAdditionalButtonClickListener?.onAdditionalButtonClick()
        }
        mButtonAdditional?.setOnLongClickListener {
            mInput?.clearFocus()
            ViewUtils.keyboardHide(context)
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
            if (searchId > 0) {
                mQueryDisposable.set(
                    stores.searchQueriesStore().insertQuery(searchId, query.toString())
                        .fromIOToMain({
                            if (!listQueries.contains(query.toString())) {
                                listQueries.add(0, query.toString())
                                updateQueriesAdapter()
                            }
                        }, { Log.e(TAG, it.localizedMessage.orEmpty()) })
                )
            }
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
        mButtonClear?.visibility =
            if (mQuery.isNullOrEmpty()) GONE else VISIBLE
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = Bundle()
        state.putParcelable("PARENT", superState)
        state.putString("query", mQuery)
        state.putStringArrayList("listQueries", listQueries)
        state.putBoolean("isFetchedListQueries", isFetchedListQueries)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as Bundle
        val superState = savedState.getParcelableCompat<Parcelable>("PARENT")
        super.onRestoreInstanceState(superState)
        mQuery = savedState.getString("query")
        mInput?.setText(mQuery)

        listQueries.clear()
        savedState.getStringArrayList("listQueries")?.let { listQueries.addAll(it) }
        isFetchedListQueries = state.getBoolean("isFetchedListQueries")
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

    interface OnQueryTextListener {
        fun onQueryTextSubmit(query: String?): Boolean
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
        private val TAG = MySearchView::class.simpleName.orEmpty()
    }
}
