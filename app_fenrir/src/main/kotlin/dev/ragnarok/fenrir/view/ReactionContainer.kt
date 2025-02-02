package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isNotEmpty
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.ReactionWithAsset
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView

class ReactionContainer : RowLayout {
    private var colorPrimary = 0
    private var colorOnPrimary = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }
        colorPrimary = CurrentTheme.getColorPrimary(context)
        colorOnPrimary = CurrentTheme.getColorOnPrimary(context)
    }

    fun displayReactions(
        isEdit: Boolean,
        myReactionSend: Int,
        reactionsData: List<ReactionWithAsset>?,
        conversation_message_id: Int, peerId: Long,
        listener: ReactionClicked?
    ) {
        if (reactionsData.isNullOrEmpty()) {
            if (isNotEmpty()) {
                removeAllViews()
            }
            visibility = GONE
            return
        }
        visibility = VISIBLE
        val i = reactionsData.size - childCount
        (0 until i).forEach { j ->
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.item_reaction, this, false)
            val holder = ReactionHolder(itemView)
            itemView.tag = holder
            addView(itemView)
        }
        if (childCount > reactionsData.size) {
            removeViews(reactionsData.size, childCount - reactionsData.size)
        }
        for (g in reactionsData.indices) {
            val root = getChildAt(g) as ViewGroup? ?: continue
            val reaction = reactionsData[g]
            val reactionHolder: ReactionHolder = root.tag as ReactionHolder? ?: continue
            if (reaction.reaction_id == myReactionSend) {
                reactionHolder.tvCount.setTextColor(colorOnPrimary)
                Utils.setBackgroundTint(root, colorPrimary)
            } else {
                reactionHolder.tvCount.setTextColor(colorPrimary)
                Utils.setBackgroundTint(root, colorOnPrimary)
            }
            ViewUtils.setCountText(reactionHolder.tvCount, reaction.count, false)
            if (reaction.small_animation.isNullOrEmpty()) {
                reactionHolder.ivReaction.setImageDrawable(null)
            } else {
                reactionHolder.ivReaction.fromNet(
                    reaction.small_animation,
                    Utils.createOkHttp(Constants.GIF_TIMEOUT, true),
                    !isEdit
                )
            }
            root.setOnClickListener {
                if (isEdit) {
                    val res: Int? =
                        if (reaction.reaction_id != myReactionSend) reaction.reaction_id else null
                    listener?.onReactionClicked(res, conversation_message_id, peerId)
                }
            }
        }
    }

    interface ReactionClicked {
        fun onReactionClicked(reaction_id: Int?, conversation_message_id: Int, peerId: Long)
    }

    private inner class ReactionHolder(root: View) {
        val tvCount: TextView = root.findViewById(R.id.count)
        val ivReaction: ThorVGLottieView = root.findViewById(R.id.reaction)
    }
}
