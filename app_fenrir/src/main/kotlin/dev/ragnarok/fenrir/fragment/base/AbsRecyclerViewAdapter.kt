package dev.ragnarok.fenrir.fragment.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.fragment.base.listener.OwnerClickListener

abstract class AbsRecyclerViewAdapter<H : RecyclerView.ViewHolder> : RecyclerView.Adapter<H>() {
    private var ownerClickListener: OwnerClickListener? = null
    fun setOwnerClickListener(ownerClickListener: OwnerClickListener?) {
        this.ownerClickListener = ownerClickListener
    }

    protected fun addOwnerAvatarClickHandling(view: View, ownerId: Int) {
        view.setOnClickListener {
            ownerClickListener?.onOwnerClick(ownerId)
        }
    }
}