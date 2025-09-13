package dev.ragnarok.fenrir.push

open class VKPlace {
    class Photo(val ownerId: Long, val photoId: Int) : VKPlace() {
        override fun toString(): String {
            return "Photo{" +
                    "ownerId=" + ownerId +
                    ", photoId=" + photoId +
                    '}'
        }
    }

    class PhotoComment(val ownerId: Long, val photoId: Int) : VKPlace() {
        override fun toString(): String {
            return "PhotoComment{" +
                    "ownerId=" + ownerId +
                    ", photoId=" + photoId +
                    '}'
        }
    }

    class WallComment(val ownerId: Long, val postId: Int) : VKPlace() {
        override fun toString(): String {
            return "WallComment{" +
                    "ownerId=" + ownerId +
                    ", postId=" + postId +
                    '}'
        }
    }

    class WallPost(val ownerId: Long, val postId: Int) : VKPlace() {
        override fun toString(): String {
            return "WallPost{" +
                    "ownerId=" + ownerId +
                    ", postId=" + postId +
                    '}'
        }
    }

    class Video(val ownerId: Long, val videoId: Int) : VKPlace() {
        override fun toString(): String {
            return "Video{" +
                    "ownerId=" + ownerId +
                    ", videoId=" + videoId +
                    '}'
        }
    }

    class VideoComment(val ownerId: Long, val videoId: Int) : VKPlace() {
        override fun toString(): String {
            return "VideoComment{" +
                    "ownerId=" + ownerId +
                    ", videoId=" + videoId +
                    '}'
        }
    }

    companion object {
        //+ wall_comment26632922_4630
        //+ wall25651989_3738
        //+ photo25651989_415613803
        //+ wall_comment-88914001_50005
        //+ photo_comment246484771_456239032
        //+ video25651989_171388574
        private val PATTERN_PHOTO: Regex = Regex("photo(-?\\d+)_(\\d+)")
        private val PATTERN_PHOTO_COMMENT: Regex = Regex("photo_comment(-?\\d+)_(\\d+)")
        private val PATTERN_VIDEO: Regex = Regex("video(-?\\d+)_(\\d+)")
        private val PATTERN_VIDEO_COMMENT: Regex = Regex("video_comment(-?\\d+)_(\\d+)")
        private val PATTERN_WALL: Regex = Regex("wall(-?\\d+)_(\\d+)")
        private val PATTERN_WALL_COMMENT: Regex = Regex("wall_comment(-?\\d+)_(\\d+)")
        fun parse(obj: String?): VKPlace? {
            obj ?: return null
            try {
                PATTERN_PHOTO.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val photoId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return Photo(ownerId, photoId)
                }
                PATTERN_PHOTO_COMMENT.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val photoId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return PhotoComment(ownerId, photoId)
                }
                PATTERN_WALL.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val postId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return WallPost(ownerId, postId)
                }
                PATTERN_WALL_COMMENT.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val postId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return WallComment(ownerId, postId)
                }
                PATTERN_VIDEO.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val videoId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return Video(ownerId, videoId)
                }
                PATTERN_VIDEO_COMMENT.find(obj)?.let {
                    val ownerId = it.groupValues.getOrNull(1)?.toLong() ?: return null
                    val videoId = it.groupValues.getOrNull(2)?.toInt() ?: return null
                    return VideoComment(ownerId, videoId)
                }
            } catch (_: Exception) {
            }
            return null
        }
    }
}
