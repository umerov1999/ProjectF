package dev.ragnarok.fenrir.fragment.feedbackvkofficial

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.link.LinkParser
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class FeedbackVKOfficialAdapter(
    private var data: FeedbackVKOfficialList?,
    private val context: Context
) : RecyclerView.Adapter<FeedbackVKOfficialAdapter.Holder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val mStartOfToday: Long = Utils.startOfTodayMillis()
    private var clickListener: ClickListener? = null
    fun checkPosition(position: Int): Boolean {
        if (data == null) {
            return false
        }
        return position >= 0 && (data?.items?.size ?: -1) > position
    }

    fun getByPosition(position: Int): FeedbackVKOfficial? {
        return data?.items?.get(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_answer_official, parent, false)
        )
    }

    private fun LoadIcon(holder: Holder, Page: FeedbackVKOfficial, isSmall: Boolean) {
        if (!isSmall) {
            holder.avatar.setOnClickListener { }
        }
        val IconRes = GetIconResByType(Page.iconType)
        if (IconRes == null && Page.iconURL == null) {
            if (isSmall) {
                holder.small.visibility = View.VISIBLE
                holder.small.setImageResource(R.drawable.client_round)
                Utils.setColorFilter(
                    holder.small, CurrentTheme.getColorPrimary(
                        context
                    )
                )
            } else {
                holder.small.visibility = View.INVISIBLE
                holder.avatar.setImageResource(R.drawable.client_round)
                Utils.setColorFilter(
                    holder.avatar, CurrentTheme.getColorPrimary(
                        context
                    )
                )
            }
            return
        }
        holder.avatar.clearColorFilter()
        holder.small.clearColorFilter()
        if (IconRes == null) {
            if (isSmall) {
                holder.small.visibility = View.VISIBLE
                displayAvatar(holder.small, transformation, Page.iconURL, Constants.PICASSO_TAG)
            } else {
                holder.small.visibility = View.INVISIBLE
                displayAvatar(holder.avatar, transformation, Page.iconURL, Constants.PICASSO_TAG)
            }
            return
        }
        if (isSmall) {
            holder.small.visibility = View.VISIBLE
            holder.small.setImageResource(IconRes)
        } else {
            holder.small.visibility = View.INVISIBLE
            holder.avatar.setImageResource(IconRes)
        }
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    private fun getDivided(messageDateJavaTime: Long, previousMessageDateJavaTime: Long?): Int {
        val stCurrent = getStatus(messageDateJavaTime)
        return if (previousMessageDateJavaTime == null) {
            stCurrent
        } else {
            val stPrevious = getStatus(previousMessageDateJavaTime)
            if (stCurrent == stPrevious) {
                DIV_DISABLE
            } else {
                stCurrent
            }
        }
    }

    private fun getStatus(time: Long): Int {
        if (time >= mStartOfToday) {
            return DIV_TODAY
        }
        if (time >= mStartOfToday - 86400000) {
            return DIV_YESTERDAY
        }
        return if (time >= mStartOfToday - 864000000) {
            DIV_THIS_WEEK
        } else DIV_OLD
    }

    @Suppress("DEPRECATION")
    private fun fromHtml(source: String): CharSequence {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val items = data?.items ?: return
        val Page = items[position]
        val previous = if (position == 0) null else items[position - 1]
        val lastMessageJavaTime = Page.time * 1000
        when (getDivided(
            lastMessageJavaTime,
            if (previous == null) null else previous.time * 1000
        )) {
            DIV_DISABLE -> holder.mHeaderTitle.visibility = View.GONE
            DIV_OLD -> {
                holder.mHeaderTitle.visibility = View.VISIBLE
                holder.mHeaderTitle.setText(R.string.dialog_day_older)
            }

            DIV_TODAY -> {
                holder.mHeaderTitle.visibility = View.VISIBLE
                holder.mHeaderTitle.setText(R.string.dialog_day_today)
            }

            DIV_YESTERDAY -> {
                holder.mHeaderTitle.visibility = View.VISIBLE
                holder.mHeaderTitle.setText(R.string.dialog_day_yesterday)
            }

            DIV_THIS_WEEK -> {
                holder.mHeaderTitle.visibility = View.VISIBLE
                holder.mHeaderTitle.setText(R.string.dialog_day_ten_days)
            }
        }
        holder.small.visibility = View.INVISIBLE
        Page.header.ifNonNullNoEmpty({ lit ->
            holder.name.visibility = View.VISIBLE
            val replace = SpannableStringBuilder(fromHtml(lit))
            holder.name.setText(
                LinkParser.parseLinks(context, replace),
                TextView.BufferType.SPANNABLE
            )
            if (Page.header_owner_id != null) {
                if (Page.header_owner_avatar_url.nonNullNoEmpty()) {
                    with()
                        .load(Page.header_owner_avatar_url)
                        .tag(Constants.PICASSO_TAG)
                        .placeholder(R.drawable.background_gray)
                        .transform(transformation)
                        .into(holder.avatar)
                    holder.avatar.setOnClickListener {
                        Page.header_owner_id?.let { vit ->
                            clickListener?.openOwnerWall(vit)
                        }
                    }
                    LoadIcon(holder, Page, true)
                } else {
                    with().cancelRequest(holder.avatar)
                    LoadIcon(holder, Page, false)
                }
            } else {
                with().cancelRequest(holder.avatar)
                LoadIcon(holder, Page, false)
            }
        }, {
            holder.name.visibility = View.GONE
            LoadIcon(holder, Page, false)
        })
        Page.text.ifNonNullNoEmpty({
            holder.description.visibility = View.VISIBLE
            val replace = SpannableStringBuilder(fromHtml(it))
            holder.description.setText(
                LinkParser.parseLinks(context, replace),
                TextView.BufferType.SPANNABLE
            )
        }, { holder.description.visibility = View.GONE })
        Page.footer.ifNonNullNoEmpty({
            holder.footer.visibility = View.VISIBLE
            val replace = SpannableStringBuilder(fromHtml(it))
            holder.footer.setText(
                LinkParser.parseLinks(context, replace),
                TextView.BufferType.SPANNABLE
            )
        }, { holder.footer.visibility = View.GONE })
        holder.time.text = AppTextUtils.getDateFromUnixTime(context, Page.time)
        val Img = Page.getImage(256)
        if (Img == null) {
            holder.additional.visibility = View.GONE
            with().cancelRequest(holder.additional)
        } else {
            holder.additional.visibility = View.VISIBLE
            with()
                .load(Img.url)
                .tag(Constants.PICASSO_TAG)
                .placeholder(R.drawable.background_gray)
                .into(holder.additional)
        }
        when {
            Page.attachments.isNullOrEmpty() -> {
                with().cancelRequest(holder.photo_image)
                holder.photo_image.visibility = View.GONE
                holder.attachments.visibility = View.GONE
                holder.attachments.adapter = null
            }

            Page.attachments?.size == 1 -> {
                holder.photo_image.visibility = View.VISIBLE
                holder.attachments.visibility = View.GONE
                holder.attachments.adapter = null
                with()
                    .load(Page.attachments?.get(0)?.getUrlForSize(PhotoSize.X, false))
                    .tag(Constants.PICASSO_TAG)
                    .placeholder(R.drawable.background_gray)
                    .into(holder.photo_image)
                holder.photo_image.setOnClickListener {
                    Page.attachments?.get(0)?.let { r ->
                        getSimpleGalleryPlace(
                            Settings.get().accounts().current, ArrayList(
                                listOf(r)
                            ), 0, true
                        ).tryOpenWith(
                            context
                        )
                    }
                }
            }

            else -> {
                with().cancelRequest(holder.photo_image)
                holder.photo_image.visibility = View.GONE
                holder.attachments.visibility = View.VISIBLE
                holder.attachments.layoutManager =
                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                val adapter = FeedbackPhotosAdapter(Page.attachments.orEmpty())
                adapter.setPhotoSelectionListener(object :
                    FeedbackPhotosAdapter.PhotoSelectionListener {
                    override fun onPhotoClicked(position: Int, photo: Photo) {
                        Page.attachments.nonNullNoEmpty {
                            getSimpleGalleryPlace(
                                Settings.get().accounts().current,
                                ArrayList(it),
                                position,
                                true
                            ).tryOpenWith(
                                context
                            )
                        }
                    }
                })
                holder.attachments.adapter = adapter
            }
        }
        Page.action.ifNonNull({
            if (it.getActionType() == FeedbackVKOfficial.Action_Types.URL) {
                holder.actionButton.setText(R.string.more_info)
            } else {
                holder.actionButton.setText(R.string.open)
            }
            holder.actionButton.visibility = View.VISIBLE
        }, {
            holder.actionButton.visibility = View.GONE
        })
        holder.actionButton.setOnClickListener {
            Page.action.requireNonNull {
                clickListener?.openAction(it)
            }
        }
    }

    private fun GetIconResByType(IconType: String?): Int? {
        if (IconType == null) return null
        if (IconType == "suggested_post_published") {
            return R.drawable.ic_feedback_suggested_post_published
        }
        if (IconType == "transfer_money_cancelled") {
            return R.drawable.ic_feedback_transfer_money_cancelled
        }
        if (IconType == "invite_game") {
            return R.drawable.ic_feedback_invite_app
        }
        if (IconType == "cancel") {
            return R.drawable.ic_feedback_cancel
        }
        if (IconType == "follow") {
            return R.drawable.ic_feedback_follow
        }
        if (IconType == "repost") {
            return R.drawable.ic_feedback_repost
        }
        if (IconType == "story_reply") {
            return R.drawable.ic_feedback_story_reply
        }
        if (IconType == "photo_tag") {
            return R.drawable.ic_feedback_photo_tag
        }
        if (IconType == "invite_group_accepted") {
            return R.drawable.ic_feedback_friend_accepted
        }
        if (IconType == "ads") {
            return R.drawable.ic_feedback_ads
        }
        if (IconType == "like") {
            return R.drawable.ic_feedback_like
        }
        if (IconType == "live") {
            return R.drawable.ic_feedback_live
        }
        if (IconType == "poll") {
            return R.drawable.ic_feedback_poll
        }
        if (IconType == "wall") {
            return R.drawable.ic_feedback_wall
        }
        if (IconType == "friend_found") {
            return R.drawable.ic_feedback_add
        }
        if (IconType == "event") {
            return R.drawable.ic_feedback_event
        }
        if (IconType == "reply") {
            return R.drawable.ic_feedback_reply
        }
        if (IconType == "gift") {
            return R.drawable.ic_feedback_gift
        }
        if (IconType == "friend_suggest") {
            return R.drawable.ic_feedback_follow
        }
        if (IconType == "invite_group") {
            return R.drawable.ic_feedback_invite_group
        }
        if (IconType == "friend_accepted") {
            return R.drawable.ic_feedback_friend_accepted
        }
        if (IconType == "mention") {
            return R.drawable.ic_feedback_mention
        }
        if (IconType == "comment") {
            return R.drawable.ic_feedback_comment
        }
        if (IconType == "message") {
            return R.drawable.ic_feedback_message
        }
        if (IconType == "private_post") {
            return R.drawable.ic_feedback_private_post
        }
        if (IconType == "birthday") {
            return R.drawable.ic_feedback_birthday
        }
        if (IconType == "invite_app") {
            return R.drawable.ic_feedback_invite_app
        }
        if (IconType == "new_post") {
            return R.drawable.ic_feedback_new_post
        }
        if (IconType == "interesting") {
            return R.drawable.ic_feedback_interesting
        }
        if (IconType == "transfer_money") {
            return R.drawable.ic_feedback_transfer_money
        }
        return if (IconType == "transfer_votes") {
            R.drawable.ic_feedback_transfer_votes
        } else null
    }

    override fun getItemCount(): Int {
        return if (data?.items == null) 0 else (data?.items?.size ?: 0)
    }

    fun setData(data: FeedbackVKOfficialList) {
        this.data = data
        notifyDataSetChanged()
    }

    interface ClickListener {
        fun openOwnerWall(owner_id: Long)
        fun openAction(action: FeedbackVKOfficial.Action)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.item_friend_avatar)
        val name: TextView = itemView.findViewById(R.id.item_friend_name)
        val description: TextView
        val footer: TextView
        val time: TextView
        val small: ImageView
        val mHeaderTitle: TextView
        val additional: ShapeableImageView
        val photo_image: ShapeableImageView
        val attachments: RecyclerView
        val actionButton: MaterialButton

        init {
            name.movementMethod = LinkMovementMethod.getInstance()
            description = itemView.findViewById(R.id.item_additional_info)
            description.movementMethod = LinkMovementMethod.getInstance()
            footer = itemView.findViewById(R.id.item_friend_footer)
            footer.movementMethod = LinkMovementMethod.getInstance()
            time = itemView.findViewById(R.id.item_friend_time)
            small = itemView.findViewById(R.id.item_icon)
            mHeaderTitle = itemView.findViewById(R.id.header_title)
            additional = itemView.findViewById(R.id.additional_image)
            attachments = itemView.findViewById(R.id.attachments)
            photo_image = itemView.findViewById(R.id.photo_image)
            actionButton = itemView.findViewById(R.id.action_button)
        }
    }

    companion object {
        private const val DIV_DISABLE = 0
        private const val DIV_TODAY = 1
        private const val DIV_YESTERDAY = 2
        private const val DIV_THIS_WEEK = 3
        private const val DIV_OLD = 4
    }

}