package dev.ragnarok.fenrir.model

class VideoWithOwner(private val video: Video, private val owner: Owner) {
    fun getVideo(): Video {
        return video
    }

    fun getOwner(): Owner {
        return owner
    }
}