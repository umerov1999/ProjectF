package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class UserDetails : Parcelable {
    var photoId: IdPair? = null
        private set
    var statusAudio: Audio? = null
        private set
    var isFavorite = false
        private set
    var isSubscribed = false
        private set
    var friendsCount = 0
        private set
    var onlineFriendsCount = 0
        private set
    var mutualFriendsCount = 0
        private set
    var followersCount = 0
        private set
    var groupsCount = 0
        private set
    var photosCount = 0
        private set
    var audiosCount = 0
        private set
    var articlesCount = 0
        private set
    var productsCount = 0
        private set
    var videosCount = 0
        private set
    var allWallCount = 0
        private set
    var ownWallCount = 0
        private set
    var postponedWallCount = 0
        private set
    var giftCount = 0
        private set
    var productServicesCount = 0
        private set
    var narrativesCount = 0
        private set
    var clipsCount = 0
        private set
    var city: City? = null
        private set
    var country: Country? = null
        private set
    var hometown: String? = null
        private set
    var phone: String? = null
        private set
    var homePhone: String? = null
        private set
    var skype: String? = null
        private set
    var instagram: String? = null
        private set
    var twitter: String? = null
        private set
    var facebook: String? = null
        private set
    var careers: List<Career>? = null
        private set
    var militaries: List<Military>? = null
        private set
    var universities: List<University>? = null
        private set
    var schools: List<School>? = null
        private set
    var relatives: List<Relative>? = null
        private set
    var relation = 0
        private set
    var relationPartner: Owner? = null
        private set
    var languages: Array<String>? = null
        private set
    var political = 0
        private set
    var peopleMain = 0
        private set
    var lifeMain = 0
        private set
    var smoking = 0
        private set
    var alcohol = 0
        private set
    var inspiredBy: String? = null
        private set
    var religion: String? = null
        private set
    var site: String? = null
        private set
    var interests: String? = null
        private set
    var music: String? = null
        private set
    var activities: String? = null
        private set
    var movies: String? = null
        private set
    var tv: String? = null
        private set
    var games: String? = null
        private set
    var quotes: String? = null
        private set
    var about: String? = null
        private set
    var books: String? = null
        private set
    var isClosed: Boolean = false
        private set
    var cover: Cover? = null
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        photoId = parcel.readTypedObjectCompat(IdPair.CREATOR)
        statusAudio = parcel.readTypedObjectCompat(Audio.CREATOR)
        isFavorite = parcel.getBoolean()
        isSubscribed = parcel.getBoolean()
        friendsCount = parcel.readInt()
        onlineFriendsCount = parcel.readInt()
        mutualFriendsCount = parcel.readInt()
        followersCount = parcel.readInt()
        groupsCount = parcel.readInt()
        photosCount = parcel.readInt()
        audiosCount = parcel.readInt()
        articlesCount = parcel.readInt()
        productsCount = parcel.readInt()
        videosCount = parcel.readInt()
        allWallCount = parcel.readInt()
        ownWallCount = parcel.readInt()
        postponedWallCount = parcel.readInt()
        giftCount = parcel.readInt()
        productServicesCount = parcel.readInt()
        narrativesCount = parcel.readInt()
        clipsCount = parcel.readInt()
        city = parcel.readTypedObjectCompat(City.CREATOR)
        country = parcel.readTypedObjectCompat(Country.CREATOR)
        hometown = parcel.readString()
        phone = parcel.readString()
        homePhone = parcel.readString()
        skype = parcel.readString()
        instagram = parcel.readString()
        twitter = parcel.readString()
        facebook = parcel.readString()
        relatives = parcel.createTypedArrayList(Relative)
        relation = parcel.readInt()
        relationPartner = ParcelableOwnerWrapper.readOwner(parcel)
        languages = parcel.createStringArray()
        political = parcel.readInt()
        peopleMain = parcel.readInt()
        lifeMain = parcel.readInt()
        smoking = parcel.readInt()
        alcohol = parcel.readInt()
        inspiredBy = parcel.readString()
        religion = parcel.readString()
        site = parcel.readString()
        interests = parcel.readString()
        music = parcel.readString()
        activities = parcel.readString()
        movies = parcel.readString()
        tv = parcel.readString()
        games = parcel.readString()
        quotes = parcel.readString()
        about = parcel.readString()
        books = parcel.readString()
        isClosed = parcel.getBoolean()
        cover = parcel.readTypedObjectCompat(Cover.CREATOR)
    }

    fun setProductServicesCount(productServicesCount: Int): UserDetails {
        this.productServicesCount = productServicesCount
        return this
    }

    fun setNarrativesCount(narrativesCount: Int): UserDetails {
        this.narrativesCount = narrativesCount
        return this
    }

    fun setClipsCount(clipsCount: Int): UserDetails {
        this.clipsCount = clipsCount
        return this
    }

    fun setInterests(interests: String?): UserDetails {
        this.interests = interests
        return this
    }

    fun setMusic(music: String?): UserDetails {
        this.music = music
        return this
    }

    fun setFavorite(isFavorite: Boolean): UserDetails {
        this.isFavorite = isFavorite
        return this
    }

    fun setSubscribed(isSubscribed: Boolean): UserDetails {
        this.isSubscribed = isSubscribed
        return this
    }

    fun setActivities(activities: String?): UserDetails {
        this.activities = activities
        return this
    }

    fun setMovies(movies: String?): UserDetails {
        this.movies = movies
        return this
    }

    fun setTv(tv: String?): UserDetails {
        this.tv = tv
        return this
    }

    fun setGames(games: String?): UserDetails {
        this.games = games
        return this
    }

    fun setQuotes(quotes: String?): UserDetails {
        this.quotes = quotes
        return this
    }

    fun setCover(cover: Cover?): UserDetails {
        this.cover = cover
        return this
    }

    fun setAbout(about: String?): UserDetails {
        this.about = about
        return this
    }

    fun setBooks(books: String?): UserDetails {
        this.books = books
        return this
    }

    fun setSite(site: String?): UserDetails {
        this.site = site
        return this
    }

    fun setAlcohol(alcohol: Int): UserDetails {
        this.alcohol = alcohol
        return this
    }

    fun setLifeMain(lifeMain: Int): UserDetails {
        this.lifeMain = lifeMain
        return this
    }

    fun setPeopleMain(peopleMain: Int): UserDetails {
        this.peopleMain = peopleMain
        return this
    }

    fun setPolitical(political: Int): UserDetails {
        this.political = political
        return this
    }

    fun setSmoking(smoking: Int): UserDetails {
        this.smoking = smoking
        return this
    }

    fun setInspiredBy(inspiredBy: String?): UserDetails {
        this.inspiredBy = inspiredBy
        return this
    }

    fun setReligion(religion: String?): UserDetails {
        this.religion = religion
        return this
    }

    fun setLanguages(languages: Array<String>?): UserDetails {
        this.languages = languages
        return this
    }

    fun setRelation(relation: Int): UserDetails {
        this.relation = relation
        return this
    }

    fun setRelationPartner(relationPartner: Owner?): UserDetails {
        this.relationPartner = relationPartner
        return this
    }

    fun setRelatives(relatives: List<Relative>?): UserDetails {
        this.relatives = relatives
        return this
    }

    fun setSchools(schools: List<School>?): UserDetails {
        this.schools = schools
        return this
    }

    fun setUniversities(universities: List<University>?): UserDetails {
        this.universities = universities
        return this
    }

    fun setMilitaries(militaries: List<Military>?): UserDetails {
        this.militaries = militaries
        return this
    }

    fun setCareers(careers: List<Career>?): UserDetails {
        this.careers = careers
        return this
    }

    fun setSkype(skype: String?): UserDetails {
        this.skype = skype
        return this
    }

    fun setInstagram(instagram: String?): UserDetails {
        this.instagram = instagram
        return this
    }

    fun setTwitter(twitter: String?): UserDetails {
        this.twitter = twitter
        return this
    }

    fun setFacebook(facebook: String?): UserDetails {
        this.facebook = facebook
        return this
    }

    fun setHomePhone(homePhone: String?): UserDetails {
        this.homePhone = homePhone
        return this
    }

    fun setPhone(phone: String?): UserDetails {
        this.phone = phone
        return this
    }

    fun setHometown(hometown: String?): UserDetails {
        this.hometown = hometown
        return this
    }

    fun setCountry(country: Country?): UserDetails {
        this.country = country
        return this
    }

    fun setCity(city: City?): UserDetails {
        this.city = city
        return this
    }

    fun setClosed(closed: Boolean): UserDetails {
        this.isClosed = closed
        return this
    }

    fun setPhotoId(photoId: IdPair?): UserDetails {
        this.photoId = photoId
        return this
    }

    fun setStatusAudio(statusAudio: Audio?): UserDetails {
        this.statusAudio = statusAudio
        return this
    }

    fun setFriendsCount(friendsCount: Int): UserDetails {
        this.friendsCount = friendsCount
        return this
    }

    fun setOnlineFriendsCount(onlineFriendsCount: Int): UserDetails {
        this.onlineFriendsCount = onlineFriendsCount
        return this
    }

    fun setMutualFriendsCount(mutualFriendsCount: Int): UserDetails {
        this.mutualFriendsCount = mutualFriendsCount
        return this
    }

    fun setFollowersCount(followersCount: Int): UserDetails {
        this.followersCount = followersCount
        return this
    }

    fun setGroupsCount(groupsCount: Int): UserDetails {
        this.groupsCount = groupsCount
        return this
    }

    fun setPhotosCount(photosCount: Int): UserDetails {
        this.photosCount = photosCount
        return this
    }

    fun setAudiosCount(audiosCount: Int): UserDetails {
        this.audiosCount = audiosCount
        return this
    }

    fun setArticlesCount(articlesCount: Int): UserDetails {
        this.articlesCount = articlesCount
        return this
    }

    fun setProductsCount(productsCount: Int): UserDetails {
        this.productsCount = productsCount
        return this
    }

    fun setVideosCount(videosCount: Int): UserDetails {
        this.videosCount = videosCount
        return this
    }

    fun setAllWallCount(allWallCount: Int): UserDetails {
        this.allWallCount = allWallCount
        return this
    }

    fun setOwnWallCount(ownWallCount: Int): UserDetails {
        this.ownWallCount = ownWallCount
        return this
    }

    fun setPostponedWallCount(postponedWallCount: Int): UserDetails {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun setGiftCount(giftCount: Int): UserDetails {
        this.giftCount = giftCount
        return this
    }

    class Relative() : Parcelable {
        var user: User? = null
            private set
        var type: String? = null
            private set
        var name: String? = null
            private set

        constructor(parcel: Parcel) : this() {
            user = parcel.readTypedObjectCompat(User.CREATOR)
            type = parcel.readString()
            name = parcel.readString()
        }

        fun setName(name: String?): Relative {
            this.name = name
            return this
        }

        fun setUser(user: User?): Relative {
            this.user = user
            return this
        }

        fun setType(type: String?): Relative {
            this.type = type
            return this
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeTypedObjectCompat(user, flags)
            parcel.writeString(type)
            parcel.writeString(name)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Relative> {
            override fun createFromParcel(parcel: Parcel): Relative {
                return Relative(parcel)
            }

            override fun newArray(size: Int): Array<Relative?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Cover() : Parcelable {
        var enabled = false
            private set
        var images: ArrayList<CoverImage>? = null
            private set

        constructor(parcel: Parcel) : this() {
            enabled = parcel.getBoolean()
            images = parcel.createTypedArrayList(CoverImage.CREATOR)
        }

        fun setImages(images: ArrayList<CoverImage>?): Cover {
            this.images = images
            return this
        }

        fun setEnabled(enabled: Boolean): Cover {
            this.enabled = enabled
            return this
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.putBoolean(enabled)
            parcel.writeTypedList(images)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Cover> {
            override fun createFromParcel(parcel: Parcel): Cover {
                return Cover(parcel)
            }

            override fun newArray(size: Int): Array<Cover?> {
                return arrayOfNulls(size)
            }
        }
    }

    class CoverImage(val url: String?, val height: Int, val width: Int) :
        Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(url)
            parcel.writeInt(height)
            parcel.writeInt(width)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<CoverImage> {
            override fun createFromParcel(parcel: Parcel): CoverImage {
                return CoverImage(parcel)
            }

            override fun newArray(size: Int): Array<CoverImage?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedObjectCompat(photoId, flags)
        parcel.writeTypedObjectCompat(statusAudio, flags)
        parcel.putBoolean(isFavorite)
        parcel.putBoolean(isSubscribed)
        parcel.writeInt(friendsCount)
        parcel.writeInt(onlineFriendsCount)
        parcel.writeInt(mutualFriendsCount)
        parcel.writeInt(followersCount)
        parcel.writeInt(groupsCount)
        parcel.writeInt(photosCount)
        parcel.writeInt(audiosCount)
        parcel.writeInt(articlesCount)
        parcel.writeInt(productsCount)
        parcel.writeInt(videosCount)
        parcel.writeInt(allWallCount)
        parcel.writeInt(ownWallCount)
        parcel.writeInt(postponedWallCount)
        parcel.writeInt(giftCount)
        parcel.writeInt(productServicesCount)
        parcel.writeInt(narrativesCount)
        parcel.writeInt(clipsCount)
        parcel.writeTypedObjectCompat(city, flags)
        parcel.writeTypedObjectCompat(country, flags)
        parcel.writeString(hometown)
        parcel.writeString(phone)
        parcel.writeString(homePhone)
        parcel.writeString(skype)
        parcel.writeString(instagram)
        parcel.writeString(twitter)
        parcel.writeString(facebook)
        parcel.writeTypedList(relatives)
        parcel.writeInt(relation)
        ParcelableOwnerWrapper.writeOwner(parcel, flags, relationPartner)
        parcel.writeStringArray(languages)
        parcel.writeInt(political)
        parcel.writeInt(peopleMain)
        parcel.writeInt(lifeMain)
        parcel.writeInt(smoking)
        parcel.writeInt(alcohol)
        parcel.writeString(inspiredBy)
        parcel.writeString(religion)
        parcel.writeString(site)
        parcel.writeString(interests)
        parcel.writeString(music)
        parcel.writeString(activities)
        parcel.writeString(movies)
        parcel.writeString(tv)
        parcel.writeString(games)
        parcel.writeString(quotes)
        parcel.writeString(about)
        parcel.writeString(books)
        parcel.putBoolean(isClosed)
        parcel.writeTypedObjectCompat(cover, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserDetails> {
        override fun createFromParcel(parcel: Parcel): UserDetails {
            return UserDetails(parcel)
        }

        override fun newArray(size: Int): Array<UserDetails?> {
            return arrayOfNulls(size)
        }
    }
}