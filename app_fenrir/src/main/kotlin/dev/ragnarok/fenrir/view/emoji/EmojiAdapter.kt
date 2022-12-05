package dev.ragnarok.fenrir.view.emoji

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.OnEmojiconClickedListener
import dev.ragnarok.fenrir.view.emoji.section.Emojicon

internal class EmojiAdapter(context: Context, data: Array<Emojicon>) : ArrayAdapter<Emojicon>(
    context, R.layout.emojicon_item, data
) {
    private var emojiClickListener: OnEmojiconClickedListener? = null
    fun setEmojiClickListener(listener: OnEmojiconClickedListener) {
        emojiClickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        if (v == null) {
            v = LayoutInflater.from(parent.context).inflate(R.layout.emojicon_item, parent, false)
            val holder = ViewHolder()
            holder.icon = v.findViewById(R.id.emojicon_icon)
            v.tag = holder
        }
        val emoji = getItem(position)
        val holder = v?.tag as ViewHolder?
        holder?.icon?.setText(emoji?.emoji, TextView.BufferType.SPANNABLE)
        holder?.icon?.setOnClickListener {
            getItem(position)?.let { it1 ->
                emojiClickListener?.onEmojiconClicked(
                    it1
                )
            }
        }
        return v!!
    }

    internal class ViewHolder {
        var icon: TextView? = null
    }
}