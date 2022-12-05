package dev.ragnarok.fenrir.view

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.google.android.material.progressindicator.CircularProgressIndicator
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LoadMoreState

class LoadMoreFooterHelperComment {
    var callback: Callback? = null
    var holder: Holder? = null
    var state = LoadMoreState.INVISIBLE
    fun setEndOfListTextRes(@StringRes res: Int) {
        holder?.tvEndOfList?.setText(res)
    }

    fun setEndOfListText(text: String?) {
        holder?.tvEndOfList?.text = text
    }

    fun switchToState(@LoadMoreState state: Int) {
        this.state = state
        holder?.container?.visibility =
            if (state == LoadMoreState.INVISIBLE) View.GONE else View.VISIBLE
        when (state) {
            LoadMoreState.LOADING -> {
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.bLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.VISIBLE
            }
            LoadMoreState.END_OF_LIST -> {
                holder?.tvEndOfList?.visibility = View.VISIBLE
                holder?.bLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }
            LoadMoreState.CAN_LOAD_MORE -> {
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.bLoadMore?.visibility = View.VISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }
            LoadMoreState.INVISIBLE -> {}
        }
    }

    interface Callback {
        fun onLoadMoreClick()
    }

    class Holder(root: View) {
        val container: View = root.findViewById(R.id.footer_load_more_root)
        val progress: CircularProgressIndicator = root.findViewById(R.id.footer_load_more_progress)
        val bLoadMore: View = root.findViewById(R.id.footer_load_more_run)
        val tvEndOfList: TextView = root.findViewById(R.id.footer_load_more_end_of_list)

    }

    companion object {

        fun createFrom(view: View, callback: Callback?): LoadMoreFooterHelperComment {
            val helper = LoadMoreFooterHelperComment()
            helper.holder = Holder(view)
            helper.callback = callback
            helper.holder?.bLoadMore?.setOnClickListener { callback?.onLoadMoreClick() }
            return helper
        }
    }
}