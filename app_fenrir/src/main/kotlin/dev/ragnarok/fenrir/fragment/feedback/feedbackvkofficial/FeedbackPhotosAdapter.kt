package dev.ragnarok.fenrir.fragment.feedback.feedbackvkofficial

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with

class FeedbackPhotosAdapter(private var data: List<Photo>) :
    RecyclerView.Adapter<FeedbackPhotosAdapter.ViewHolder>() {
    private var photoSelectionListener: PhotoSelectionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root =
            LayoutInflater.from(parent.context).inflate(R.layout.item_feedback_photo, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val photo = data[position]
        with()
            .load(photo.getUrlForSize(PhotoSize.X, false))
            .tag(Constants.PICASSO_TAG)
            .placeholder(R.drawable.background_gray)
            .into(viewHolder.photoImageView)
        viewHolder.photoImageView.setOnClickListener {
            photoSelectionListener?.onPhotoClicked(viewHolder.bindingAdapterPosition, photo)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Photo>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setPhotoSelectionListener(photoSelectionListener: PhotoSelectionListener?) {
        this.photoSelectionListener = photoSelectionListener
    }

    interface PhotoSelectionListener {
        fun onPhotoClicked(position: Int, photo: Photo)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ShapeableImageView = itemView.findViewById(R.id.imageView)

    }
}