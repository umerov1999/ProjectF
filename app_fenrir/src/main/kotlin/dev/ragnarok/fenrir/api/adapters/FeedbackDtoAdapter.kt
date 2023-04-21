package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.feedback.*
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.decodeFromJsonElementOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class FeedbackDtoAdapter : AbsDtoAdapter<VKApiBaseFeedback>("VKApiBaseFeedback") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiBaseFeedback {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.jsonObject
        return when (val type = optString(root, "type")) {
            "follow", "friend_accepted" -> USERS_PARSER.parse(
                root
            )

            "mention" -> MENTION_WALL_PARSER.parse(root)
            "wall", "wall_publish" -> WALL_PARSER.parse(root)
            "comment_photo", "comment_post", "comment_video" -> CREATE_COMMENT_PARSER.parse(
                root
            )

            "reply_comment", "reply_comment_photo", "reply_comment_video", "reply_topic" -> REPLY_COMMENT_PARSER.parse(
                root
            )

            "like_video", "like_photo", "like_post" -> LIKE_PARSER.parse(
                root
            )

            "like_comment_photo", "like_comment_video", "like_comment_topic", "like_comment" -> LIKE_COMMENT_PARSER.parse(
                root
            )

            "copy_post", "copy_photo", "copy_video" -> COPY_PARSER.parse(
                root
            )

            "mention_comment_photo", "mention_comment_video", "mention_comments" -> MENTION_COMMENT_PARSER.parse(
                root
            )

            else -> throw UnsupportedOperationException("unsupported type $type")
        }
    }

    private abstract class Parser<T : VKApiBaseFeedback?> {
        protected abstract fun createDto(): T
        open fun parse(root: JsonObject): T {
            val dto = createDto()
            dto?.type = optString(root, "type")
            dto?.date = optLong(root, "date")
            if (hasObject(root, "reply")) {
                dto?.reply =
                    kJson.decodeFromJsonElementOrNull(
                        VKApiComment.serializer(),
                        root["reply"]
                    )
            }
            return dto
        }
    }

    private class BaseCopyParser : Parser<VKApiCopyFeedback>() {
        override fun createDto(): VKApiCopyFeedback {
            return VKApiCopyFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiCopyFeedback {
            val dto = super.parse(root)
            dto.copies = kJson.decodeFromJsonElementOrNull(Copies.serializer(), root["feedback"])
            dto.what = when (dto.type) {
                "copy_post" -> kJson.decodeFromJsonElementOrNull(
                    VKApiPost.serializer(),
                    root["parent"]
                )

                "copy_photo" -> kJson.decodeFromJsonElementOrNull(
                    VKApiPhoto.serializer(),
                    root["parent"]
                )

                "copy_video" -> kJson.decodeFromJsonElementOrNull(
                    VKApiVideo.serializer(),
                    root["parent"]
                )

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            return dto
        }
    }

    private class BaseCreateCommentParser : Parser<VKApiCommentFeedback>() {
        override fun createDto(): VKApiCommentFeedback {
            return VKApiCommentFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiCommentFeedback {
            val dto = super.parse(root)
            dto.comment = root["feedback"]?.let {
                kJson.decodeFromJsonElement(VKApiComment.serializer(), it)
            }
            dto.comment_of = when (dto.type) {
                "comment_post" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(
                        VKApiPost.serializer(),
                        it
                    )
                }

                "comment_photo" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(
                        VKApiPhoto.serializer(),
                        it
                    )
                }

                "comment_video" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(
                        VKApiVideo.serializer(),
                        it
                    )
                }

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            return dto
        }
    }

    private class BaseReplyCommentParser : Parser<VKApiReplyCommentFeedback>() {
        override fun createDto(): VKApiReplyCommentFeedback {
            return VKApiReplyCommentFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiReplyCommentFeedback {
            val dto = super.parse(root)
            dto.feedback_comment =
                root["feedback"]?.let {
                    kJson.decodeFromJsonElement(VKApiComment.serializer(), it)
                }
            if ("reply_topic" == dto.type) {
                dto.own_comment = null
                dto.comments_of = root["parent"]?.let {
                    kJson.decodeFromJsonElement(VKApiTopic.serializer(), it)
                }
                return dto
            }
            dto.own_comment = root["parent"]?.let {
                kJson.decodeFromJsonElement(VKApiComment.serializer(), it)
            }
            dto.comments_of = when (dto.type) {
                "reply_comment" -> {
                    root["parent"]?.asJsonObjectSafe?.get("post")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiPost.serializer(),
                            it
                        )
                    }
                }

                "reply_comment_photo" -> {
                    root["parent"]?.asJsonObjectSafe?.get("photo")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiPhoto.serializer(),
                            it
                        )
                    }
                }

                "reply_comment_video" -> {
                    root["parent"]?.asJsonObjectSafe?.get("video")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiVideo.serializer(),
                            it
                        )
                    }
                }

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            return dto
        }
    }

    private class BaseUsersParser : Parser<VKApiUsersFeedback>() {
        override fun createDto(): VKApiUsersFeedback {
            return VKApiUsersFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiUsersFeedback {
            val dto = super.parse(root)
            dto.users = root["feedback"]?.let {
                kJson.decodeFromJsonElement(UserArray.serializer(), it)
            }
            return dto
        }
    }

    private class LikeParser : Parser<VKApiLikeFeedback>() {
        override fun createDto(): VKApiLikeFeedback {
            return VKApiLikeFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiLikeFeedback {
            val dto = super.parse(root)
            dto.liked = when (dto.type) {
                "like_photo" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(VKApiPhoto.serializer(), it)
                }

                "like_post" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(VKApiPost.serializer(), it)
                }

                "like_video" -> root["parent"]?.let {
                    kJson.decodeFromJsonElement(VKApiVideo.serializer(), it)
                }

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            dto.users = root["feedback"]?.let {
                kJson.decodeFromJsonElement(UserArray.serializer(), it)
            }
            return dto
        }
    }

    private class BaseLikeCommentParser : Parser<VKApiLikeCommentFeedback>() {
        override fun createDto(): VKApiLikeCommentFeedback {
            return VKApiLikeCommentFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiLikeCommentFeedback {
            val dto = super.parse(root)
            dto.commented = when (dto.type) {
                "like_comment" -> {
                    root["parent"]?.asJsonObjectSafe?.get("post")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiPost.serializer(),
                            it
                        )
                    }
                }

                "like_comment_photo" -> {
                    root["parent"]?.asJsonObjectSafe?.get("photo")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiPhoto.serializer(),
                            it
                        )
                    }
                }

                "like_comment_video" -> {
                    root["parent"]?.asJsonObjectSafe?.get("video")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiVideo.serializer(),
                            it
                        )
                    }
                }

                "like_comment_topic" -> {
                    root["parent"]?.asJsonObjectSafe?.get("topic")?.let {
                        kJson.decodeFromJsonElement(
                            VKApiTopic.serializer(),
                            it
                        )
                    }
                }

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            dto.users = root["feedback"]?.let {
                kJson.decodeFromJsonElement(UserArray.serializer(), it)
            }
            dto.comment = root["parent"]?.let {
                kJson.decodeFromJsonElement(VKApiComment.serializer(), it)
            }
            return dto
        }
    }

    private class BaseMentionWallParser : Parser<VKApiMentionWallFeedback>() {
        override fun createDto(): VKApiMentionWallFeedback {
            return VKApiMentionWallFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiMentionWallFeedback {
            val dto = super.parse(root)
            dto.post = root["feedback"]?.let {
                kJson.decodeFromJsonElement(VKApiPost.serializer(), it)
            }
            return dto
        }
    }

    private class BaseWallParser : Parser<VKApiWallFeedback>() {
        override fun createDto(): VKApiWallFeedback {
            return VKApiWallFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiWallFeedback {
            val dto = super.parse(root)
            dto.post = root["feedback"]?.let {
                kJson.decodeFromJsonElement(VKApiPost.serializer(), it)
            }
            return dto
        }
    }

    private class BaseMentionCommentParser : Parser<VKApiMentionCommentFeedback>() {
        override fun createDto(): VKApiMentionCommentFeedback {
            return VKApiMentionCommentFeedback()
        }

        override fun parse(
            root: JsonObject
        ): VKApiMentionCommentFeedback {
            val dto = super.parse(root)
            dto.where = root["feedback"]?.let {
                kJson.decodeFromJsonElement(VKApiComment.serializer(), it)
            }
            dto.comment_of = when (dto.type) {
                "mention_comments" ->
                    root["parent"]?.let {
                        kJson.decodeFromJsonElement(VKApiPost.serializer(), it)
                    }

                "mention_comment_photo" ->
                    root["parent"]?.let {
                        kJson.decodeFromJsonElement(VKApiPhoto.serializer(), it)
                    }

                "mention_comment_video" ->
                    root["parent"]?.let {
                        kJson.decodeFromJsonElement(VKApiVideo.serializer(), it)
                    }

                else -> throw UnsupportedOperationException("Unsupported feedback type: " + dto.type)
            }
            return dto
        }
    }

    companion object {
        private val TAG = FeedbackDtoAdapter::class.java.simpleName
        private val MENTION_COMMENT_PARSER = BaseMentionCommentParser()
        private val LIKE_COMMENT_PARSER = BaseLikeCommentParser()
        private val COPY_PARSER = BaseCopyParser()
        private val CREATE_COMMENT_PARSER = BaseCreateCommentParser()
        private val REPLY_COMMENT_PARSER = BaseReplyCommentParser()
        private val LIKE_PARSER = LikeParser()
        private val USERS_PARSER = BaseUsersParser()
        private val MENTION_WALL_PARSER = BaseMentionWallParser()
        private val WALL_PARSER = BaseWallParser()
    }
}