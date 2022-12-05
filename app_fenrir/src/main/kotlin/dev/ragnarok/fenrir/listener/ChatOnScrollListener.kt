package dev.ragnarok.fenrir.listener

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener

class ChatOnScrollListener(private val fab: FloatingActionButton) :
    RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (!fab.isShown) {
                fab.show()
            }
        } else {
            if (fab.isShown) {
                fab.hide(object : OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton) {
                        super.onHidden(fab)
                        fab.visibility = View.INVISIBLE
                    }
                })
            }
        }
    }
}