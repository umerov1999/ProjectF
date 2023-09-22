package dev.ragnarok.fenrir.dialog.feedbacklink

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.feedback.CommentFeedback
import dev.ragnarok.fenrir.model.feedback.CopyFeedback
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.model.feedback.FeedbackModelType
import dev.ragnarok.fenrir.model.feedback.LikeCommentFeedback
import dev.ragnarok.fenrir.model.feedback.LikeFeedback
import dev.ragnarok.fenrir.model.feedback.MentionCommentFeedback
import dev.ragnarok.fenrir.model.feedback.MentionFeedback
import dev.ragnarok.fenrir.model.feedback.ParcelableFeedbackWrapper
import dev.ragnarok.fenrir.model.feedback.PostPublishFeedback
import dev.ragnarok.fenrir.model.feedback.ReplyCommentFeedback
import dev.ragnarok.fenrir.model.feedback.UsersFeedback
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.util.Utils.singletonArrayList

class FeedbackLinkDialog : DialogFragment(), FeedbackLinkAdapter.ActionListener {
    private var mFeedback: Feedback? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wrapper: ParcelableFeedbackWrapper? = requireArguments().getParcelableCompat("feedback")
        mFeedback = wrapper?.get()
    }

    @SuppressLint("SwitchIntDef")
    private fun getAllModels(notification: Feedback?): List<AbsModel> {
        notification ?: return emptyList()
        val models: MutableList<AbsModel> = ArrayList()
        when (notification.getModelType()) {
            FeedbackModelType.MODEL_COMMENT_FEEDBACK -> {
                addSupport(models, (notification as CommentFeedback).commentOf)
                addSupport(models, notification.comment)
            }

            FeedbackModelType.MODEL_COPY_FEEDBACK -> {
                addSupport(models, (notification as CopyFeedback).what)
                addListSupport(models, notification.owners)
            }

            FeedbackModelType.MODEL_LIKECOMMENT_FEEDBACK -> {
                addSupport(models, (notification as LikeCommentFeedback).liked)
                addSupport(models, notification.commented)
                addListSupport(models, notification.owners)
            }

            FeedbackModelType.MODEL_LIKE_FEEDBACK -> {
                addSupport(models, (notification as LikeFeedback).liked)
                addListSupport(models, notification.owners)
            }

            FeedbackModelType.MODEL_MENTIONCOMMENT_FEEDBACK -> {
                addSupport(models, (notification as MentionCommentFeedback).where)
                addSupport(models, notification.commentOf)
            }

            FeedbackModelType.MODEL_MENTION_FEEDBACK -> {
                addSupport(models, (notification as MentionFeedback).where)
            }

            FeedbackModelType.MODEL_POSTPUBLISH_FEEDBACK -> {
                addSupport(models, (notification as PostPublishFeedback).post)
            }

            FeedbackModelType.MODEL_REPLYCOMMENT_FEEDBACK -> {
                addSupport(models, (notification as ReplyCommentFeedback).commentsOf)
                addSupport(models, notification.ownComment)
                addSupport(models, notification.feedbackComment)
            }

            FeedbackModelType.MODEL_USERS_FEEDBACK -> {
                addListSupport(models, (notification as UsersFeedback).owners)
            }
        }
        return models
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(requireActivity(), R.layout.fragment_feedback_links, null)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        val adapter = FeedbackLinkAdapter(requireActivity(), getAllModels(mFeedback), this)
        recyclerView.adapter = adapter
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.choose_action)
            .setNegativeButton(R.string.button_cancel, null)
            .setView(view)
        return builder.create()
    }

    private val accountId: Long
        get() = requireArguments().getLong(Extra.ACCOUNT_ID)

    private fun close() {
        dismiss()
    }

    override fun onPostClick(post: Post) {
        close()
        getPostPreviewPlace(accountId, post.vkid, post.ownerId, post).tryOpenWith(requireActivity())
    }

    override fun onCommentClick(comment: Comment) {
        close()
        getCommentsPlace(accountId, comment.commented, comment.getObjectId()).tryOpenWith(
            requireActivity()
        )
    }

    override fun onTopicClick(topic: Topic) {
        close()
        getCommentsPlace(accountId, Commented.from(topic), null).tryOpenWith(requireActivity())
    }

    override fun onPhotoClick(photo: Photo) {
        close()
        getSimpleGalleryPlace(accountId, singletonArrayList(photo), 0, true).tryOpenWith(
            requireActivity()
        )
    }

    override fun onVideoClick(video: Video) {
        close()
        getVideoPreviewPlace(accountId, video).tryOpenWith(requireActivity())
    }

    override fun onUserClick(user: User) {
        close()
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    companion object {
        fun newInstance(accountId: Long, feedback: Feedback?): FeedbackLinkDialog {
            val bundle = Bundle()
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            bundle.putParcelable("feedback", ParcelableFeedbackWrapper(feedback))
            val feedbackLinkDialog = FeedbackLinkDialog()
            feedbackLinkDialog.arguments = bundle
            return feedbackLinkDialog
        }

        internal fun addSupport(models: MutableList<AbsModel>, o: AbsModel?) {
            if (o == null) {
                return
            }
            if (o.getModelType() == AbsModelType.MODEL_USER ||
                o.getModelType() == AbsModelType.MODEL_POST ||
                o.getModelType() == AbsModelType.MODEL_PHOTO ||
                o.getModelType() == AbsModelType.MODEL_COMMENT ||
                o.getModelType() == AbsModelType.MODEL_VIDEO ||
                o.getModelType() == AbsModelType.MODEL_TOPIC
            ) {
                if (!models.contains(o)) {
                    models.add(o)
                }
            }
        }

        internal inline fun <reified T : AbsModel> addListSupport(
            models: MutableList<AbsModel>,
            o: List<T>?
        ) {
            for (i in o.orEmpty()) {
                if (i.getModelType() == AbsModelType.MODEL_USER ||
                    i.getModelType() == AbsModelType.MODEL_POST ||
                    i.getModelType() == AbsModelType.MODEL_PHOTO ||
                    i.getModelType() == AbsModelType.MODEL_COMMENT ||
                    i.getModelType() == AbsModelType.MODEL_VIDEO ||
                    i.getModelType() == AbsModelType.MODEL_TOPIC
                ) {
                    if (!models.contains(i)) {
                        models.add(i)
                    }
                }
            }
        }
    }
}