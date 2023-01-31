package dev.ragnarok.fenrir.push

import android.os.Build
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.FCMToken
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.service.ApiErrorCodes
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.VKPushRegistration
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.deviceName
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Locale

class PushRegistrationResolver(
    private val deviceIdProvider: IDeviceIdProvider,
    private val settings: ISettings,
    private val networker: INetworker
) : IPushRegistrationResolver {
    override fun canReceivePushNotification(): Boolean {
        val accountId = settings.accounts().current
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

    override fun resolvePushRegistration(): Completable {
        return info
            .observeOn(Schedulers.io())
            .flatMapCompletable { data: Data ->
                val available = settings.pushSettings().registrations
                val accountId = settings.accounts().current
                val hasAuth = accountId != ISettings.IAccountsSettings.INVALID_ID
                if (!hasAuth && available.isEmpty()) {
                    d(TAG, "No auth, no regsitrations, OK")
                    return@flatMapCompletable Completable.complete()
                }
                if (accountId <= 0 || settings.accounts()
                        .getType(settings.accounts().current) != Constants.DEFAULT_ACCOUNT_TYPE
                ) return@flatMapCompletable Completable.never()
                val needUnregister: MutableSet<VKPushRegistration> = HashSet(0)
                val optionalAccountId: Optional<Long> = if (hasAuth) wrap(accountId) else empty()
                var hasOk = false
                var hasRemove = false
                for (registered in available) {
                    val reason = analizeRegistration(registered, data, optionalAccountId)
                    d(TAG, "Reason: $reason")
                    when (reason) {
                        Reason.UNREGISTER_AND_REMOVE -> needUnregister.add(registered)
                        Reason.REMOVE -> hasRemove = true
                        Reason.OK -> hasOk = true
                    }
                }
                if (hasAuth && hasOk && !hasRemove && needUnregister.isEmpty()) {
                    d(TAG, "Has auth, valid registration, OK")
                    return@flatMapCompletable Completable.complete()
                }
                var completable = Completable.complete()
                for (unreg in needUnregister) {
                    completable = completable.andThen(unregister(unreg))
                }
                val target: MutableList<VKPushRegistration> = ArrayList()
                if (!hasOk && hasAuth) {
                    val vkToken = settings.accounts().getAccessToken(accountId)
                    vkToken ?: return@flatMapCompletable Completable.complete()
                    val current =
                        VKPushRegistration().set(
                            accountId,
                            data.deviceId ?: "",
                            vkToken,
                            data.gcmToken
                        )
                    target.add(current)
                    completable = completable.andThen(register(current))
                }
                completable
                    .doOnComplete { settings.pushSettings().savePushRegistations(target) }
                    .doOnComplete { d(TAG, "Register success") }
                    .doOnError { throwable -> d(TAG, "Register error, t: $throwable") }
            }
    }

    private fun register(registration: VKPushRegistration): Completable {
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
            networker.vkManual(registration.userId, registration.vkToken)
                .account()
                .registerDevice(
                    registration.gmcToken,
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
            networker.vkManual(registration.userId, registration.vkToken)
                .account()
                .registerDevice(
                    registration.gmcToken,
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
        //} catch (JSONException e) {
        //return Completable.error(e);
        //}
    }

    private fun unregister(registration: VKPushRegistration): Completable {
        return networker.vkManual(registration.userId, registration.vkToken)
            .account()
            .unregisterDevice(registration.deviceId)
            .ignoreElement()
            .onErrorResumeNext { t ->
                val cause = getCauseIfRuntime(t)
                if (cause is ApiException && cause.error.errorCode == ApiErrorCodes.USER_AUTHORIZATION_FAILED) {
                    return@onErrorResumeNext Completable.complete()
                }
                Completable.error(t)
            }
    }

    private fun analizeRegistration(
        available: VKPushRegistration,
        data: Data,
        optionAccountId: Optional<Long>
    ): Reason {
        if (data.deviceId != available.deviceId) {
            return Reason.REMOVE
        }
        if (data.gcmToken != available.gmcToken) {
            return Reason.REMOVE
        }
        if (optionAccountId.isEmpty) {
            return Reason.UNREGISTER_AND_REMOVE
        }
        val currentAccountId = optionAccountId.get()
        if (available.userId != currentAccountId) {
            return Reason.UNREGISTER_AND_REMOVE
        }
        val currentVkToken = settings.accounts().getAccessToken(currentAccountId)
        return if (available.vkToken != currentVkToken) {
            Reason.REMOVE
        } else Reason.OK
    }

    private val info: Single<Data>
        get() = FCMToken.fcmToken.flatMap { s ->
            val data = Data(s, deviceIdProvider.deviceId)
            Single.just(data)
        }

    private enum class Reason {
        OK, REMOVE, UNREGISTER_AND_REMOVE
    }

    private class Data(val gcmToken: String, val deviceId: String?)
    companion object {
        private val TAG = PushRegistrationResolver::class.java.simpleName
    }
}