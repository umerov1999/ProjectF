package dev.ragnarok.fenrir.view.emoji.section

class Emojicon {
    var emoji: String? = null
        private set

    private constructor()
    constructor(emoji: String?) {
        this.emoji = emoji
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val emojicon = other as Emojicon
        return emoji == emojicon.emoji
    }

    override fun hashCode(): Int {
        return emoji.hashCode()
    }

    companion object {

        fun fromCodePoint(codePoint: Int): Emojicon {
            val emoji = Emojicon()
            emoji.emoji = newString(codePoint)
            return emoji
        }


        fun fromChar(ch: Char): Emojicon {
            val emoji = Emojicon()
            emoji.emoji = ch.toString()
            return emoji
        }


        fun fromChars(chars: String?): Emojicon {
            val emoji = Emojicon()
            emoji.emoji = chars
            return emoji
        }


        private fun newString(codePoint: Int): String {
            return if (Character.charCount(codePoint) == 1) {
                codePoint.toString()
            } else {
                String(Character.toChars(codePoint))
            }
        }
    }
}