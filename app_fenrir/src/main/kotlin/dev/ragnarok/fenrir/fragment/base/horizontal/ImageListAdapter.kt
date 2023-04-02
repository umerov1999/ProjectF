package dev.ragnarok.fenrir.fragment.base.horizontal

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

class ImageListAdapter : RecyclerView.Adapter<ImageListAdapter.ImageVewHolder>() {

    private var mData: List<Photo> = arrayListOf()
    private var mCurrentSelectedIndex = -1
    private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVewHolder {
        mContext = parent.context
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_preview_image, parent, false)
        return ImageVewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ImageVewHolder, position: Int) {
        holder.bind(mData[position], position)
    }

    fun getSize(): Int {
        return mData.size
    }

    fun setData(data: List<Photo>) {
        mCurrentSelectedIndex = if (data.isNotEmpty())
            0
        else
            -1
        mData = data
        notifyDataSetChanged()
    }

    fun selectPosition(index: Int) {
        if (mCurrentSelectedIndex >= 0) {
            val prevIndex = mCurrentSelectedIndex
            mCurrentSelectedIndex = index
            notifyItemChanged(prevIndex)
            notifyItemChanged(index)
        } else {
            mCurrentSelectedIndex = index
            notifyItemChanged(index)
        }
    }

    fun getSelectedItem(): Int {
        return mCurrentSelectedIndex
    }

    inner class ImageVewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var item_image_iv: ShapeableImageView? = null

        init {
            item_image_iv = itemView.findViewById(R.id.item_image_iv)
        }

        fun bind(pData: Photo, position: Int) {
            val previewUrl: String? = pData.getUrlForSize(PhotoSize.M, false)
            if (previewUrl.nonNullNoEmpty()) {
                item_image_iv?.let {
                    PicassoInstance.with()
                        .load(previewUrl)
                        .into(it)
                }
            } else {
                item_image_iv?.let { PicassoInstance.with().cancelRequest(it) }
            }

            itemView.setOnClickListener {
                mOnRecyclerImageClickListener?.onRecyclerImageClick(bindingAdapterPosition)
                selectPosition(bindingAdapterPosition)
            }

            if (mCurrentSelectedIndex == position)
                setSelected(true)
            else
                setSelected(false)
        }

        private fun setSelected(isSelected: Boolean) {
            if (isSelected) {
                Utils.setColorFilter(item_image_iv, CurrentTheme.getColorSecondary(mContext))
            } else {
                item_image_iv?.clearColorFilter()
            }
        }
    }

    private var mOnRecyclerImageClickListener: OnRecyclerImageClickListener? = null

    fun setListener(listener: OnRecyclerImageClickListener) {
        mOnRecyclerImageClickListener = listener
    }

    interface OnRecyclerImageClickListener {
        fun onRecyclerImageClick(index: Int)
    }
}
