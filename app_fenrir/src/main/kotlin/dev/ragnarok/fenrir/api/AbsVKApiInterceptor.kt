package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.UserAgentTool.getAccountUserAgent
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.makeVK
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.exception.UnauthorizedException
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

abstract class AbsVKApiInterceptor(private val version: String) :
    Interceptor {
    protected abstract val token: String?

    @AccountType
    protected abstract val type: Int
    protected abstract val accountId: Long
    protected abstract val customDeviceName: String?

    /*
   private String RECEIPT_GMS_TOKEN() {
       try {
           GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
           int isGooglePlayServicesAvailable = instance.isGooglePlayServicesAvailable(Includes.provideApplicationContext());
           if (isGooglePlayServicesAvailable != 0) {
               return null;
           }
           return FirebaseInstanceId.getInstance().getToken("54740537194", "id" + getAccountId());
       } catch (Throwable th) {
           th.printStackTrace();
           return null;
       }
   }
    */

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val vkAccessToken: String = token.nonNullNoEmpty({ it },
            { throw UnauthorizedException("No authorization! Please, login and retry") })
        val original: Request = chain.request()

        val formBuilder = FormBody.Builder()
        val body = original.body
        var hasVersion = false
        var hasDeviceId = false
        var hasAccessToken = false
        if (body is FormBody) {
            for (i in 0 until body.size) {
                val name = body.name(i)
                when (name) {
                    "v" -> {
                        hasVersion = true
                    }

                    "device_id" -> hasDeviceId = true
                    "access_token" -> hasAccessToken = true
                }
                val value = body.value(i)
                formBuilder.add(name, value)
            }
        }
        if (!hasVersion) {
            formBuilder.add("v", version)
        }
        if (!hasAccessToken) {
            formBuilder.add("access_token", vkAccessToken)
        }
        formBuilder.add("lang", Constants.DEVICE_COUNTRY_CODE)
            .add("https", "1")
        if (!hasDeviceId) {
            formBuilder.add(
                "device_id",
                Utils.getDeviceId(provideApplicationContext())
            )
        }
        return chain.proceed(
            original.newBuilder().vkHeader(false)
                .addHeader("User-Agent", getAccountUserAgent(type, customDeviceName))
                .post(formBuilder.build())
                .makeVK(true)
                .build()
        )
    }
}
