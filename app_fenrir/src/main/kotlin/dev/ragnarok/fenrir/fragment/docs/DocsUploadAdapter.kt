package dev.ragnarok.fenrir.fragment.docs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder
import dev.ragnarok.fenrir.fragment.base.holder.SharedHolders
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.view.CircleRoadProgress

class DocsUploadAdapter(
    private var data: List<Upload>,
    private val actionListener: ActionListener
) : RecyclerView.Adapter<DocsUploadAdapter.Holder>() {
    private val sharedHolders: SharedHolders<Holder> = SharedHolders(false)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.doc_upload_entry, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val upload = data[position]
        sharedHolders.put(position, holder)
        val inProgress = upload.status == Upload.STATUS_UPLOADING
        holder.progress.visibility = if (inProgress) View.VISIBLE else View.INVISIBLE
        if (inProgress) {
            holder.progress.changePercentage(upload.progress)
        } else {
            holder.progress.changePercentage(0)
        }
        @ColorInt var titleColor = CurrentTheme.getPrimaryTextColorCode(holder.title.context)
        when (upload.status) {
            Upload.STATUS_UPLOADING -> {
                val precentText = upload.progress.toString() + "%"
                holder.status.text = precentText
            }

            Upload.STATUS_CANCELLING -> holder.status.setText(R.string.cancelling)
            Upload.STATUS_QUEUE -> holder.status.setText(R.string.in_order)
            Upload.STATUS_ERROR -> {
                holder.status.setText(R.string.error)
                titleColor = ERROR_COLOR
            }
        }
        holder.status.setTextColor(titleColor)
        holder.title.text = upload.fileUri.toString()
        holder.buttonDelete.setOnClickListener { actionListener.onRemoveClick(upload) }
    }

    fun changeUploadProgress(position: Int, progress: Int, smoothly: Boolean) {
        val holder = sharedHolders.findOneByEntityId(position)
        if (holder != null) {
            val precentText = "$progress%"
            holder.status.text = precentText
            if (smoothly) {
                holder.progress.changePercentageSmoothly(progress)
            } else {
                holder.progress.changePercentage(progress)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Upload>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface ActionListener {
        fun onRemoveClick(upload: Upload)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView), IdentificableHolder {
        val title: TextView = itemView.findViewById(R.id.title)
        val status: TextView = itemView.findViewById(R.id.status)
        val buttonDelete: View = itemView.findViewById(R.id.progress_root)
        val progress: CircleRoadProgress = itemView.findViewById(R.id.progress_view)
        override val holderId: Int
            get() = itemView.tag as Int

        //ImageView image;
        init {
            //this.image = (ImageView) itemView.findViewById(R.id.image);
            itemView.tag = generateNextHolderId()
        }
    }

    companion object {
        private val ERROR_COLOR = Color.parseColor("#ff0000")
        private var idGenerator = 0
        internal fun generateNextHolderId(): Int {
            idGenerator++
            return idGenerator
        }
    }

}