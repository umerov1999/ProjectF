package dev.ragnarok.fenrir.push

import android.content.Context
import android.os.Build
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.FCMToken
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.service.ApiErrorCodes
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.VKPushRegistration
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.deviceName
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import java.util.Locale

class PushRegistrationResolver(
    private val settings: ISettings,
    private val networker: INetworker
) : IPushRegistrationResolver {
    override fun canReceivePushNotification(accountId: Long): Boolean {
        if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            return false
        }
        val available = settings.pushSettings().registrations
        val can = available.size == 1 && available[0].userId == accountId
        d(
            TAG, "canReceivePushNotification, reason: " + can.toString()
                .uppercase(Locale.getDefault())
        )
        return can
    }

    override fun resolvePushRegistration(accountId: Long, context: Context): Flow<Boolean> {
        return FCMToken.fcmToken
            .flatMapConcat { fcmToken ->
                val available = settings.pushSettings().registrations
                if (accountId == ISettings.IAccountsSettings.INVALID_ID && available.isEmpty() || accountId <= 0 || settings.accounts()
                        .getType(accountId) != Constants.DEFAULT_ACCOUNT_TYPE
                ) {
                    emptyTaskFlow()
                } else {
                    val deviceId =
                        Utils.getDeviceId(Settings.get().accounts().getType(accountId), context)
                    val needUnregister: MutableSet<VKPushRegistration> = HashSet(0)
                    var hasOk = false
                    var hasRemove = false
                    for (registered in available) {
                        val reason = analyzeRegistration(registered, deviceId, fcmToken, accountId)
                        d(TAG, "Reason: $reason")
                        when (reason) {
                            Reason.UNREGISTER_AND_REMOVE -> needUnregister.add(registered)
                            Reason.REMOVE -> hasRemove = true
                            Reason.OK -> hasOk = true
                        }
                    }
                    if (hasOk && !hasRemove && needUnregister.isEmpty()) {
                        d(TAG, "Has auth, valid registration, OK")
                        emptyTaskFlow()
                    } else {
                        var completable = emptyTaskFlow()
                        for (unregistered in needUnregister) {
                            completable = completable.andThen(unregister(unregistered))
                        }
                        val target: MutableList<VKPushRegistration> = ArrayList()
                        if (!hasOk) {
                            val vkToken = settings.accounts().getAccessToken(accountId)
                            if (vkToken != null) {
                                val current =
                                    VKPushRegistration().set(
                                        accountId,
                                        deviceId,
                                        vkToken,
                                        fcmToken
                                    )
                                target.add(current)
                                completable = completable.andThen(register(current))
                            }
                        }
                        completable
                            .map {
                                settings.pushSettings().savePushRegistrations(target)
                                d(TAG, "Register success")
                                true
                            }
                            .catch { d(TAG, "Register error, t: $it") }
                    }
                }
            }
    }

    private fun register(registration: VKPushRegistration): Flow<Boolean> {
        //try {
        /*
            JSONArray fr_of_fr = new JSONArray();
            fr_of_fr.put("fr_of_fr");

            JSONObject json = new JSONObject();
            json.put("msg", "on"); // личные сообщения +
            json.put("sdk_open", "on");
            json.put("mention", "on");
            json.put("event_soon", "on");
            json.put("app_request", "on");
            json.put("chat", "on"); // групповые чаты +
            json.put("wall_post", "on"); // новая запись на стене пользователя +
            json.put("comment", "on"); // комментарии +
            json.put("reply", "on"); // ответы +
            json.put("wall_publish", "on"); // размещение предложенной новости +
            json.put("friend", "on");  // запрос на добавления в друзья +
            json.put("friend_accepted", "on"); // подтверждение заявки в друзья +
            json.put("group_invite", "on"); // приглашение в сообщество +
            json.put("birthday", "on"); // уведомления о днях рождениях на текущую дату

            //(хер приходят)
            json.put("like", fr_of_fr); // отметки "Мне нравится"
            json.put("group_accepted", fr_of_fr); // подтверждение заявки на вступление в группу - (хер приходят) 09.01.2016
            json.put("mention", fr_of_fr); // упоминания - (хер приходят) 09.01.2016
            json.put("repost", fr_of_fr); // действия "Рассказать друзьям" - (хер приходят) 09.01.2016

            json.put("new_post", "on"); //записи выбранных людей и сообществ;

            String targetSettingsStr = json.toString();

             */
        val deviceModel = deviceName
        //String osVersion = Utils.getAndroidVersion();
        return if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.KATE) {
            networker.vkManual(registration.userId, registration.vkAccessToken)
                .account()
                .registerDevice(
                    Constants.API_ID,
                    Constants.API_ID,
                    registration.fcmToken,
                    null,
                    null,
                    "fcm",
                    null,
                    null,
                    deviceModel,
                    registration.deviceId,
                    Build.VERSION.RELEASE,
                    "{\"msg\":\"on\",\"chat\":\"on\",\"friend\":\"on\",\"reply\":\"on\",\"comment\":\"on\",\"mention\":\"on\",\"like\":\"off\"}"
                )
                .ignoreElement()
        } else {
            networker.vkManual(registration.userId, registration.vkAccessToken)
                .account()
                .registerDevice(
                    Constants.API_ID,
                    Constants.API_ID,
                    registration.fcmToken,
                    1,
                    Constants.VK_ANDROID_APP_VERSION_CODE,
                    "fcm",
                    "vk_client",
                    4,
                    deviceModel,
                    registration.deviceId,
                    Build.VERSION.RELEASE,
                    null
                )
                .ignoreElement()
        }
    }

    private fun unregister(registration: VKPushRegistration): Flow<Boolean> {
        return networker.vkManual(registration.userId, registration.vkAccessToken)
            .account()
            .unregisterDevice(registration.deviceId)
            .ignoreElement()
            .catch {
                val cause = getCauseIfRuntime(it)
                if (cause is ApiException && cause.error.errorCode == ApiErrorCodes.USER_AUTHORIZATION_FAILED) {
                    return@catch emit(false)
                }
                throw it
            }
    }

    private fun analyzeRegistration(
        available: VKPushRegistration,
        deviceId: String,
        fcmToken: String,
        accountId: Long
    ): Reason {
        when {
            deviceId != available.deviceId -> {
                return Reason.REMOVE
            }

            fcmToken != available.fcmToken -> {
                return Reason.REMOVE
            }

            else -> {
                if (available.userId != accountId) {
                    return Reason.UNREGISTER_AND_REMOVE
                }
                val currentVkToken = settings.accounts().getAccessToken(accountId)
                return if (available.vkAccessToken != currentVkToken) {
                    Reason.REMOVE
                } else Reason.OK
            }
        }
    }

    private enum class Reason {
        OK, REMOVE, UNREGISTER_AND_REMOVE
    }

    companion object {
        private val TAG = PushRegistrationResolver::class.simpleName.orEmpty()
    }
}