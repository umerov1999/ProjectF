package dev.ragnarok.fenrir.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.VoiceActionListener
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.SaveMessageBuilder
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.model.WikiPage
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemeOverlay
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.TextingNotifier
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView.BotKeyboardViewDelegate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class QuickAnswerActivity : AppCompatActivity() {
    private val mLiveSubscription = CompositeDisposable()
    private val compositeDisposable = CompositeDisposable()
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(this@QuickAnswerActivity).showToast(R.string.permission_all_granted_text)
    }
    private var etText: TextInputEditText? = null
    private var notifier: TextingNotifier? = null
    private var accountId = 0L
    private lateinit var msg: Message
    private var messageIsRead = false
    private var messagesRepository: IMessagesRepository = messages
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        @StyleRes val theme: Int = when (Settings.get().main().themeOverlay) {
            ThemeOverlay.AMOLED -> R.style.QuickReply_Amoled
            ThemeOverlay.MD1 -> R.style.QuickReply_MD1
            ThemeOverlay.OFF -> R.style.QuickReply
            else -> R.style.QuickReply
        }
        setTheme(theme)
        super.onCreate(savedInstanceState)
        val focusToField = intent.getBooleanExtra(EXTRA_FOCUS_TO_FIELD, true)
        if (!focusToField) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }
        msg = (intent.extras?.getParcelableCompat(Extra.MESSAGE) ?: return)
        accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
        notifier = TextingNotifier(accountId)
        setContentView(R.layout.activity_quick_answer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar?.setNavigationIcon(R.drawable.arrow_left)
        setSupportActionBar(toolbar)
        val tvMessage = findViewById<TextView>(R.id.item_message_text)
        val tvTime = findViewById<TextView>(R.id.item_message_time)
        etText = findViewById(R.id.activity_quick_answer_edit_text)
        val ivAvatar = findViewById<ImageView>(R.id.avatar)
        val btnToDialog = findViewById<ImageButton>(R.id.activity_quick_answer_to_dialog)
        val btnSend = findViewById<ImageButton>(R.id.activity_quick_answer_send)
        val messageTime = AppTextUtils.getDateFromUnixTime(this, msg.date)
        val title = intent.getStringExtra(Extra.TITLE)
        supportActionBar?.title = title
        tvMessage.setText(intent.getStringExtra(PARAM_BODY), TextView.BufferType.SPANNABLE)
        tvTime.text = messageTime
        val transformation = CurrentTheme.createTransformationForAvatar()
        val imgUrl = intent.getStringExtra(Extra.IMAGE)
        if (ivAvatar != null) {
            ViewUtils.displayAvatar(ivAvatar, transformation, imgUrl, null)
        }
        val forwardMessagesRoot = findViewById<ViewGroup>(R.id.forward_messages)
        val attachmentsRoot = findViewById<View>(R.id.item_message_attachment_container)
        val attachmentsHolder = AttachmentsHolder()
        attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
            .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
            .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
            .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
            .setVgBigLinks(attachmentsRoot.findViewById(R.id.biglinks_attachments))
            .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
            .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
            .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments))
        val botKeyboardView = findViewById<BotKeyboardView>(R.id.input_keyboard_container)
        if (botKeyboardView != null) {
            val msgKeyboard = msg.keyboard
            if (msgKeyboard != null && msgKeyboard.inline && msgKeyboard.buttons?.size.orZero() > 0) {
                botKeyboardView.visibility = View.VISIBLE
                botKeyboardView.setButtons(msgKeyboard.buttons, false)
            } else {
                botKeyboardView.visibility = View.GONE
            }
            botKeyboardView.setDelegate(object : BotKeyboardViewDelegate {
                override fun didPressedButton(button: Keyboard.Button, needClose: Boolean) {
                    if (button.type == "open_link") {
                        LinkHelper.openLinkInBrowser(this@QuickAnswerActivity, button.link)
                        return
                    }
                    val builder = SaveMessageBuilder(accountId, msg.peerId)
                        .setPayload(button.payload).setBody(button.label)
                    compositeDisposable.add(
                        messagesRepository.put(builder)
                            .fromIOToMain()
                            .subscribe({ onMessageSaved() }) { throwable ->
                                onSavingError(
                                    throwable
                                )
                            })
                }
            })
        }
        val hasAttachments =
            msg.fwd.nonNullNoEmpty() || msg.attachments?.hasAttachments == true
        attachmentsRoot.visibility = if (hasAttachments) View.VISIBLE else View.GONE
        if (hasAttachments) {
            val attachmentsViewBinder =
                AttachmentsViewBinder(this, object : OnAttachmentsActionCallback {
                    override fun onPollOpen(poll: Poll) {}
                    override fun onVideoPlay(video: Video) {}
                    override fun onAudioPlay(
                        position: Int,
                        audios: ArrayList<Audio>,
                        holderPosition: Int?
                    ) {
                        startForPlayList(this@QuickAnswerActivity, audios, position, false)
                    }

                    override fun onForwardMessagesOpen(messages: ArrayList<Message>) {}
                    override fun onOpenOwner(ownerId: Long) {}
                    override fun onGoToMessagesLookup(message: Message) {}
                    override fun onDocPreviewOpen(document: Document) {}
                    override fun onPostOpen(post: Post) {}
                    override fun onLinkOpen(link: Link) {}
                    override fun onUrlOpen(url: String) {}
                    override fun onFaveArticle(article: Article) {}
                    override fun onShareArticle(article: Article) {}
                    override fun onWikiPageOpen(page: WikiPage) {}
                    override fun onPhotosOpen(
                        photos: ArrayList<Photo>,
                        index: Int,
                        refresh: Boolean
                    ) {
                    }

                    override fun onUrlPhotoOpen(
                        url: String,
                        prefix: String,
                        photo_prefix: String
                    ) {
                    }

                    override fun onStoryOpen(story: Story) {}
                    override fun onWallReplyOpen(reply: WallReply) {}
                    override fun onAudioPlaylistOpen(playlist: AudioPlaylist) {}
                    override fun onPhotoAlbumOpen(album: PhotoAlbum) {}
                    override fun onMarketAlbumOpen(market_album: MarketAlbum) {}
                    override fun onMarketOpen(market: Market) {}
                    override fun onArtistOpen(artist: AudioArtist) {}
                    override fun onRequestWritePermissions() {
                        requestWritePermission.launch()
                    }
                })
            attachmentsViewBinder.setVoiceActionListener(object : VoiceActionListener {
                override fun onVoiceHolderBinded(voiceMessageId: Int, voiceHolderId: Int) {}
                override fun onVoicePlayButtonClick(
                    voiceHolderId: Int,
                    voiceMessageId: Int,
                    messageId: Int,
                    peerId: Long,
                    voiceMessage: VoiceMessage
                ) {
                    val audio =
                        Audio().setId(voiceMessage.getId()).setOwnerId(voiceMessage.getOwnerId())
                            .setTitle(
                                voiceMessage.getId().toString() + "_" + voiceMessage.getOwnerId()
                            )
                            .setArtist("Voice")
                            .setIsLocal().setDuration(voiceMessage.getDuration()).setUrl(
                                Utils.firstNonEmptyString(
                                    voiceMessage.getLinkMp3(),
                                    voiceMessage.getLinkOgg()
                                )
                            )
                    startForPlayList(this@QuickAnswerActivity, ArrayList(listOf(audio)), 0, false)
                }

                override fun onVoiceTogglePlaybackSpeed() {}
                override fun onTranscript(voiceMessageId: String, messageId: Int) {}
            })
            attachmentsViewBinder.displayAttachments(
                msg.attachments,
                attachmentsHolder,
                true,
                msg.getObjectId(),
                msg.peerId, null
            )
            attachmentsViewBinder.displayForwards(msg.fwd, forwardMessagesRoot, true)
        }
        etText?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                if (!messageIsRead) {
                    setMessageAsRead()
                    messageIsRead = true
                }
                cancelFinishWithDelay()
                notifier?.notifyAboutTyping(msg.peerId)
            }
        })
        btnSend.setOnClickListener { send() }
        btnToDialog.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.action = MainActivity.ACTION_OPEN_PLACE
            val chatPlace = PlaceFactory.getChatPlace(
                accountId, accountId, Peer(
                    msg.peerId
                ).setAvaUrl(imgUrl).setTitle(title)
            )
            intent.putExtra(Extra.PLACE, chatPlace)
            startActivity(intent)
            finish()
        }
        val liveDelay = intent.getBooleanExtra(EXTRA_LIVE_DELAY, false)
        if (liveDelay) {
            finishWithDelay()
        }
    }

    private fun finishWithDelay() {
        mLiveSubscription.add(
            Observable.just(Any())
                .delay(1, TimeUnit.MINUTES)
                .subscribe { finish() })
    }

    internal fun cancelFinishWithDelay() {
        mLiveSubscription.dispose()
    }

    override fun onDestroy() {
        mLiveSubscription.dispose()
        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /**
     * Отправка сообщения
     */
    private fun send() {
        val trimmed_text = etText?.text.toString().trim { it <= ' ' }
        if (trimmed_text.isEmpty()) {
            createCustomToast(this).setDuration(Toast.LENGTH_LONG)
                .showToastError(R.string.text_hint)
            return
        }
        val requireEncryption = Settings.get()
            .security()
            .isMessageEncryptionEnabled(accountId, msg.peerId)
        @KeyLocationPolicy var policy = KeyLocationPolicy.PERSIST
        if (requireEncryption) {
            policy = Settings.get()
                .security()
                .getEncryptionLocationPolicy(accountId, msg.peerId)
        }
        val builder = SaveMessageBuilder(accountId, msg.peerId)
            .setBody(trimmed_text)
            .setForwardMessages(ArrayList(setOf(msg)))
            .setRequireEncryption(requireEncryption)
            .setKeyLocationPolicy(policy)
        compositeDisposable.add(
            messagesRepository.put(builder)
                .fromIOToMain()
                .subscribe({ onMessageSaved() }) { throwable ->
                    onSavingError(
                        throwable
                    )
                })
    }

    internal fun onSavingError(throwable: Throwable) {
        createCustomToast(this).showToastThrowable(throwable)
    }

    internal fun onMessageSaved() {
        NotificationHelper.tryCancelNotificationForPeer(this, accountId, msg.peerId)
        messagesRepository.runSendingQueue()
        finish()
    }

    internal fun setMessageAsRead() {
        compositeDisposable.add(
            messagesRepository.markAsRead(accountId, msg.peerId, msg.getObjectId())
                .fromIOToMain()
                .subscribe(RxUtils.dummy(), RxUtils.ignore())
        )
    }

    companion object {
        const val PARAM_BODY = "body"
        const val EXTRA_FOCUS_TO_FIELD = "focus_to_field"
        const val EXTRA_LIVE_DELAY = "live_delay"


        fun forStart(
            context: Context?,
            accountId: Long,
            msg: Message?,
            body: String?,
            imgUrl: String?,
            title: String?
        ): Intent {
            val intent = Intent(context, QuickAnswerActivity::class.java)
            intent.putExtra(PARAM_BODY, body)
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.MESSAGE, msg)
            intent.putExtra(Extra.TITLE, title)
            intent.putExtra(Extra.IMAGE, imgUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}