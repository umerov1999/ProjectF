package dev.ragnarok.fenrir.dialog.feedbacklink

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
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.model.feedback.ParcelableFeedbackWrapper
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import java.lang.reflect.Field
import java.util.*

class FeedbackLinkDialog : DialogFragment(), FeedbackLinkAdapter.ActionListener {
    private var mFeedback: Feedback? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wrapper: ParcelableFeedbackWrapper? = requireArguments().getParcelableCompat("feedback")
        mFeedback = wrapper?.get()
    }

    private fun getAllModels(notification: Feedback?): List<Any> {
        notification ?: return Collections.emptyList()
        val models: MutableList<Any> = ArrayList()
        val fields: MutableList<Field> = ArrayList()
        fillClassFields(fields, notification.javaClass)
        for (field in fields) {
            field.isAccessible = true
            try {
                val o = field[notification]
                if (o is List<*>) {
                    for (listItem in o) {
                        if (isSupport(listItem) && !models.contains(listItem)) {
                            if (listItem != null) {
                                models.add(listItem)
                            }
                        }
                    }
                }
                if (o != null && isSupport(o) && !models.contains(o)) {
                    models.add(o)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
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

    private val accountId: Int
        get() = requireArguments().getInt(Extra.ACCOUNT_ID)

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
        fun newInstance(accountId: Int, feedback: Feedback?): FeedbackLinkDialog {
            val bundle = Bundle()
            bundle.putInt(Extra.ACCOUNT_ID, accountId)
            bundle.putParcelable("feedback", ParcelableFeedbackWrapper(feedback))
            val feedbackLinkDialog = FeedbackLinkDialog()
            feedbackLinkDialog.arguments = bundle
            return feedbackLinkDialog
        }

        internal fun fillClassFields(fields: MutableList<Field>, type: Class<*>) {
            fields.addAll(listOf(*type.declaredFields))
            if (type.superclass != null) {
                fillClassFields(fields, type.superclass)
            }
        }

        internal fun isSupport(o: Any?): Boolean {
            return o is User ||
                    o is Post ||
                    o is Photo ||
                    o is Comment ||
                    o is Video ||
                    o is Topic
        }
    }
}