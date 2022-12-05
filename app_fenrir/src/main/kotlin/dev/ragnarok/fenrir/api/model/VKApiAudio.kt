package dev.ragnarok.fenrir.api.model

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.adapters.AudioDtoAdapter
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.IIdComparable
import kotlinx.serialization.Serializable

/**
 * An audio object describes an audio file and contains the following fields.
 */
@Serializable(with = AudioDtoAdapter::class)
class VKApiAudio
/**
 * Creates empty Audio instance.
 */
    : VKApiAttachment, IIdComparable {
    /**
     * Audio ID.
     */
    var id = 0

    /**
     * Audio owner ID.
     */
    var owner_id = 0

    /**
     * Artist name.
     */
    var artist: String? = null

    /**
     * Audio file title.
     */
    var title: String? = null

    /**
     * Duration (in seconds).
     */
    var duration = 0

    /**
     * Link to mp3 or hls.
     */
    var url: String? = null

    /**
     * ID of the lyrics (if available) of the audio file.
     */
    var lyrics_id = 0

    var date: Long = 0

    /**
     * ID of the album containing the audio file (if assigned).
     */
    var album_id = 0
    var album_owner_id = 0
    var album_access_key: String? = null

    /**
     * Genre ID. See the list of audio genres.
     */
    var genre_id = 0
    var thumb_image_little: String? = null
    var thumb_image_big: String? = null
    var thumb_image_very_big: String? = null
    var album_title: String? = null
    var main_artists: Map<String, String>? = null
    var isHq = false

    /**
     * An access key using for get information about hidden objects.
     */
    var access_key: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_AUDIO
    }

    /**
     * Audio object genres.
     */
    object Genre {
        const val TOP_ALL = 0
        const val ROCK = 1
        const val POP = 2
        const val EASY_LISTENING = 4
        const val DANCE_AND_HOUSE = 5
        const val INSTRUMENTAL = 6
        const val METAL = 7
        const val DRUM_AND_BASS = 10
        const val TRANCE = 11
        const val CHANSON = 12
        const val ETHNIC = 13
        const val ACOUSTIC_AND_VOCAL = 14
        const val REGGAE = 15
        const val CLASSICAL = 16
        const val INDIE_POP = 17
        const val OTHER = 18
        const val SPEECH = 19
        const val ALTERNATIVE = 21
        const val ELECTROPOP_AND_DISCO = 22
        const val JAZZ_AND_BLUES = 1001
        fun getTitleByGenre(context: Context, genre: Int): String? {
            when (genre) {
                TOP_ALL -> return context.getString(R.string.top)
                ACOUSTIC_AND_VOCAL -> return "#" + context.getString(R.string.acoustic)
                ALTERNATIVE -> return "#" + context.getString(R.string.alternative)
                CHANSON -> return "#" + context.getString(R.string.chanson)
                CLASSICAL -> return "#" + context.getString(R.string.classical)
                DANCE_AND_HOUSE -> return "#" + context.getString(R.string.dance)
                DRUM_AND_BASS -> return "#" + context.getString(R.string.drum_and_bass)
                EASY_LISTENING -> return "#" + context.getString(R.string.easy_listening)
                ELECTROPOP_AND_DISCO -> return "#" + context.getString(R.string.disco)
                ETHNIC -> return "#" + context.getString(R.string.ethnic)
                INDIE_POP -> return "#" + context.getString(R.string.indie_pop)
                INSTRUMENTAL -> return "#" + context.getString(R.string.instrumental)
                METAL -> return "#" + context.getString(R.string.metal)
                OTHER -> return "#" + context.getString(R.string.other)
                POP -> return "#" + context.getString(R.string.pop)
                REGGAE -> return "#" + context.getString(R.string.reggae)
                ROCK -> return "#" + context.getString(R.string.rock)
                SPEECH -> return "#" + context.getString(R.string.speech)
                TRANCE -> return "#" + context.getString(R.string.trance)
                JAZZ_AND_BLUES -> return "#" + context.getString(R.string.jazz_and_blues)
            }
            return null
        }
    }

    override fun compareFullId(object_s: String): Boolean {
        return (owner_id.toString() + "_" + id) == object_s
    }
}