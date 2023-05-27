package dev.ragnarok.fenrir.link.types

class StoryLink(val ownerId: Long, val storyId: Int, val access_key: String?) : AbsLink(STORY) {
    override fun toString(): String {
        return "StoryLink{" +
                "ownerId=" + ownerId +
                ", storyId=" + storyId +
                ", Access_Key=" + access_key +
                '}'
    }
}
