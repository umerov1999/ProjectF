package dev.ragnarok.fenrir.push

import java.util.regex.Pattern

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
        private val PATTERN_PHOTO = Pattern.compile("photo(-?\\d+)_(\\d+)")
        private val PATTERN_PHOTO_COMMENT = Pattern.compile("photo_comment(-?\\d+)_(\\d+)")
        private val PATTERN_VIDEO = Pattern.compile("video(-?\\d+)_(\\d+)")
        private val PATTERN_VIDEO_COMMENT = Pattern.compile("video_comment(-?\\d+)_(\\d+)")
        private val PATTERN_WALL = Pattern.compile("wall(-?\\d+)_(\\d+)")
        private val PATTERN_WALL_COMMENT = Pattern.compile("wall_comment(-?\\d+)_(\\d+)")
        fun parse(obj: String?): VKPlace? {
            var matcher = obj?.let { PATTERN_PHOTO.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val photoId = matcher.group(2)?.toInt() ?: return null
                return Photo(ownerId, photoId)
            }
            matcher = obj?.let { PATTERN_PHOTO_COMMENT.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val photoId = matcher.group(2)?.toInt() ?: return null
                return PhotoComment(ownerId, photoId)
            }
            matcher = obj?.let { PATTERN_WALL.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val postId = matcher.group(2)?.toInt() ?: return null
                return WallPost(ownerId, postId)
            }
            matcher = obj?.let { PATTERN_WALL_COMMENT.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val postId = matcher.group(2)?.toInt() ?: return null
                return WallComment(ownerId, postId)
            }
            matcher = obj?.let { PATTERN_VIDEO.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val videoId = matcher.group(2)?.toInt() ?: return null
                return Video(ownerId, videoId)
            }
            matcher = obj?.let { PATTERN_VIDEO_COMMENT.matcher(it) }
            if (matcher?.find() == true) {
                val ownerId = matcher.group(1)?.toLong() ?: return null
                val videoId = matcher.group(2)?.toInt() ?: return null
                return VideoComment(ownerId, videoId)
            }
            return null
        }
    }
}
