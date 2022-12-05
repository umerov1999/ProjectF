package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.GroupSettingsAdapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = GroupSettingsAdapter::class)
class GroupSettingsDto {
    var title: String? = null
    var description: String? = null
    var address: String? = null
    var place: Place? = null
    var country_id = 0
    var city_id = 0

    /**
     * 3 - закрытая
     * 0 - выключена
     * 1 - открытая
     * 2 - ограниченная
     */
    var wall = 0
    var photos = 0
    var video = 0
    var audio = 0
    var docs = 0
    var topics = 0
    var wiki = 0
    var obscene_filter = false
    var obscene_stopwords = false
    var obscene_words: Array<String>? = null
    var access = 0
    var subject = 0

    /**
     * "public_date": "13.0.2014"
     */
    var public_date: String? = null
    var public_date_label: String? = null

    /**
     * Может быть как int, так и String
     */
    var public_category: String? = null

    /**
     * Может быть как int, так и String
     */
    var public_subcategory: String? = null
    var public_category_list: List<PublicCategory>? = null
    var contacts = 0
    var links = 0
    var events = 0
    var places = 0
    var rss = false
    var website: String? = null
    var age_limits = 0
    var market: Market? = null

    @Serializable
    class Place {
        @SerialName("id")
        var id = 0

        @SerialName("title")
        var title: String? = null

        @SerialName("latitude")
        var latitude = 0.0

        @SerialName("longitude")
        var longitude = 0.0

        @SerialName("created")
        var created: Long = 0

        @SerialName("icon")
        var icon: String? = null

        @SerialName("group_id")
        var group_id = 0

        @SerialName("group_photo")
        var group_photo: String? = null

        @SerialName("checkins")
        var checkins = 0

        @SerialName("type")
        var type = 0

        @SerialName("country")
        var country = 0

        @SerialName("city")
        var city = 0

        @SerialName("address")
        var address: String? = null
    }

    @Serializable
    class PublicCategory {
        @SerialName("id")
        var id = 0

        @SerialName("name")
        var name: String? = null

        @SerialName("subtypes_list")
        var subtypes_list: List<PublicCategory>? = null
    }

    @Serializable
    class Market {
        @SerialName("enabled")
        var enabled = false

        @SerialName("comments_enabled")
        var comments_enabled = false

        /**
         * -1 Полный список
         * -2 Весь мир
         * -3 Страны СНГ
         */
        @SerialName("country_ids")
        var country_ids: IntArray? = null

        @SerialName("city_ids")
        var city_ids: IntArray? = null

        /**
         * Если в сообщения сообщества, то меньше ноля (напр. -72124992)
         */
        @SerialName("contact_id")
        var contact_id = 0

        @SerialName("currency")
        var currency: Currency? = null
    }

    @Serializable
    class Currency {
        @SerialName("id")
        var id = 0

        @SerialName("name")
        var name: String? = null
    }
}