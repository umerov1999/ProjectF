package dev.ragnarok.fenrir.fragment.base

import android.view.View
import android.view.ViewGroup
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.view.AudioContainer

class AttachmentsHolder {
    var vgAudios: AudioContainer? = null
        private set
    var voiceMessageRoot: ViewGroup? = null
        private set
    var vgDocs: ViewGroup? = null
        private set
    var vgArticles: ViewGroup? = null
        private set
    var vgStickers: ViewGroup? = null
        private set
    var vgPhotos: ViewGroup? = null
        private set
    var vgVideos: ViewGroup? = null
        private set
    var vgPosts: ViewGroup? = null
        private set
    var vgFriends: ViewGroup? = null
        private set

    fun setVgAudios(vgAudios: AudioContainer?): AttachmentsHolder {
        this.vgAudios = vgAudios
        return this
    }

    fun setVgVideos(vgVideos: ViewGroup?): AttachmentsHolder {
        this.vgVideos = vgVideos
        return this
    }

    fun setVgDocs(vgDocs: ViewGroup?): AttachmentsHolder {
        this.vgDocs = vgDocs
        return this
    }

    fun setVgArticles(vgArticles: ViewGroup?): AttachmentsHolder {
        this.vgArticles = vgArticles
        return this
    }

    fun setVgStickers(vgStickers: ViewGroup?): AttachmentsHolder {
        this.vgStickers = vgStickers
        return this
    }

    fun setVgPhotos(vgPhotos: ViewGroup?): AttachmentsHolder {
        this.vgPhotos = vgPhotos
        return this
    }

    fun setVgPosts(vgPosts: ViewGroup?): AttachmentsHolder {
        this.vgPosts = vgPosts
        return this
    }

    fun setVoiceMessageRoot(voiceMessageRoot: ViewGroup?): AttachmentsHolder {
        this.voiceMessageRoot = voiceMessageRoot
        return this
    }

    fun setVgFriends(vgFriends: ViewGroup?): AttachmentsHolder {
        this.vgFriends = vgFriends
        return this
    }

    companion object {

        fun forCopyPost(container: ViewGroup): AttachmentsHolder {
            val containers = AttachmentsHolder()
            containers.setVgStickers(container.findViewById(R.id.copy_history_stickers_attachments))
                .setVgPhotos(container.findViewById(R.id.copy_history_photo_attachments))
                .setVgAudios(container.findViewById(R.id.copy_history_audio_attachments))
                .setVgVideos(container.findViewById(R.id.copy_history_video_attachments))
                .setVgDocs(container.findViewById(R.id.copy_history_docs_attachments))
                .setVgArticles(container.findViewById(R.id.copy_history_articles_attachments))
            return containers
        }


        fun forPost(container: ViewGroup): AttachmentsHolder {
            val containers = AttachmentsHolder()
            containers.setVgStickers(container.findViewById(R.id.post_stickers_attachments))
                .setVgPosts(container.findViewById(R.id.post_posts_attachments))
                .setVgPhotos(container.findViewById(R.id.post_photo_attachments))
                .setVgAudios(container.findViewById(R.id.post_audio_attachments))
                .setVgVideos(container.findViewById(R.id.post_video_attachments))
                .setVgDocs(container.findViewById(R.id.post_docs_attachments))
                .setVgFriends(container.findViewById(R.id.post_friends_attachments))
                .setVgArticles(container.findViewById(R.id.post_articles_attachments))
            return containers
        }


        fun forComment(container: ViewGroup): AttachmentsHolder {
            val containers = AttachmentsHolder()
            containers.setVgStickers(container.findViewById(R.id.comments_stickers_attachments))
                .setVgPhotos(container.findViewById(R.id.comments_photo_attachments))
                .setVgAudios(container.findViewById(R.id.comments_audio_attachments))
                .setVgVideos(container.findViewById(R.id.comments_video_attachments))
                .setVgDocs(container.findViewById(R.id.comments_docs_attachments))
                .setVgArticles(container.findViewById(R.id.comments_articles_attachments))
            return containers
        }

        fun forFeedback(container: View): AttachmentsHolder {
            val containers = AttachmentsHolder()
            containers.setVgStickers(container.findViewById(R.id.feedback_stickers_attachments))
                .setVgPhotos(container.findViewById(R.id.feedback_photo_attachments))
                .setVgAudios(container.findViewById(R.id.feedback_audio_attachments))
                .setVgVideos(container.findViewById(R.id.feedback_video_attachments))
                .setVgDocs(container.findViewById(R.id.feedback_docs_attachments))
                .setVgArticles(container.findViewById(R.id.feedback_articles_attachments))
            return containers
        }
    }
}