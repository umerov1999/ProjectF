package dev.ragnarok.fenrir.link.types

class FaveLink @JvmOverloads constructor(val section: String? = null) : AbsLink(FAVE) {
    override fun toString(): String {
        return "FaveLink{" +
                "section='" + section + '\'' +
                '}'
    }

    companion object {
        const val SECTION_VIDEOS = "likes_video"
        const val SECTION_PHOTOS = "likes_photo"
        const val SECTION_POSTS = "likes_posts"
        const val SECTION_PAGES = "pages"
        const val SECTION_LINKS = "links"
        const val SECTION_ARTICLES = "articles"
        const val SECTION_PRODUCTS = "products"
    }
}