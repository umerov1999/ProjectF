package dev.ragnarok.filegallery.view.pager

import com.squareup.picasso3.Callback
import java.lang.ref.WeakReference

class WeakPicassoLoadCallback(baseCallback: Callback) : Callback {
    private val mReference: WeakReference<Callback> = WeakReference(baseCallback)
    override fun onSuccess() {
        mReference.get()?.onSuccess()
    }

    override fun onError(t: Throwable) {
        mReference.get()?.onError(t)
    }
}