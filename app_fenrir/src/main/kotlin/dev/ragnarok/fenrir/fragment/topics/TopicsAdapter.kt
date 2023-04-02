package dev.ragnarok.fenrir.fragment.topics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class TopicsAdapter(
    context: Context,
    topics: MutableList<Topic>,
    private val mActionListener: ActionListener
) : RecyclerBindableAdapter<Topic, TopicsAdapter.ViewHolder>(topics) {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var firstLastPadding = 0
    override fun onBindItemViewHolder(viewHolder: ViewHolder, position: Int, type: Int) {
        val item = getItem(position)
        val context = viewHolder.itemView.context
        viewHolder.title.text = item.title
        viewHolder.subtitle.text = context.getString(
            R.string.topic_comments_counter,
            AppTextUtils.getDateFromUnixTime(item.lastUpdateTime), item.commentsCount
        )
        viewHolder.itemView.setOnLongClickListener {
            val popup = PopupMenu(context, viewHolder.itemView)
            popup.inflate(R.menu.topics_item_menu)
            popup.setOnMenuItemClickListener { item1: MenuItem ->
                if (item1.itemId == R.id.copy_url) {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText(
                        context.getString(R.string.link),
                        "vk.com/topic" + item.ownerId + "_" + item.id
                    )
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(context).showToast(R.string.copied)
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.show()
            false
        }
        val avaUrl = item.updater?.maxSquareAvatar
        if (avaUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .into(viewHolder.creator)
        } else {
            with()
                .load(avaUrl)
                .transform(transformation)
                .into(viewHolder.creator)
        }
        viewHolder.itemView.setPadding(
            viewHolder.itemView.paddingLeft,
            if (position == 0) firstLastPadding else 0,
            viewHolder.itemView.paddingRight,
            if (position == itemCount - 1) firstLastPadding else 0
        )
        viewHolder.itemView.setOnClickListener {
            mActionListener.onTopicClick(
                item
            )
        }
    }

    override fun viewHolder(view: View, type: Int): ViewHolder {
        return ViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_topic
    }

    interface ActionListener {
        fun onTopicClick(topic: Topic)
    }

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val title: TextView = root.findViewById(R.id.item_topic_title)
        val subtitle: TextView = root.findViewById(R.id.item_topic_subtitle)
        val creator: ImageView = root.findViewById(R.id.item_topicstarter_avatar)
    }

    init {
        if (Utils.is600dp(context)) {
            firstLastPadding = Utils.dpToPx(16f, context).toInt()
        }
    }
}