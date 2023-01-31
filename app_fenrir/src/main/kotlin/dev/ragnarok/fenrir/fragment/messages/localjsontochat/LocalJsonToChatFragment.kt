package dev.ragnarok.fenrir.fragment.messages.localjsontochat

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter.OnMessageActionListener
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

class LocalJsonToChatFragment :
    PlaceSupportMvpFragment<LocalJsonToChatPresenter, ILocalJsonToChatView>(), ILocalJsonToChatView,
    OnMessageActionListener {
    private var mEmpty: TextView? = null
    private var mLoadingProgressBar: RLottieImageView? = null
    private var mLoadingProgressBarDispose = Disposable.disposed()
    private var mLoadingProgressBarLoaded = false
    private var mAdapter: MessagesAdapter? = null
    private var recyclerView: RecyclerView? = null

    private var Title: TextView? = null
    private var SubTitle: TextView? = null
    private var Avatar: ImageView? = null
    private var EmptyAvatar: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_json_chat, container, false)
        root.background = CurrentTheme.getChatBackground(requireActivity())
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        val mAttachment: FloatingActionButton = root.findViewById(R.id.goto_button)
        mAttachment.setOnClickListener { presenter?.toggleAttachment() }
        mAttachment.setOnLongClickListener {
            val my = presenter?.updateMessages(true) ?: return@setOnLongClickListener false
            mAttachment.setImageResource(if (my) R.drawable.account_circle else R.drawable.attachment)
            true
        }

        Title = root.findViewById(R.id.dialog_title)
        SubTitle = root.findViewById(R.id.dialog_subtitle)
        Avatar = root.findViewById(R.id.toolbar_avatar)
        EmptyAvatar = root.findViewById(R.id.empty_avatar_text)

        recyclerView = root.findViewById(android.R.id.list)
        recyclerView?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, true)
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mLoadingProgressBar = root.findViewById(R.id.loading_progress_bar)
        mAdapter = MessagesAdapter(requireActivity(), mutableListOf(), this, true)
        recyclerView?.adapter = mAdapter
        return root
    }

    @DrawableRes
    private fun is_select(@DrawableRes res: Int, id: Int, selected: Int): Int {
        if (id == selected) {
            return R.drawable.check
        }
        return res
    }

    override fun attachments_mode(accountId: Long, last_selected: Int) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                0,
                getString(R.string.json_all_messages),
                is_select(R.drawable.close, 0, last_selected),
                false
            )
        )
        menus.add(
            OptionRequest(
                1,
                getString(R.string.photos),
                is_select(R.drawable.photo_album, 1, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                2,
                getString(R.string.videos),
                is_select(R.drawable.video, 2, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                3,
                getString(R.string.documents),
                is_select(R.drawable.book, 3, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                4,
                getString(R.string.music),
                is_select(R.drawable.song, 4, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                5,
                getString(R.string.links),
                is_select(R.drawable.web, 5, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                6,
                getString(R.string.photo_album),
                is_select(R.drawable.album_photo, 6, last_selected),
                false
            )
        )
        menus.add(
            OptionRequest(
                7,
                getString(R.string.playlist),
                is_select(R.drawable.audio_player, 7, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                8,
                getString(R.string.json_attachments_forward),
                is_select(R.drawable.ic_outline_forward, 8, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                9,
                getString(R.string.posts),
                is_select(R.drawable.about_writed, 9, last_selected),
                true
            )
        )
        menus.add(
            OptionRequest(
                10,
                getString(R.string.json_all_attachments),
                is_select(R.drawable.attachment, 10, last_selected),
                false
            )
        )

        menus.show(childFragmentManager, "json_attachments_select",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    presenter?.uAttachmentType = option.id
                    presenter?.updateMessages(false)
                }
            })
    }

    override fun scroll_pos(pos: Int) {
        recyclerView?.scrollToPosition(pos)
    }

    override fun resolveEmptyText(visible: Boolean) {
        mEmpty?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayData(posts: ArrayList<Message>) {
        mAdapter?.setItems(posts, true)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mLoadingProgressBarDispose.dispose()
        if (mLoadingProgressBarLoaded && !refreshing) {
            mLoadingProgressBarLoaded = false
            val k = ObjectAnimator.ofFloat(mLoadingProgressBar, View.ALPHA, 0.0f).setDuration(1000)
            k.addListener(object : StubAnimatorListener() {
                override fun onAnimationEnd(animation: Animator) {
                    mLoadingProgressBar?.clearAnimationDrawable()
                    mLoadingProgressBar?.visibility = View.GONE
                    mLoadingProgressBar?.alpha = 1f
                }

                override fun onAnimationCancel(animation: Animator) {
                    mLoadingProgressBar?.clearAnimationDrawable()
                    mLoadingProgressBar?.visibility = View.GONE
                    mLoadingProgressBar?.alpha = 1f
                }
            })
            k.start()
        } else if (refreshing) {
            mLoadingProgressBarDispose = Completable.create {
                it.onComplete()
            }.delay(300, TimeUnit.MILLISECONDS).fromIOToMain().subscribe({
                mLoadingProgressBarLoaded = true
                mLoadingProgressBar?.visibility = View.VISIBLE
                mLoadingProgressBar?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    Utils.dp(100F),
                    Utils.dp(100F),
                    intArrayOf(
                        0x000000,
                        CurrentTheme.getColorPrimary(requireActivity()),
                        0x777777,
                        CurrentTheme.getColorSecondary(requireActivity())
                    )
                )
                mLoadingProgressBar?.playAnimation()
            }, RxUtils.ignore())
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LocalJsonToChatPresenter> =
        object : IPresenterFactory<LocalJsonToChatPresenter> {
            override fun create(): LocalJsonToChatPresenter {
                return LocalJsonToChatPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireActivity(),
                    saveInstanceState
                )
            }
        }

    override fun displayToolbarAvatar(peer: Peer) {
        Avatar?.setOnClickListener {
            presenter?.fireOwnerClick(peer.id)
        }
        if (peer.avaUrl.nonNullNoEmpty()) {
            EmptyAvatar?.visibility = View.GONE
            Avatar?.let {
                PicassoInstance.with()
                    .load(peer.avaUrl)
                    .transform(RoundTransformation())
                    .into(it)
            }
        } else {
            Avatar?.let { PicassoInstance.with().cancelRequest(it) }
            if (peer.getTitle().nonNullNoEmpty()) {
                EmptyAvatar?.visibility = View.VISIBLE
                var name: String = peer.getTitle().orEmpty()
                if (name.length > 2) name = name.substring(0, 2)
                name = name.trim { it <= ' ' }
                EmptyAvatar?.text = name
            } else {
                EmptyAvatar?.visibility = View.GONE
            }
            Avatar?.setImageBitmap(
                RoundTransformation().localTransform(
                    Utils.createGradientChatImage(
                        200,
                        200,
                        peer.id
                    )
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoadingProgressBarDispose.dispose()
    }

    override fun setToolbarTitle(title: String?) {
        ActivityUtils.supportToolbarFor(this)?.title = null
        Title?.text = title
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        ActivityUtils.supportToolbarFor(this)?.subtitle = null
        SubTitle?.text = subtitle
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onAvatarClick(message: Message, userId: Long, position: Int) {
        presenter?.fireOwnerClick(userId)
    }

    override fun onLongAvatarClick(message: Message, userId: Long, position: Int) {}
    override fun onRestoreClick(message: Message, position: Int) {}
    override fun onBotKeyboardClick(button: Keyboard.Button) {}

    override fun onMessageLongClick(message: Message, position: Int): Boolean {
        return false
    }

    override fun onMessageClicked(message: Message, position: Int) {}
    override fun onMessageDelete(message: Message) {}

    companion object {

        fun newInstance(accountId: Long): LocalJsonToChatFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = LocalJsonToChatFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
