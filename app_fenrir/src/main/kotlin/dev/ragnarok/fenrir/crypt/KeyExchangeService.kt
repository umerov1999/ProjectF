package dev.ragnarok.fenrir.crypt

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Base64
import android.util.LongSparseArray
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.KeyExchangeCommitActivity.Companion.createIntent
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.crypt.CryptHelper.analizeMessageBody
import dev.ragnarok.fenrir.crypt.CryptHelper.createRsaPublicKeyFromString
import dev.ragnarok.fenrir.crypt.CryptHelper.decryptRsa
import dev.ragnarok.fenrir.crypt.CryptHelper.encryptRsa
import dev.ragnarok.fenrir.crypt.CryptHelper.generateRandomAesKey
import dev.ragnarok.fenrir.crypt.CryptHelper.generateRsaKeyPair
import dev.ragnarok.fenrir.crypt.ver.Version.currentVersion
import dev.ragnarok.fenrir.crypt.ver.Version.ofCurrent
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Logger.wtf
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.concurrent.TimeUnit
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import kotlin.random.Random

class KeyExchangeService : Service() {
    private val mUtilsInteractor: IUtilsInteractor = InteractorFactory.createUtilsInteractor()
    private val mCompositeSubscription = CompositeDisposable()
    private var mCurrentActiveSessions: LongSparseArray<KeyExchangeSession> = LongSparseArray(1)
    private var mCurrentActiveNotifications: LongSparseArray<NotificationCompat.Builder> =
        LongSparseArray(1)
    private var mFinishedSessionsIds: MutableSet<Long> = HashSet(1)
    private var mNotificationManager: NotificationManagerCompat? = null
    private val mStopServiceHandler = Handler(Looper.getMainLooper()) { msg: Message ->
        if (msg.what == WHAT_STOP_SERVICE) {
            finishAllByTimeout()
            stopSelf()
        }
        false
    }
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            wakeLock?.setReferenceCounted(false)
        }
        mNotificationManager = NotificationManagerCompat.from(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS_MESSAGE -> {
                val accountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val peerId = intent.extras!!.getLong(Extra.PEER_ID)
                val messageId = intent.extras!!.getInt(Extra.MESSAGE_ID)
                val message: ExchangeMessage = intent.getParcelableExtraCompat(Extra.MESSAGE)!!
                processNewKeyExchangeMessage(accountId, peerId, messageId, message)
            }

            ACTION_INICIATE_KEY_EXCHANGE -> {
                val accountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val peerId = intent.extras!!.getLong(Extra.PEER_ID)
                @KeyLocationPolicy val keyLocationPolicy = intent.extras!!.getInt(
                    EXTRA_KEY_LOCATION_POLICY
                )
                initiateKeyExchange(accountId, peerId, keyLocationPolicy)
            }

            ACTION_APPLY_EXHANGE -> {
                val accountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val peerId = intent.extras!!.getLong(Extra.PEER_ID)
                val messageId = intent.extras!!.getInt(Extra.MESSAGE_ID)
                val message: ExchangeMessage = intent.getParcelableExtraCompat(Extra.MESSAGE)!!
                if (AppPerms.hasNotificationPermissionSimple(this)) {
                    mNotificationManager?.cancel(
                        message.sessionId.toString(),
                        NOTIFICATION_KEY_EXCHANGE_REQUEST
                    )
                }
                processKeyExchangeMessage(accountId, peerId, messageId, message, false)
            }

            ACTION_DECLINE -> {
                val accountId = intent.extras!!.getLong(Extra.ACCOUNT_ID)
                val peerId = intent.extras!!.getLong(Extra.PEER_ID)
                val message: ExchangeMessage = intent.getParcelableExtraCompat(Extra.MESSAGE)!!
                declineInputSession(accountId, peerId, message)
            }
        }
        toggleServiceLiveHandler()
        return START_NOT_STICKY
    }

    private fun declineInputSession(accountId: Long, peerId: Long, message: ExchangeMessage) {
        notifyOpponentAboutSessionFail(
            accountId,
            peerId,
            message.sessionId,
            ErrorCodes.CANCELED_BY_USER
        )
    }

    private fun processNewKeyExchangeMessage(
        accountId: Long,
        peerId: Long,
        messageId: Int,
        message: ExchangeMessage
    ) {
        when (message.senderSessionState) {
            SessionState.NO_INITIATOR_STATE_1, SessionState.NO_INITIATOR_FINISHED, SessionState.INITIATOR_STATE_1, SessionState.INITIATOR_STATE_2, SessionState.INITIATOR_FINISHED -> processKeyExchangeMessage(
                accountId,
                peerId,
                messageId,
                message,
                true
            )

            SessionState.INITIATOR_EMPTY, SessionState.NO_INITIATOR_EMPTY -> throw IllegalStateException(
                "Invalid session state"
            )

            SessionState.FAILED -> onReceiveSessionFailStatus(message)
            SessionState.CLOSED -> {}
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun findSessionFor(accountId: Long, peerId: Long): KeyExchangeSession? {
        for (i in 0 until mCurrentActiveSessions.size()) {
            val key = mCurrentActiveSessions.keyAt(i)
            val session = mCurrentActiveSessions[key]
            if (session.accountId == accountId && session.peerId == peerId) {
                return session
            }
        }
        return null
    }

    private fun registerSession(session: KeyExchangeSession) {
        mCurrentActiveSessions.put(session.id, session)
    }

    @SuppressLint("CheckResult")
    private fun initiateKeyExchange(
        accountId: Long,
        peerId: Long,
        @KeyLocationPolicy keyLocationPolicy: Int
    ) {
        val existsSession = findSessionFor(accountId, peerId)
        if (existsSession != null) {
            CustomToast.createCustomToast(this).setDuration(Toast.LENGTH_LONG)
                .showToastInfo(R.string.session_already_created)
            return
        }
        mUtilsInteractor.getServerTime(accountId)
            .fromIOToMain()
            .delay(1500, TimeUnit.MILLISECONDS)
            .subscribe({
                val session = KeyExchangeSession.createOutSession(
                    it, accountId, peerId, keyLocationPolicy
                )
                session.oppenentSessionState = SessionState.NO_INITIATOR_EMPTY
                registerSession(session)
                notifyAboutKeyExchangeAsync(accountId, peerId, session.id)
                fireSessionStateChanged(session)
                try {
                    val pair = generateRsaKeyPair(ofCurrent().rsaKeySize)
                    session.myPrivateKey = pair.private
                    val encodedPublicKey = pair.public.encoded
                    val pulicBase64 = Base64.encodeToString(encodedPublicKey, Base64.DEFAULT)
                    val message = ExchangeMessage.Builder(
                        currentVersion,
                        session.id,
                        SessionState.INITIATOR_STATE_1
                    )
                        .setPublicKey(pulicBase64)
                        .setKeyLocationPolicy(keyLocationPolicy)
                        .create()
                    sendMessage(accountId, peerId, message)
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                }
            }) { }
    }

    private fun onReceiveSessionFailStatus(message: ExchangeMessage) {
        d(TAG, "onReceiveSessionFailStatus, message: $message")
        if (mFinishedSessionsIds.contains(message.sessionId)) {
            wtf(TAG, "onReceiveSessionFailStatus, session already finished")
            return
        }
        val session = mCurrentActiveSessions[message.sessionId]
        finishSessionByOpponentFail(session, message)
    }

    private fun notifyOpponentAboutSessionFail(
        accountId: Long,
        peerId: Long,
        sessionId: Long,
        errorCode: Int
    ) {
        d(TAG, "notifyOpponentAboutSessionFail, sessionId: $sessionId")
        val message = ExchangeMessage.Builder(currentVersion, sessionId, SessionState.FAILED)
            .setErrorCode(errorCode)
            .create()
        sendMessage(accountId, peerId, message)
    }

    private fun displayUserConfirmNotification(
        accountId: Long,
        peerId: Long,
        messageId: Int,
        message: ExchangeMessage
    ) {
        mCompositeSubscription.add(OwnerInfo.getRx(this, accountId, Peer.toUserId(peerId))
            .fromIOToMain()
            .subscribe({ userInfo ->
                displayUserConfirmNotificationImpl(
                    accountId,
                    peerId,
                    messageId,
                    message,
                    userInfo
                )
            }) { })
    }

    override fun onDestroy() {
        wakeLock?.release()
        mCompositeSubscription.dispose()
        super.onDestroy()
    }

    private fun findBuilder(sessionId: Long): NotificationCompat.Builder? {
        return mCurrentActiveNotifications.get(sessionId, null)
    }

    private fun notifyAboutKeyExchangeAsync(accountId: Long, peerId: Long, sessionId: Long) {
        mCompositeSubscription.add(OwnerInfo.getRx(this, accountId, Peer.toUserId(peerId))
            .fromIOToMain()
            .subscribe({ userInfo ->
                notifyAboutKeyExchange(
                    sessionId,
                    userInfo
                )
            }) { })
    }

    private fun notifyAboutKeyExchange(sessionId: Long, info: OwnerInfo) {
        val session = mCurrentActiveSessions.get(sessionId, null)
            ?: //сессия уже неактивна
            return
        val targetContentText = getString(R.string.key_exchange_content_text, info.user.fullName)
        val builder =
            NotificationCompat.Builder(this, AppNotificationChannels.KEY_EXCHANGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_crypt_key_vector)
                .setLargeIcon(info.avatar)
                .setContentTitle(getString(R.string.key_exchange))
                .setContentText(targetContentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(NotificationCompat.BigTextStyle().bigText(targetContentText))
                .setAutoCancel(true)
        mCurrentActiveNotifications.put(sessionId, builder)
        refreshSessionNotification(session)
    }

    @SuppressLint("MissingPermission")
    private fun displayUserConfirmNotificationImpl(
        accountId: Long,
        peerId: Long,
        messageId: Int,
        message: ExchangeMessage,
        ownerInfo: OwnerInfo
    ) {
        if (hasOreo()) {
            mNotificationManager?.createNotificationChannel(
                AppNotificationChannels.getKeyExchangeChannel(
                    this
                )
            )
        }
        val targetContentText =
            getString(R.string.key_exchange_request_content_text, ownerInfo.user.fullName)
        val builder =
            NotificationCompat.Builder(this, AppNotificationChannels.KEY_EXCHANGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_crypt_key_vector)
                .setLargeIcon(ownerInfo.avatar)
                .setContentTitle(getString(R.string.key_exchange_request))
                .setContentText(targetContentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(targetContentText))
                .setAutoCancel(true)
        val intent = createIntent(this, accountId, peerId, ownerInfo.user, messageId, message)
        val contentIntent = PendingIntent.getActivity(
            this,
            messageId,
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val apply = createIntentForApply(this, message, accountId, peerId, messageId)
        val quickPendingIntent = PendingIntent.getService(
            this,
            messageId,
            apply,
            makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val applyAction = NotificationCompat.Action(
            R.drawable.check,
            getString(R.string.apply),
            quickPendingIntent
        )
        builder.addAction(applyAction)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        val notification = builder.build()
        if (AppPerms.hasNotificationPermissionSimple(this)) {
            mNotificationManager?.notify(
                message.sessionId.toString(),
                NOTIFICATION_KEY_EXCHANGE_REQUEST,
                notification
            )
        }
    }

    private fun processKeyExchangeMessage(
        accountId: Long, peerId: Long, messageId: Int,
        message: ExchangeMessage, needConfirmIfSessionNotStarted: Boolean
    ) {
        var session = mCurrentActiveSessions[message.sessionId]
        if (session != null && session.isMessageProcessed(messageId)) {
            d(TAG, "This message was processed, id: $messageId")
            return
        }
        if (mFinishedSessionsIds.contains(message.sessionId)) {
            d(TAG, "This session was CLOSED, mFinishedSessionsIds contains session_id")
            return
        }
        if (session != null && session.localSessionState == SessionState.CLOSED) {
            d(TAG, "This session was CLOSED, mCurrentActiveSessions array contains session")
            return
        }
        @SessionState val opponentSessionState = message.senderSessionState
        if (session == null) {
            if (opponentSessionState != SessionState.INITIATOR_STATE_1) {
                notifyOpponentAboutSessionFail(
                    accountId,
                    peerId,
                    message.sessionId,
                    ErrorCodes.SESSION_EXPIRED
                )
                return
            }
            if (needConfirmIfSessionNotStarted) {
                displayUserConfirmNotification(accountId, peerId, messageId, message)
                return
            }
            session = KeyExchangeSession.createInputSession(
                message.sessionId, accountId,
                peerId, message.getKeyLocationPolicy()
            )
            mCurrentActiveSessions.put(message.sessionId, session)
            notifyAboutKeyExchangeAsync(accountId, peerId, session.id)
        }
        session.appendMessageId(messageId)
        session.oppenentSessionState = opponentSessionState
        fireSessionStateChanged(session)
        d(TAG, "processKeyExchangeMessage, opponentSessionState: $opponentSessionState")
        try {
            when (opponentSessionState) {
                SessionState.INITIATOR_STATE_1 -> {
                    assertSessionState(session, SessionState.NO_INITIATOR_EMPTY)
                    processNoIniciatorEmptyState(accountId, peerId, session, message)
                }

                SessionState.NO_INITIATOR_STATE_1 -> {
                    assertSessionState(session, SessionState.INITIATOR_STATE_1)
                    processIniciatorState1(accountId, peerId, session, message)
                }

                SessionState.INITIATOR_STATE_2 -> {
                    assertSessionState(session, SessionState.NO_INITIATOR_STATE_1)
                    processNoIniciatorState1(accountId, peerId, session, message)
                }

                SessionState.NO_INITIATOR_FINISHED -> {
                    assertSessionState(session, SessionState.INITIATOR_STATE_2)
                    processIniciatorState2(accountId, peerId, session, message)
                }

                SessionState.INITIATOR_FINISHED -> {
                    assertSessionState(session, SessionState.NO_INITIATOR_FINISHED)
                    processNoIniciatorFinished(accountId, peerId, session)
                }

                SessionState.CLOSED, SessionState.FAILED, SessionState.INITIATOR_EMPTY, SessionState.NO_INITIATOR_EMPTY -> {}
            }
        } catch (e: InvalidSessionStateException) {
            e.printStackTrace()
            notifyOpponentAboutSessionFail(
                accountId,
                peerId,
                session.id,
                ErrorCodes.INVALID_SESSION_STATE
            )
            finishSession(session, true)
        }
    }

    private fun processNoIniciatorFinished(
        accountId: Long,
        peerId: Long,
        session: KeyExchangeSession
    ) {
        storeKeyToDatabase(accountId, peerId, session)
        finishSession(session, false)
    }

    private fun finishSessionByOpponentFail(session: KeyExchangeSession, message: ExchangeMessage) {
        session.localSessionState = SessionState.CLOSED
        mCurrentActiveSessions.remove(session.id)
        mFinishedSessionsIds.add(session.id)
        mCurrentActiveNotifications.remove(session.id)
        if (AppPerms.hasNotificationPermissionSimple(this)) {
            mNotificationManager?.cancel(session.id.toString(), NOTIFICATION_KEY_EXCHANGE)
        }
        showError(localizeError(message.errorCode))
        d(
            TAG,
            "Session was released by opponent, id: " + session.id + ", error_code: " + message.errorCode
        )
        toggleServiceLiveHandler()
    }

    private fun finishSession(session: KeyExchangeSession, withError: Boolean) {
        session.localSessionState = SessionState.CLOSED
        mCurrentActiveSessions.remove(session.id)
        mFinishedSessionsIds.add(session.id)
        mCurrentActiveNotifications.remove(session.id)
        if (AppPerms.hasNotificationPermissionSimple(this)) {
            mNotificationManager?.cancel(session.id.toString(), NOTIFICATION_KEY_EXCHANGE)
        }
        if (withError) {
            showError(getString(R.string.key_exchange_failed))
        } else {
            CustomToast.createCustomToast(this).setDuration(Toast.LENGTH_LONG)
                .showToastSuccessBottom(R.string.you_have_successfully_exchanged_keys)
        }
        d(TAG, "Session was released, id: " + session.id + ", withError: " + withError)
        toggleServiceLiveHandler()
    }

    private fun localizeError(code: Int): String {
        return when (code) {
            ErrorCodes.INVALID_SESSION_STATE -> getString(R.string.error_key_exchange_invalid_session_state)
            ErrorCodes.CANCELED_BY_USER -> getString(R.string.error_key_exchange_cancelled_by_user)
            ErrorCodes.SESSION_EXPIRED -> getString(R.string.error_key_exchange_session_expired)
            else -> getString(R.string.key_exchange_failed)
        }
    }

    private fun showError(text: String) {
        CustomToast.createCustomToast(this).setDuration(Toast.LENGTH_LONG).showToastError(text)
    }

    private fun finishAllByTimeout() {
        for (i in 0 until mCurrentActiveSessions.size()) {
            val id = mCurrentActiveSessions.keyAt(i)
            val session = mCurrentActiveSessions[id]
            if (session != null) {
                finishSession(session, true)
            }
        }
    }

    private fun toggleServiceLiveHandler() {
        mStopServiceHandler.removeMessages(WHAT_STOP_SERVICE)
        mStopServiceHandler.sendEmptyMessageDelayed(WHAT_STOP_SERVICE, (60 * 1000).toLong())
    }

    @SuppressLint("CheckResult")
    private fun storeKeyToDatabase(accountId: Long, peerId: Long, session: KeyExchangeSession) {
        val pair = AesKeyPair()
            .setVersion(currentVersion)
            .setAccountId(accountId)
            .setPeerId(peerId)
            .setDate(now())
            .setHisAesKey(session.hisAesKey)
            .setMyAesKey(session.myAesKey)
            .setSessionId(session.id)
            .setStartMessageId(session.startMessageId)
            .setEndMessageId(session.endMessageId)
        Stores.instance
            .keys(session.keyLocationPolicy)
            .saveKeyPair(pair)
            .fromIOToMain()
            .subscribe({}) { throwable -> showError(throwable.toString()) }
    }

    private fun processIniciatorState2(
        accountId: Long,
        peerId: Long,
        session: KeyExchangeSession,
        message: ExchangeMessage
    ) {
        val m = ExchangeMessage.Builder(
            currentVersion,
            message.sessionId,
            SessionState.INITIATOR_FINISHED
        )
            .create()
        sendMessage(accountId, peerId, m)
        storeKeyToDatabase(accountId, peerId, session)
        finishSession(session, false)
    }

    private fun processIniciatorState1(
        accountId: Long,
        peerId: Long,
        session: KeyExchangeSession,
        message: ExchangeMessage
    ) {
        val hisAesKey = message.aesKey
        val myPrivateKey = session.myPrivateKey
        try {
            val hisAesEncoded = Base64.decode(hisAesKey, Base64.DEFAULT)
            val hisOriginalAes = decryptRsa(hisAesEncoded, myPrivateKey)
            session.hisAesKey = hisOriginalAes
            val myOriginalAesKey = generateRandomAesKey(ofCurrent().aesKeySize)
            session.myAesKey = myOriginalAesKey
            val hisPublicKey = createRsaPublicKeyFromString(message.publicKey.orEmpty())
            val myEncodedAesKey = encryptRsa(myOriginalAesKey, hisPublicKey)
            val myEncodedAesKeyBase64 = Base64.encodeToString(myEncodedAesKey, Base64.DEFAULT)
            d(
                TAG,
                "processIniciatorState1, myOriginalAesKey: $myOriginalAesKey, hisOriginalAes: $hisOriginalAes"
            )
            val m =
                ExchangeMessage.Builder(currentVersion, session.id, SessionState.INITIATOR_STATE_2)
                    .setAesKey(myEncodedAesKeyBase64)
                    .create()
            sendMessage(accountId, peerId, m)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }
    }

    //NO_INITIATOR_STATE_1
    private fun processNoIniciatorState1(
        accountId: Long,
        peerId: Long,
        session: KeyExchangeSession,
        message: ExchangeMessage
    ) {
        val hisAesKey = message.aesKey
        val myPrivateKey = session.myPrivateKey
        try {
            val hisAesEncoded = Base64.decode(hisAesKey, Base64.DEFAULT)
            val hisOriginalAes = decryptRsa(hisAesEncoded, myPrivateKey)
            d(TAG, "processNoIniciatorState1, hisOriginalAes: $hisOriginalAes")
            session.hisAesKey = hisOriginalAes
            val m = ExchangeMessage.Builder(
                currentVersion,
                session.id,
                SessionState.NO_INITIATOR_FINISHED
            )
                .create()
            sendMessage(accountId, peerId, m)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    //NO_INITIATOR_EMPTY
    private fun processNoIniciatorEmptyState(
        accountId: Long,
        peerId: Long,
        session: KeyExchangeSession,
        message: ExchangeMessage
    ) {
        try {
            val originalAesKey = generateRandomAesKey(ofCurrent().aesKeySize)
            session.myAesKey = originalAesKey
            d(TAG, "processNoIniciatorEmptyState, originalAesKey: $originalAesKey")
            val publicKey = createRsaPublicKeyFromString(message.publicKey.orEmpty())
            val encodedAesKey = encryptRsa(originalAesKey, publicKey)
            val encodedAesKeyBase64 = Base64.encodeToString(encodedAesKey, Base64.DEFAULT)
            val myPair = generateRsaKeyPair(ofCurrent().rsaKeySize)
            session.myPrivateKey = myPair.private
            val myEncodedPublicKey = myPair.public.encoded
            val myPulicBase64 = Base64.encodeToString(myEncodedPublicKey, Base64.DEFAULT)
            val m = ExchangeMessage.Builder(
                currentVersion,
                message.sessionId,
                SessionState.NO_INITIATOR_STATE_1
            )
                .setAesKey(encodedAesKeyBase64)
                .setPublicKey(myPulicBase64)
                .create()
            sendMessage(accountId, peerId, m)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    private fun sendMessage(accountId: Long, peerId: Long, message: ExchangeMessage) {
        d(TAG, "sendMessage, message: $message")
        sendMessageImpl(accountId, peerId, message)
            .fromIOToMain()
            .subscribe({ integer ->
                onMessageSent(
                    message,
                    integer
                )
            }) { throwable ->
                showError(throwable.toString())
                val session = findSessionFor(accountId, peerId)
                if (session != null) {
                    finishSession(session, true)
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun refreshSessionNotification(session: KeyExchangeSession) {
        if (hasOreo()) {
            mNotificationManager?.createNotificationChannel(
                AppNotificationChannels.getKeyExchangeChannel(
                    this
                )
            )
        }
        val builder = findBuilder(session.id)
        val localState = session.localSessionState
        val opponentState = session.oppenentSessionState
        val state = localState.coerceAtLeast(opponentState)
        builder?.let {
            it.setProgress(SessionState.CLOSED, state, false)
            if (AppPerms.hasNotificationPermissionSimple(this)) {
                mNotificationManager?.notify(
                    session.id.toString(),
                    NOTIFICATION_KEY_EXCHANGE,
                    it.build()
                )
            }
        }
    }

    private fun fireSessionStateChanged(session: KeyExchangeSession) {
        d(
            TAG,
            "fireSessionStateChanged, id: " + session.id + ", state: " + session.localSessionState
        )
        refreshSessionNotification(session)
    }

    private fun onMessageSent(message: ExchangeMessage, id: Int) {
        d(TAG, "onMessageSent, result_id: $id, message: $message")
        val session = mCurrentActiveSessions[message.sessionId]
        if (session != null) {
            session.localSessionState = message.senderSessionState
            session.appendMessageId(id)
            fireSessionStateChanged(session)
        }
        toggleServiceLiveHandler()
    }

    companion object {
        const val ACTION_APPLY_EXHANGE = "ACTION_APPLY_EXHANGE"
        const val ACTION_DECLINE = "ACTION_DECLINE"
        private val TAG = KeyExchangeService::class.java.simpleName
        private const val EXTRA_KEY_LOCATION_POLICY = "key_location_policy"
        private const val ACTION_PROCESS_MESSAGE = "ACTION_PROCESS_MESSAGE"
        private const val ACTION_INICIATE_KEY_EXCHANGE = "ACTION_INICIATE_KEY_EXCHANGE"
        private const val NOTIFICATION_KEY_EXCHANGE = 20
        private const val NOTIFICATION_KEY_EXCHANGE_REQUEST = 10
        private const val WHAT_STOP_SERVICE = 12
        fun intercept(context: Context, accountId: Long, dto: VKApiMessage?): Boolean {
            return if (dto == null) {
                false
            } else intercept(
                context,
                accountId,
                dto.peer_id,
                dto.id,
                dto.body,
                dto.out
            )
        }

        fun intercept(
            context: Context,
            accountId: Long,
            peerId: Long,
            messageId: Int,
            messageBody: String?,
            out: Boolean
        ): Boolean {
            messageBody ?: return false
            @MessageType val type = analizeMessageBody(messageBody)
            return if (type == MessageType.KEY_EXCHANGE) {
                try {
                    val exchangeMessageBody = messageBody.substring(3) // without RSA on start
                    val message: ExchangeMessage =
                        kJson.decodeFromString(
                            ExchangeMessage.serializer(),
                            exchangeMessageBody
                        )
                    if (!out) {
                        processMessage(context, accountId, peerId, messageId, message)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else false
        }

        @Throws(InvalidSessionStateException::class)
        internal fun assertSessionState(
            session: KeyExchangeSession,
            @SessionState requiredState: Int
        ) {
            if (session.localSessionState != requiredState) {
                throw InvalidSessionStateException("Invalid session state, require: " + requiredState + ", existing: " + session.localSessionState)
            }
        }

        internal fun sendMessageImpl(
            accountId: Long,
            peerId: Long,
            message: ExchangeMessage
        ): Single<Int> {
            return Single.just(Any())
                .delay(1, TimeUnit.SECONDS)
                .flatMap {
                    get()
                        .vkDefault(accountId)
                        .messages()
                        .send(
                            Random(System.nanoTime()).nextLong(),
                            peerId,
                            null,
                            message.toString(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                }
        }

        fun iniciateKeyExchangeSession(
            context: Context, accountId: Long,
            peerId: Long, @KeyLocationPolicy policy: Int
        ) {
            val intent = Intent(context, KeyExchangeService::class.java)
            intent.action = ACTION_INICIATE_KEY_EXCHANGE
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.PEER_ID, peerId)
            intent.putExtra(EXTRA_KEY_LOCATION_POLICY, policy)
            context.startService(intent)
        }

        private fun processMessage(
            context: Context,
            accountId: Long,
            peerId: Long,
            messageId: Int,
            message: ExchangeMessage
        ) {
            val intent = Intent(context, KeyExchangeService::class.java)
            intent.action = ACTION_PROCESS_MESSAGE
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.PEER_ID, peerId)
            intent.putExtra(Extra.MESSAGE_ID, messageId)
            intent.putExtra(Extra.MESSAGE, message)
            context.startService(intent)
        }

        fun createIntentForApply(
            context: Context,
            message: ExchangeMessage,
            accountId: Long,
            peerId: Long,
            messageId: Int
        ): Intent {
            val apply = Intent(context, KeyExchangeService::class.java)
            apply.action = ACTION_APPLY_EXHANGE
            apply.putExtra(Extra.ACCOUNT_ID, accountId)
            apply.putExtra(Extra.PEER_ID, peerId)
            apply.putExtra(Extra.MESSAGE_ID, messageId)
            apply.putExtra(Extra.MESSAGE, message)
            return apply
        }

        fun createIntentForDecline(
            context: Context,
            message: ExchangeMessage,
            accountId: Long,
            peerId: Long,
            messageId: Int
        ): Intent {
            val intent = Intent(context, KeyExchangeService::class.java)
            intent.action = ACTION_DECLINE
            intent.putExtra(Extra.MESSAGE, message)
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.PEER_ID, peerId)
            intent.putExtra(Extra.MESSAGE_ID, messageId)
            return intent
        }
    }
}