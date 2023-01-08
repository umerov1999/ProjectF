package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class News : AbsModel {
    /**
     * Каждый из элементов массива в поле friends содержит поля: uid— идентификатор пользователя
     */
    var friends: List<User>? = null
        private set

    /**
     * тип списка новости, соответствующий одному из значений параметра filters;
     */
    var type: String? = null
        private set

    /**
     * идентификатор источника новости (положительный — новость пользователя, отрицательный — новость группы);
     */
    var sourceId = 0
        private set
    var source: Owner? = null
        private set

    /**
     * находится в записях со стен, содержит тип новости (post или copy);
     */
    var postType: String? = null
        private set

    /**
     * передается в случае, если этот пост сделан при удалении;
     */
    var isFinalPost = false
        private set

    /**
     * находится в записях со стен, если сообщение является копией сообщения с чужой стены,
     * и содержит идентификатор владельца стены, у которого было скопировано сообщение;
     */
    var copyOwnerId = 0
        private set

    /**
     * находится в записях со стен, если сообщение является копией сообщения с чужой стены,
     * и содержит идентификатор скопированного сообщения на стене его владельца;
     */
    var copyPostId = 0
        private set

    /**
     * находится в записях со стен, если сообщение является копией сообщения с чужой стены,
     * и содержит дату скопированного сообщения;
     */
    var copyPostDate: Long = 0
        private set

    /**
     * время публикации новости в формате unixtime;
     */
    var date: Long = 0
        private set

    /**
     * находится в записях со стен и содержит идентификатор записи на стене владельца;
     */
    var postId = 0
        private set

    /**
     * массив, содержащий историю репостов для записи. Возвращается только в том случае, если запись
     * является репостом. Каждый из объектов массива, в свою очередь, является объектом-записью стандартного формата.
     */
    private var copyHistory: ArrayList<Post>? = null

    /**
     * находится в записях со стен и содержит текст записи;
     */
    var text: String? = null
        private set

    /**
     * содержит true, если текущий пользователь может редактировать запись;
     */
    var isCanEdit = false
        private set

    /**
     * возвращается, если пользователь может удалить новость, всегда содержит true;
     */
    var isCanDelete = false
        private set

    /**
     * количество комментариев
     */
    var commentCount = 0
        private set

    /**
     * информация о том, может ли текущий пользователь комментировать запись
     * (true — может, false — не может);
     */
    var isCommentCanPost = false
        private set

    /**
     * число пользователей, которым понравилась запись,
     */
    var likeCount = 0
        private set

    /**
     * наличие отметки «Мне нравится» от текущего пользователя
     */
    var isUserLike = false
        private set

    /**
     * информация о том, может ли текущий пользователь поставить отметку «Мне нравится»
     */
    var isCanLike = false
        private set

    /**
     * информация о том, может ли текущий пользователь сделать репост записи
     */
    var isCanPublish = false
        private set

    /**
     * число пользователей, сделавших репост
     */
    var repostsCount = 0
        private set
    //private int attachmentsMask;
    /**
     * наличие репоста от текущего пользователя
     */
    var isUserReposted = false
        private set

    /**
     * находится в записях со стен и содержит массив объектов,
     * которые прикреплены к текущей новости (фотография, ссылка и т.п.
     */
    var attachments: Attachments? = null
        private set

    var copyright: Copyright? = null
        private set

    @Transient
    var tag: Any? = null
        private set
    var viewCount = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        type = parcel.readString()
        sourceId = parcel.readInt()
        source = Owner.readOwnerFromParcel(sourceId, parcel)
        postType = parcel.readString()
        isFinalPost = parcel.getBoolean()
        copyOwnerId = parcel.readInt()
        copyPostId = parcel.readInt()
        copyPostDate = parcel.readLong()
        date = parcel.readLong()
        postId = parcel.readInt()
        copyHistory = parcel.createTypedArrayList(Post.CREATOR)
        text = parcel.readString()
        isCanEdit = parcel.getBoolean()
        isCanDelete = parcel.getBoolean()
        commentCount = parcel.readInt()
        isCommentCanPost = parcel.getBoolean()
        likeCount = parcel.readInt()
        isUserLike = parcel.getBoolean()
        isCanLike = parcel.getBoolean()
        isCanPublish = parcel.getBoolean()
        repostsCount = parcel.readInt()
        isUserReposted = parcel.getBoolean()
        attachments = parcel.readTypedObjectCompat(Attachments.CREATOR)
        friends = parcel.createTypedArrayList(User.CREATOR)
        viewCount = parcel.readInt()
        copyright = parcel.readTypedObjectCompat(Copyright.CREATOR)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_NEWS
    }

    fun setTag(tag: Any?): News {
        this.tag = tag
        return this
    }

    fun setCopyright(copyright: Copyright?): News {
        this.copyright = copyright
        return this
    }

    fun setType(type: String?): News {
        this.type = type
        return this
    }

    fun setSourceId(sourceId: Int): News {
        this.sourceId = sourceId
        return this
    }

    fun setSource(source: Owner?): News {
        this.source = source
        return this
    }

    fun setPostType(postType: String?): News {
        this.postType = postType
        return this
    }

    fun setFinalPost(finalPost: Boolean): News {
        isFinalPost = finalPost
        return this
    }

    fun setCopyOwnerId(copyOwnerId: Int): News {
        this.copyOwnerId = copyOwnerId
        return this
    }

    fun setCopyPostId(copyPostId: Int): News {
        this.copyPostId = copyPostId
        return this
    }

    fun setCopyPostDate(copyPostDate: Long): News {
        this.copyPostDate = copyPostDate
        return this
    }

    fun setDate(date: Long): News {
        this.date = date
        return this
    }

    fun getCopyHistory(): List<Post>? {
        return copyHistory
    }

    fun setCopyHistory(copyHistory: ArrayList<Post>?): News {
        this.copyHistory = copyHistory
        return this
    }

    fun setText(text: String?): News {
        this.text = text
        return this
    }

    fun setCanEdit(canEdit: Boolean): News {
        isCanEdit = canEdit
        return this
    }

    fun setCanDelete(canDelete: Boolean): News {
        isCanDelete = canDelete
        return this
    }

    fun setCommentCount(commentCount: Int): News {
        this.commentCount = commentCount
        return this
    }

    fun setCommentCanPost(commentCanPost: Boolean): News {
        isCommentCanPost = commentCanPost
        return this
    }

    fun setLikeCount(likeCount: Int): News {
        this.likeCount = likeCount
        return this
    }

    fun setUserLike(userLike: Boolean): News {
        isUserLike = userLike
        return this
    }

    fun setCanLike(canLike: Boolean): News {
        isCanLike = canLike
        return this
    }

    fun setCanPublish(canPublish: Boolean): News {
        isCanPublish = canPublish
        return this
    }

    fun setRepostsCount(repostsCount: Int): News {
        this.repostsCount = repostsCount
        return this
    }

    fun setUserReposted(userReposted: Boolean): News {
        isUserReposted = userReposted
        return this
    }

    fun setAttachments(attachments: Attachments?): News {
        this.attachments = attachments
        return this
    }

    fun setPostId(postId: Int): News {
        this.postId = postId
        return this
    }

    fun setFriends(friends: List<User>?): News {
        this.friends = friends
        return this
    }

    val ownerMaxSquareAvatar: String?
        get() = source?.maxSquareAvatar
    val ownerName: String?
        get() = source?.fullName

    fun toPost(): Post? {
        return if ("post" != type) {
            null
        } else Post()
            .setVkid(postId)
            .setOwnerId(sourceId)
            .setText(text)
            .setAttachments(attachments)
            .setCopyHierarchy(copyHistory)
            .setDate(date)
            .setAuthor(source)
            .setCanLike(isCanLike)
            .setLikesCount(likeCount)
            .setUserLikes(isUserLike)
            .setReplyOwnerId(copyOwnerId)
            .setReplyPostId(copyPostId)
            .setRepostCount(repostsCount)
            .setUserReposted(isUserReposted)
            .setCommentsCount(commentCount)
            .setCanPostComment(isCommentCanPost)
            .setCanRepost(isCanPublish)
            .setViewCount(viewCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeInt(sourceId)
        parcel.writeTypedObjectCompat(source, flags)
        parcel.writeString(postType)
        parcel.putBoolean(isFinalPost)
        parcel.writeInt(copyOwnerId)
        parcel.writeInt(copyPostId)
        parcel.writeLong(copyPostDate)
        parcel.writeLong(date)
        parcel.writeInt(postId)
        parcel.writeTypedList(copyHistory)
        parcel.writeString(text)
        parcel.putBoolean(isCanEdit)
        parcel.putBoolean(isCanDelete)
        parcel.writeInt(commentCount)
        parcel.putBoolean(isCommentCanPost)
        parcel.writeInt(likeCount)
        parcel.putBoolean(isUserLike)
        parcel.putBoolean(isCanLike)
        parcel.putBoolean(isCanPublish)
        parcel.writeInt(repostsCount)
        parcel.putBoolean(isUserReposted)
        parcel.writeTypedObjectCompat(attachments, flags)
        parcel.writeTypedList(friends)
        parcel.writeInt(viewCount)
        parcel.writeTypedObjectCompat(copyright, flags)
    }

    fun hasAttachments(): Boolean {
        return attachments?.hasAttachments == true
    }

    fun setViewCount(viewCount: Int): News {
        this.viewCount = viewCount
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

    companion object CREATOR : Parcelable.Creator<News> {
        override fun createFromParcel(parcel: Parcel): News {
            return News(parcel)
        }

        override fun newArray(size: Int): Array<News?> {
            return arrayOfNulls(size)
        }
    }
}