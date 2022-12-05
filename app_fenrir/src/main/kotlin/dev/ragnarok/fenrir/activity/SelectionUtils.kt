package dev.ragnarok.fenrir.activity

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils

object SelectionUtils {
    private val TAG = SelectionUtils::class.java.simpleName
    private const val VIEW_TAG = "SelectionUtils.SelectionView"


    fun addSelectionProfileSupport(context: Context?, root: ViewGroup?, mayBeUser: Any) {
        if (context !is ProfileSelectable || root == null) return
        val criteria = (context as ProfileSelectable).acceptableCriteria
        var canSelect =
            if (criteria?.getIsPeopleOnly() == true) mayBeUser is User else mayBeUser is Owner || mayBeUser is FavePage
        if (canSelect && criteria?.getOwnerType() == SelectProfileCriteria.OwnerType.ONLY_FRIENDS) {
            assert(mayBeUser is User)
            canSelect = (mayBeUser as User).isFriend
        }
        val callack = context as ProfileSelectable
        var selectionView =
            root.findViewWithTag<ImageView>(VIEW_TAG)
        if (!canSelect && selectionView == null) return
        if (canSelect && selectionView == null) {
            selectionView = ImageView(context)
            selectionView.setImageResource(R.drawable.plus)
            selectionView.tag = VIEW_TAG
            selectionView.setBackgroundResource(R.drawable.circle_back)
            selectionView.background.alpha = 150
            selectionView.layoutParams = createLayoutParams(root)
            val dp4px = Utils.dpToPx(4f, context).toInt()
            selectionView.setPadding(dp4px, dp4px, dp4px, dp4px)
            Logger.d(TAG, "Added new selectionView")
            root.addView(selectionView)
        } else {
            Logger.d(TAG, "Re-use selectionView")
        }
        selectionView?.visibility = if (canSelect) View.VISIBLE else View.GONE
        if (!canSelect) {
            selectionView.setOnClickListener(null)
        } else {
            if (mayBeUser is FavePage && mayBeUser.owner != null) {
                mayBeUser.owner?.let { vv ->
                    selectionView.setOnClickListener { callack.select(vv) }
                }
            } else if (mayBeUser is Owner) {
                selectionView.setOnClickListener {
                    callack.select(
                        mayBeUser
                    )
                }
            }
        }
    }

    private fun createLayoutParams(parent: ViewGroup): ViewGroup.LayoutParams {
        return if (parent is FrameLayout) {
            val margin = Utils.dpToPx(6f, parent.getContext()).toInt()
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            params.bottomMargin = margin
            params.leftMargin = margin
            params.rightMargin = margin
            params.topMargin = margin
            params
        } else {
            throw IllegalArgumentException("Not yet impl for parent: " + parent.javaClass.simpleName)
        }
    }
}