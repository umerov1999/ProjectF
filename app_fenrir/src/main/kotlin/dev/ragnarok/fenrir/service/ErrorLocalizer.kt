package dev.ragnarok.fenrir.service

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.nonNullNoEmpty
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorLocalizer {
    private val sApiLocalizer = ApiLocalizer()

    fun localizeThrowable(context: Context, throwable: Throwable?): String {
        throwable ?: return "null"
        return when (throwable) {
            is ApiException -> {
                val error = throwable.error
                api().getMessage(context, error.errorCode, error.errorMsg) ?: "null"
            }

            is SocketTimeoutException -> {
                context.getString(R.string.error_timeout_message)
            }

            is InterruptedIOException -> {
                if ("timeout" == throwable.message || "executor rejected" == throwable.message) {
                    context.getString(R.string.error_timeout_message)
                } else {
                    throwable.message.nonNullNoEmpty({ it }, { throwable.toString() })
                }
            }

            is ConnectException, is UnknownHostException -> {
                context.getString(R.string.error_unknown_host)
            }

            is NotFoundException -> {
                context.getString(R.string.error_not_found_message)
            }

            is HttpException -> {
                if (throwable.code < 0) {
                    context.getString(R.string.client_rest_shutdown)
                } else {
                    context.getString(R.string.vk_servers_error, throwable.code)
                }
            }

            else -> throwable.message.nonNullNoEmpty({ it }, { throwable.toString() })
        }
    }

    fun api(): Localizer {
        return sApiLocalizer
    }

    interface Localizer {
        fun getMessage(
            context: Context,
            code: Int,
            ifUnknown: String?,
            vararg params: Any?
        ): String?
    }

    open class BaseLocazer : Localizer {
        override fun getMessage(
            context: Context,
            code: Int,
            ifUnknown: String?,
            vararg params: Any?
        ): String? {
            return if (ifUnknown.isNullOrEmpty()) context.getString(R.string.unknown_error) else ifUnknown
        }
    }

    class ApiLocalizer : BaseLocazer() {
        override fun getMessage(
            context: Context,
            code: Int,
            ifUnknown: String?,
            vararg params: Any?
        ): String? {
            return when (code) {
                -1 -> context.getString(R.string.api_error_internal_1)
                1 -> context.getString(R.string.api_error_1)
                2 -> context.getString(R.string.api_error_2)
                3 -> context.getString(R.string.api_error_3)
                4 -> context.getString(R.string.api_error_4)
                ApiErrorCodes.USER_AUTHORIZATION_FAILED -> context.getString(R.string.api_error_5)
                6 -> context.getString(R.string.api_error_6)
                7 -> context.getString(R.string.api_error_7)
                8 -> context.getString(R.string.api_error_8)
                9 -> context.getString(R.string.api_error_9)
                10 -> context.getString(R.string.api_error_10)
                11 -> context.getString(R.string.api_error_11)
                ApiErrorCodes.CAPTCHA_NEED -> context.getString(R.string.api_error_14)
                ApiErrorCodes.ACCESS_DENIED -> context.getString(R.string.api_error_15)
                16 -> context.getString(R.string.api_error_16)
                17 -> context.getString(R.string.api_error_17)
                ApiErrorCodes.PAGE_HAS_BEEN_REMOVED_OR_BLOCKED -> context.getString(R.string.api_error_18)
                19 -> context.getString(R.string.api_error_19)
                20 -> context.getString(R.string.api_error_20)
                21 -> context.getString(R.string.api_error_21)
                23 -> context.getString(R.string.api_error_23)
                24 -> context.getString(R.string.api_error_24)
                25 -> context.getString(R.string.api_error_25)
                27 -> context.getString(R.string.api_error_27)
                28 -> context.getString(R.string.api_error_28)
                29 -> context.getString(R.string.api_error_29)
                30 -> context.getString(R.string.api_error_30)
                34 -> context.getString(R.string.api_error_34)
                42 -> context.getString(R.string.api_error_42)
                101 -> context.getString(R.string.api_error_101)
                103 -> context.getString(R.string.api_error_103)
                105 -> context.getString(R.string.api_error_105)
                113 -> context.getString(R.string.api_error_113)
                114 -> context.getString(R.string.api_error_114)
                118 -> context.getString(R.string.api_error_118)
                121 -> context.getString(R.string.api_error_121)
                150 -> context.getString(R.string.api_error_150)
                174 -> context.getString(R.string.api_error_174)
                175 -> context.getString(R.string.api_error_175)
                176 -> context.getString(R.string.api_error_176)
                200 -> context.getString(R.string.api_error_200)
                201 -> context.getString(R.string.api_error_201)
                203 -> context.getString(R.string.api_error_203)
                210 -> context.getString(R.string.api_error_210)
                214 -> context.getString(R.string.api_error_214)
                219 -> context.getString(R.string.api_error_219)
                220 -> context.getString(R.string.api_error_220)
                221 -> context.getString(R.string.api_error_221)
                300 -> context.getString(R.string.api_error_300)
                500 -> context.getString(R.string.api_error_500)
                600 -> context.getString(R.string.api_error_600)
                603 -> context.getString(R.string.api_error_603)
                701 -> context.getString(R.string.api_error_701)
                800 -> context.getString(R.string.api_error_800)
                900 -> context.getString(R.string.api_error_900)
                901 -> context.getString(R.string.api_error_901)
                902 -> context.getString(R.string.api_error_902)
                909 -> context.getString(R.string.api_error_909)
                914 -> context.getString(R.string.api_error_914)
                913 -> context.getString(R.string.api_error_913)
                921 -> context.getString(R.string.api_error_921)
                1150 -> context.getString(R.string.api_error_1150)
                1151 -> context.getString(R.string.api_error_1151)
                1170 -> context.getString(R.string.api_error_1170)
                else -> super.getMessage(context, code, ifUnknown, *params)
            }
        }
    }
}