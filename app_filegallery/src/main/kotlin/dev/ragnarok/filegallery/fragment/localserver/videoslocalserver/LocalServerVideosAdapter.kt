package dev.ragnarok.filegallery.fragment.localserver.videoslocalserver

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.Option
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.model.menu.options.VideoLocalServerOption
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance.Companion.with
import dev.ragnarok.filegallery.util.DownloadWorkUtils.doDownloadVideo
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.disposables.Disposable
import java.util.regex.Pattern

class LocalServerVideosAdapter(private val context: Context, private var data: List<Video>) :
    RecyclerView.Adapter<LocalServerVideosAdapter.Holder>() {
    private val mVideoInteractor: ILocalServerApi = networkInterfaces.localServerApi()
    private var videoOnClickListener: VideoOnClickListener? = null
    private var listDisposable = Disposable.disposed()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_local_server_video, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val video = data[position]
        holder.title.text = video.title
        holder.description.text = video.description
        holder.videoLenght.text = Utils.BytesToSize(video.duration.toLong())
        val photoUrl = video.image
        if (photoUrl.nonNullNoEmpty()) {
            with()
                .load(photoUrl)
                .tag(Constants.PICASSO_TAG)
                .into(holder.image)
        } else {
            with().cancelRequest(holder.image)
        }
        holder.card.setOnClickListener {
            videoOnClickListener?.onVideoClick(holder.bindingAdapterPosition, video)
        }
        holder.card.setOnLongClickListener {
            val menus = ModalBottomSheetDialogFragment.Builder()
            menus.add(
                OptionRequest(
                    VideoLocalServerOption.save_item_video,
                    context.getString(R.string.download),
                    R.drawable.save,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    VideoLocalServerOption.play_item_video,
                    context.getString(R.string.play),
                    R.drawable.play,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    VideoLocalServerOption.update_time_item_video,
                    context.getString(R.string.update_time),
                    R.drawable.ic_recent,
                    false
                )
            )
            menus.add(
                OptionRequest(
                    VideoLocalServerOption.delete_item_video,
                    context.getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    VideoLocalServerOption.edit_item_video,
                    context.getString(R.string.edit),
                    R.drawable.about_writed,
                    true
                )
            )
            menus.header(
                Utils.firstNonEmptyString(video.description, " ") + " - " + video.title,
                R.drawable.video,
                null
            )
            menus.columns(2)
            menus.show(
                (context as FragmentActivity).supportFragmentManager,
                "server_video_options",
                object : ModalBottomSheetDialogFragment.Listener {
                    override fun onModalOptionSelected(option: Option) {
                        when (option.id) {
                            VideoLocalServerOption.save_item_video -> video.link?.let { it1 ->
                                doDownloadVideo(
                                    context, video, it1, "Local"
                                )
                            }

                            VideoLocalServerOption.play_item_video -> {
                                videoOnClickListener?.onVideoClick(
                                    holder.bindingAdapterPosition,
                                    video
                                )
                            }

                            VideoLocalServerOption.update_time_item_video -> {
                                val hash = parseLocalServerURL(video.link)
                                if (hash.isNullOrEmpty()) {
                                    return
                                }
                                listDisposable = mVideoInteractor.update_time(hash)
                                    .fromIOToMain().subscribe(
                                        {
                                            createCustomToast(
                                                context, holder.card
                                            )?.showToast(R.string.success)
                                        }) { t: Throwable? ->
                                        createCustomToast(context, holder.card)
                                            ?.setDuration(Toast.LENGTH_LONG)?.showToastThrowable(t)
                                    }
                            }

                            VideoLocalServerOption.edit_item_video -> {
                                val hash2 = parseLocalServerURL(video.link)
                                if (hash2.isNullOrEmpty()) {
                                    return
                                }
                                listDisposable = mVideoInteractor.get_file_name(hash2)
                                    .fromIOToMain().subscribe(
                                        { t: String? ->
                                            val root = View.inflate(
                                                context, R.layout.entry_file_name, null
                                            )
                                            (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).setText(
                                                t
                                            )
                                            MaterialAlertDialogBuilder(context)
                                                .setTitle(R.string.change_name)
                                                .setCancelable(true)
                                                .setView(root)
                                                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                                                    listDisposable =
                                                        mVideoInteractor.update_file_name(
                                                            hash2,
                                                            (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).text.toString()
                                                                .trim { it <= ' ' })
                                                            .fromIOToMain()
                                                            .subscribe({
                                                                createCustomToast(
                                                                    context, holder.card
                                                                )?.showToast(R.string.success)
                                                            }) { o: Throwable? ->
                                                                createCustomToast(
                                                                    context,
                                                                    holder.card
                                                                )
                                                                    ?.setDuration(Toast.LENGTH_LONG)
                                                                    ?.showToastThrowable(o)
                                                            }
                                                }
                                                .setNegativeButton(R.string.button_cancel, null)
                                                .show()
                                        }) { t: Throwable? ->
                                        createCustomToast(context, holder.card)
                                            ?.setDuration(Toast.LENGTH_LONG)?.showToastThrowable(t)
                                    }
                            }

                            VideoLocalServerOption.delete_item_video -> MaterialAlertDialogBuilder(
                                context
                            )
                                .setMessage(R.string.do_delete)
                                .setTitle(R.string.confirmation)
                                .setCancelable(true)
                                .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                                    val hash1 = parseLocalServerURL(video.link)
                                    if (hash1.isNullOrEmpty()) {
                                        return@setPositiveButton
                                    }
                                    listDisposable = mVideoInteractor.delete_media(hash1)
                                        .fromIOToMain().subscribe(
                                            {
                                                createCustomToast(
                                                    context, holder.card
                                                )?.showToast(R.string.success)
                                            }) { o: Throwable? ->
                                            createCustomToast(context, holder.card)
                                                ?.setDuration(Toast.LENGTH_LONG)
                                                ?.showToastThrowable(o)
                                        }
                                }
                                .setNegativeButton(R.string.button_cancel, null)
                                .show()

                            else -> {}
                        }
                    }

                })
            true
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        listDisposable.dispose()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setVideoOnClickListener(videoOnClickListener: VideoOnClickListener?) {
        this.videoOnClickListener = videoOnClickListener
    }

    fun setData(data: List<Video>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface VideoOnClickListener {
        fun onVideoClick(position: Int, video: Video)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.card_view)
        val image: ImageView = itemView.findViewById(R.id.video_image)
        val videoLenght: TextView = itemView.findViewById(R.id.video_lenght)
        val title: TextView = itemView.findViewById(R.id.title)
        val description: TextView = itemView.findViewById(R.id.description)

    }

    companion object {
        private val PATTERN_FENRIR_SERVER_TRACK_HASH = Pattern.compile("hash=([^&]*)")
        fun parseLocalServerURL(string: String?): String? {
            string ?: return null
            val matcher = PATTERN_FENRIR_SERVER_TRACK_HASH.matcher(string)
            try {
                if (matcher.find()) {
                    return matcher.group(1)
                }
            } catch (ignored: NumberFormatException) {
            }
            return null
        }
    }

}