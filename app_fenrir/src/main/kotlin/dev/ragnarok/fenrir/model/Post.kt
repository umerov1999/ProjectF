package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.model.VKApiPost

class Post : AbsModel, Cloneable {
    var dbid = 0
        private set
    var vkid = 0
        private set
    var ownerId = 0
        private set
    var author: Owner? = null
        private set
    var authorId = 0
        private set
    var date: Long = 0
        private set
    var text: String? = null
        private set
    var replyOwnerId = 0
        private set
    var replyPostId = 0
        private set
    var isFriendsOnly = false
        private set
    var commentsCount = 0
        private set
    var isCanPostComment = false
        private set
    var likesCount = 0
        private set
    var isCanLike = false
        private set
    var isUserLikes = false
        private set
    var repostCount = 0
        private set
    var isCanRepost = false
        private set
    var isUserReposted = false
        private set
    var postType = 0
        private set
    var attachments: Attachments? = null
        private set
    var signerId = 0
        private set
    var creator: User? = null
        private set
    var isCanPin = false
        private set
    var isPinned = false
        private set
    private var copyHierarchy: ArrayList<Post>? = null
    var isDeleted = false
        private set
    var source: PostSource? = null
        private set
    var viewCount = 0
        private set
    var creatorId = 0
        private set
    var isCanEdit = false
        private set
    var isFavorite = false
        private set
    var copyright: Copyright? = null
        private set
    var isDonut = false
        private set

    internal constructor(parcel: Parcel) {
        dbid = parcel.readInt()
        vkid = parcel.readInt()
        ownerId = parcel.readInt()
        authorId = parcel.readInt()
        author = Owner.readOwnerFromParcel(authorId, parcel)
        date = parcel.readLong()
        text = parcel.readString()
        replyOwnerId = parcel.readInt()
        replyPostId = parcel.readInt()
        isFriendsOnly = parcel.getBoolean()
        commentsCount = parcel.readInt()
        isCanPostComment = parcel.getBoolean()
        likesCount = parcel.readInt()
        isCanLike = parcel.getBoolean()
        repostCount = parcel.readInt()
        isCanRepost = parcel.getBoolean()
        isUserReposted = parcel.getBoolean()
        postType = parcel.readInt()
        attachments = parcel.readTypedObjectCompat(Attachments.CREATOR)
        signerId = parcel.readInt()
        creatorId = parcel.readInt()
        creator = parcel.readTypedObjectCompat(User.CREATOR)
        isCanPin = parcel.getBoolean()
        isPinned = parcel.getBoolean()
        copyHierarchy = parcel.createTypedArrayList(CREATOR)
        isDeleted = parcel.getBoolean()
        source = parcel.readTypedObjectCompat(PostSource.CREATOR)
        viewCount = parcel.readInt()
        isCanEdit = parcel.getBoolean()
        isFavorite = parcel.getBoolean()
        copyright = parcel.readTypedObjectCompat(Copyright.CREATOR)
        isDonut = parcel.getBoolean()
    }

    constructor()

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_POST
    }

    fun setSource(source: PostSource?): Post {
        this.source = source
        return this
    }

    fun setDbid(dbid: Int): Post {
        this.dbid = dbid
        return this
    }

    fun setVkid(vkid: Int): Post {
        this.vkid = vkid
        return this
    }

    fun setCopyright(copyright: Copyright?): Post {
        this.copyright = copyright
        return this
    }

    fun setIsDonut(isDonut: Boolean): Post {
        this.isDonut = isDonut
        return this
    }

    fun setFavorite(favorite: Boolean): Post {
        isFavorite = favorite
        return this
    }

    fun setOwnerId(ownerId: Int): Post {
        this.ownerId = ownerId
        return this
    }

    fun setAuthor(author: Owner?): Post {
        this.author = author
        return this
    }

    fun setAuthorId(authorId: Int): Post {
        this.authorId = authorId
        return this
    }

    fun setDate(date: Long): Post {
        this.date = date
        return this
    }

    fun setText(text: String?): Post {
        this.text = text
        return this
    }

    fun setReplyOwnerId(replyOwnerId: Int): Post {
        this.replyOwnerId = replyOwnerId
        return this
    }

    fun setReplyPostId(replyPostId: Int): Post {
        this.replyPostId = replyPostId
        return this
    }

    fun setFriendsOnly(friendsOnly: Boolean): Post {
        isFriendsOnly = friendsOnly
        return this
    }

    fun setCommentsCount(commentsCount: Int): Post {
        this.commentsCount = commentsCount
        return this
    }

    fun setCanPostComment(canPostComment: Boolean): Post {
        isCanPostComment = canPostComment
        return this
    }

    fun setLikesCount(likesCount: Int): Post {
        this.likesCount = likesCount
        return this
    }

    fun setCanLike(canLike: Boolean): Post {
        isCanLike = canLike
        return this
    }

    fun setRepostCount(repostCount: Int): Post {
        this.repostCount = repostCount
        return this
    }

    fun setUserReposted(userReposted: Boolean): Post {
        isUserReposted = userReposted
        return this
    }

    fun setPostType(postType: Int): Post {
        this.postType = postType
        return this
    }

    fun setAttachments(attachments: Attachments?): Post {
        this.attachments = attachments
        return this
    }

    fun setSignerId(signerId: Int): Post {
        this.signerId = signerId
        return this
    }

    fun setCreator(creator: User?): Post {
        this.creator = creator
        return this
    }

    fun setCanPin(canPin: Boolean): Post {
        isCanPin = canPin
        return this
    }

    fun setPinned(pinned: Boolean): Post {
        isPinned = pinned
        return this
    }

    fun getCopyHierarchy(): List<Post>? {
        return copyHierarchy
    }

    fun setCopyHierarchy(copyHierarchy: ArrayList<Post>?): Post {
        this.copyHierarchy = copyHierarchy
        return this
    }

    fun setCanRepost(canRepost: Boolean): Post {
        isCanRepost = canRepost
        return this
    }

    fun setDeleted(deleted: Boolean): Post {
        isDeleted = deleted
        return this
    }

    fun setUserLikes(userLikes: Boolean): Post {
        isUserLikes = userLikes
        return this
    }

    fun setViewCount(viewCount: Int): Post {
        this.viewCount = viewCount
        return this
    }

    fun prepareCopyHierarchy(initialSize: Int): ArrayList<Post> {
        if (copyHierarchy == null) {
            copyHierarchy = ArrayList(initialSize)
        }
        return copyHierarchy!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "POST[dbid: $dbid, vkid: $vkid, ownerid: $ownerId]"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(dbid)
        parcel.writeInt(vkid)
        parcel.writeInt(ownerId)
        parcel.writeInt(authorId)
        parcel.writeTypedObjectCompat(author, flags)
        parcel.writeLong(date)
        parcel.writeString(text)
        parcel.writeInt(replyOwnerId)
        parcel.writeInt(replyPostId)
        parcel.putBoolean(isFriendsOnly)
        parcel.writeInt(commentsCount)
        parcel.putBoolean(isCanPostComment)
        parcel.writeInt(likesCount)
        parcel.putBoolean(isCanLike)
        parcel.writeInt(repostCount)
        parcel.putBoolean(isCanRepost)
        parcel.putBoolean(isUserReposted)
        parcel.writeInt(postType)
        parcel.writeTypedObjectCompat(attachments, flags)
        parcel.writeInt(signerId)
        parcel.writeInt(creatorId)
        parcel.writeTypedObjectCompat(creator, flags)
        parcel.putBoolean(isCanPin)
        parcel.putBoolean(isPinned)
        parcel.writeTypedList(copyHierarchy)
        parcel.putBoolean(isDeleted)
        parcel.writeTypedObjectCompat(source, flags)
        parcel.writeInt(viewCount)
        parcel.putBoolean(isCanEdit)
        parcel.putBoolean(isFavorite)
        parcel.writeTypedObjectCompat(copyright, flags)
        parcel.putBoolean(isDonut)
    }

    /**
     * Получить аватар автора поста
     *
     * @return ссылка на квадратное изображение в разрешении до 200px
     */
    val authorPhoto: String?
        get() = author?.maxSquareAvatar
    val authorName: String?
        get() = author?.fullName
    val isPostponed: Boolean
        get() = postType == VKApiPost.Type.POSTPONE

    fun hasAttachments(): Boolean {
        return attachments?.hasAttachments == true
    }

    fun hasText(): Boolean {
        return text.nonNullNoEmpty()
    }

    fun generateVkPostLink(): String {
        return String.format("vk.com/wall%s_%s", ownerId, vkid)
    }

    fun hasPhotos(): Boolean {
        return attachments?.photos.nonNullNoEmpty()
    }

    fun hasVideos(): Boolean {
        return attachments?.videos.nonNullNoEmpty()
    }

    fun hasCopyHierarchy(): Boolean {
        return copyHierarchy.nonNullNoEmpty()
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Post {
        val clone = super.clone() as Post
        clone.attachments = attachments?.clone()
        clone.copyHierarchy = copyHierarchy?.size?.let { ArrayList(it) }
        copyHierarchy?.let { clone.copyHierarchy?.addAll(it) }
        return clone
    }

    val textCopiesInclude: String?
        get() {
            when {
                text.nonNullNoEmpty() -> {
                    return text
                }
                hasCopyHierarchy() -> {
                    for (copy in copyHierarchy.orEmpty()) {
                        if (copy.text.nonNullNoEmpty()) {
                            return copy.text
                        }
                    }
                }
            }
            return null
        }

    @JvmOverloads
    fun findFirstImageCopiesInclude(
        @PhotoSize prefferedSize: Int = PhotoSize.Q,
        excludeNonAspectRatio: Boolean = false
    ): String? {
        if (hasPhotos()) {
            return attachments?.photos?.get(0)?.getUrlForSize(prefferedSize, excludeNonAspectRatio)
        }
        if (hasVideos()) {
            return attachments?.videos?.get(0)?.image
        }
        if (hasDocs()) {
            return attachments?.docs?.get(0)
                ?.getPreviewWithSize(prefferedSize, excludeNonAspectRatio)
        }
        if (hasCopyHierarchy()) {
            for (copy in copyHierarchy.orEmpty()) {
                val url = copy.findFirstImageCopiesInclude(prefferedSize, excludeNonAspectRatio)
                if (url.nonNullNoEmpty()) {
                    return url
                }
            }
        }
        return null
    }

    private fun hasDocs(): Boolean {
        return attachments?.docs.nonNullNoEmpty()
    }

    fun setCreatorId(creatorId: Int): Post {
        this.creatorId = creatorId
        return this
    }

    fun setCanEdit(canEdit: Boolean): Post {
        isCanEdit = canEdit
        return this
    }

    class Copyright(val name: String, val link: String?) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString().orEmpty(),
            parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(link)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Copyright> {
            override fun createFromParcel(parcel: Parcel): Copyright {
                return Copyright(parcel)
            }

            override fun newArray(size: Int): Array<Copyright?> {
                return arrayOfNulls(size)
            }
        }

    }

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}