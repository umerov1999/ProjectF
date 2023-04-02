package dev.ragnarok.fenrir.fragment.fave.favearticles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class FaveArticlesAdapter(private var data: List<Article>, private val context: Context) :
    RecyclerView.Adapter<FaveArticlesAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_article, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val article = data[position]
        holder.btFave.setImageResource(R.drawable.favorite)
        holder.btFave.setOnClickListener {
            clickListener?.onDelete(
                holder.bindingAdapterPosition,
                article
            )
        }
        if (article.uRL != null) {
            holder.ivButton.visibility = View.VISIBLE
            holder.ivButton.setOnClickListener {
                article.uRL.nonNullNoEmpty {
                    clickListener?.onArticleClick(article)
                }
            }
        } else holder.ivButton.visibility = View.GONE
        holder.btShare.setOnClickListener { clickListener?.onShare(article) }
        var photo_url: String? = null
        if (article.photo != null) {
            photo_url =
                article.photo?.getUrlForSize(Settings.get().main().prefPreviewImageSize, false)
        }
        if (photo_url != null) {
            holder.ivPhoto.visibility = View.VISIBLE
            displayAvatar(holder.ivPhoto, null, photo_url, Constants.PICASSO_TAG)
            holder.ivPhoto.setOnLongClickListener {
                article.photo?.let { it1 -> clickListener?.onPhotosOpen(it1) }
                true
            }
        } else holder.ivPhoto.visibility = View.GONE
        if (article.subTitle != null) {
            holder.ivSubTitle.visibility = View.VISIBLE
            holder.ivSubTitle.text = article.subTitle
        } else holder.ivSubTitle.visibility = View.GONE
        if (article.title != null) {
            holder.ivTitle.visibility = View.VISIBLE
            holder.ivTitle.text = article.title
        } else holder.ivTitle.visibility = View.GONE
        if (article.ownerName != null) {
            holder.ivName.visibility = View.VISIBLE
            holder.ivName.text = article.ownerName
        } else holder.ivName.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Article>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onArticleClick(article: Article)
        fun onPhotosOpen(photo: Photo)
        fun onDelete(index: Int, article: Article)
        fun onShare(article: Article)
    }

    class Holder(root: View) : RecyclerView.ViewHolder(root) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.item_article_image)
        val btFave: ImageView = itemView.findViewById(R.id.item_article_to_fave)
        val btShare: ImageView = itemView.findViewById(R.id.item_article_share)
        val ivSubTitle: TextView = itemView.findViewById(R.id.item_article_subtitle)
        val ivTitle: TextView = itemView.findViewById(R.id.item_article_title)
        val ivName: TextView = itemView.findViewById(R.id.item_article_name)
        val ivButton: Button = itemView.findViewById(R.id.item_article_read)
    }
}