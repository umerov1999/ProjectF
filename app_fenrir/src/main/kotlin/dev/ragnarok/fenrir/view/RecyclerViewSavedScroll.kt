package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("CustomViewStyleable", "PrivateResource")
class RecyclerViewSavedScroll @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    private var uid: Int = -1

    fun updateUid(uid: Int?) {
        this.uid = uid ?: -1
        if (uid == -1) {
            return
        }
        parcelables[uid]?.let {
            try {
                layoutManager?.onRestoreInstanceState(it)
            } catch (_: Exception) {
            }
        }
    }

    fun saveState() {
        if (uid == -1) {
            return
        }
        parcelables[uid] = layoutManager?.onSaveInstanceState()
    }

    override fun onDetachedFromWindow() {
        if (uid == -1) {
            super.onDetachedFromWindow()
            return
        }
        parcelables[uid] = layoutManager?.onSaveInstanceState()
        super.onDetachedFromWindow()
    }

    companion object {
        private var parcelables = HashMap<Int, Parcelable?>()
    }
}
