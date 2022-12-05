package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.PostSourceDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = PostSourceDtoAdapter::class)
class VKApiPostSource {
    /**
     * На данный момент поддерживаются следующие типы источников записи на стене, значение которых указываются в поле type:
     * vk — запись создана через основной интерфейс сайта (http://vk.com/);
     * widget — запись создана через виджет на стороннем сайте;
     * api — запись создана приложением через API;
     * rss— запись создана посредством импорта RSS-ленты со стороннего сайта;
     * sms — запись создана посредством отправки SMS-сообщения на специальный номер.
     */
    var type = 0

    /**
     * может содержать название платформы, если оно доступно: android, iphone, wphone
     */
    var platform: String? = null

    /**
     * Поле data является опциональным и содержит следующие данные в зависимости от значения поля type:
     * vk — содержит тип действия, из-за которого была создана запись:
     * profile_activity — изменение статуса под именем пользователя;
     * profile_photo — изменение профильной фотографии пользователя;
     * widget — содержит тип виджета, через который была создана запись:
     * comments — виджет комментариев;
     * like — виджет «Мне нравится»;
     * poll — виджет опросов;
     */
    var data = 0

    /**
     * является опциональным и может содержать внешнюю ссылку на ресурс, с которого была опубликована запись.
     */
    var url: String? = null

    object Type {
        const val VK = 1
        const val WIDGET = 2
        const val API = 3
        const val RSS = 4
        const val SMS = 5
        fun parse(original: String?): Int {
            return when (original) {
                "vk" -> {
                    VK
                }
                "widget" -> {
                    WIDGET
                }
                "api" -> {
                    API
                }
                "rss" -> {
                    RSS
                }
                "sms" -> {
                    SMS
                }
                else -> {
                    0
                }
            }
        }
    }

    object Data {
        const val VK = 1
        const val PROFILE_ACTIVITY = 2
        const val PROFILE_PHOTO = 3
        const val WIDGET = 4
        const val COMMENTS = 5
        const val LIKE = 6
        const val POLL = 7
        fun parse(original: String?): Int {
            return when (original) {
                "vk" -> {
                    VK
                }
                "profile_activity" -> {
                    PROFILE_ACTIVITY
                }
                "profile_photo" -> {
                    PROFILE_PHOTO
                }
                "widget" -> {
                    WIDGET
                }
                "like" -> {
                    LIKE
                }
                "poll" -> {
                    POLL
                }
                else -> {
                    0
                }
            }
        }
    }
}