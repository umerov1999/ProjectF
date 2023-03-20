package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class UserDetails : Parcelable {
    private var photoId: IdPair? = null
    private var statusAudio: Audio? = null
    private var isFavorite = false
    private var isSubscribed = false
    private var friendsCount = 0
    private var onlineFriendsCount = 0
    private var mutualFriendsCount = 0
    private var followersCount = 0
    private var groupsCount = 0
    private var photosCount = 0
    private var audiosCount = 0
    private var articlesCount = 0
    private var productsCount = 0
    private var videosCount = 0
    private var allWallCount = 0
    private var ownWallCount = 0
    private var postponedWallCount = 0
    private var giftCount = 0
    private var productServicesCount = 0
    private var narrativesCount = 0
    private var clipsCount = 0
    private var city: City? = null
    private var country: Country? = null
    private var hometown: String? = null
    private var phone: String? = null
    private var homePhone: String? = null
    private var skype: String? = null
    private var instagram: String? = null
    private var twitter: String? = null
    private var facebook: String? = null
    private var careers: List<Career>? = null
    private var militaries: List<Military>? = null
    private var universities: List<University>? = null
    private var schools: List<School>? = null
    private var relatives: List<Relative>? = null
    private var relation = 0
    private var relationPartner: Owner? = null
    private var languages: Array<String>? = null
    private var political = 0
    private var peopleMain = 0
    private var lifeMain = 0
    private var smoking = 0
    private var alcohol = 0
    private var inspiredBy: String? = null
    private var religion: String? = null
    private var site: String? = null
    private var interests: String? = null
    private var music: String? = null
    private var activities: String? = null
    private var movies: String? = null
    private var tv: String? = null
    private var games: String? = null
    private var quotes: String? = null
    private var about: String? = null
    private var books: String? = null
    private var isClosed: Boolean = false
    private var cover: Cover? = null

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
        relationPartner = parcel.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)!!.get()
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

    fun getProductServicesCount(): Int {
        return productServicesCount
    }

    fun setNarrativesCount(narrativesCount: Int): UserDetails {
        this.narrativesCount = narrativesCount
        return this
    }

    fun getNarrativesCount(): Int {
        return narrativesCount
    }

    fun setClipsCount(clipsCount: Int): UserDetails {
        this.clipsCount = clipsCount
        return this
    }

    fun getClipsCount(): Int {
        return clipsCount
    }

    fun getInterests(): String? {
        return interests
    }

    fun setInterests(interests: String?): UserDetails {
        this.interests = interests
        return this
    }

    fun getMusic(): String? {
        return music
    }

    fun setMusic(music: String?): UserDetails {
        this.music = music
        return this
    }

    fun isSetFavorite(): Boolean {
        return isFavorite
    }

    fun setFavorite(isFavorite: Boolean): UserDetails {
        this.isFavorite = isFavorite
        return this
    }

    fun isSetSubscribed(): Boolean {
        return isSubscribed
    }

    fun setSubscribed(isSubscribed: Boolean): UserDetails {
        this.isSubscribed = isSubscribed
        return this
    }

    fun getActivities(): String? {
        return activities
    }

    fun setActivities(activities: String?): UserDetails {
        this.activities = activities
        return this
    }

    fun getMovies(): String? {
        return movies
    }

    fun setMovies(movies: String?): UserDetails {
        this.movies = movies
        return this
    }

    fun getTv(): String? {
        return tv
    }

    fun setTv(tv: String?): UserDetails {
        this.tv = tv
        return this
    }

    fun getGames(): String? {
        return games
    }

    fun setGames(games: String?): UserDetails {
        this.games = games
        return this
    }

    fun getQuotes(): String? {
        return quotes
    }

    fun setQuotes(quotes: String?): UserDetails {
        this.quotes = quotes
        return this
    }

    fun getCover(): Cover? {
        return cover
    }

    fun setCover(cover: Cover?): UserDetails {
        this.cover = cover
        return this
    }

    fun getAbout(): String? {
        return about
    }

    fun setAbout(about: String?): UserDetails {
        this.about = about
        return this
    }

    fun getBooks(): String? {
        return books
    }

    fun setBooks(books: String?): UserDetails {
        this.books = books
        return this
    }

    fun getSite(): String? {
        return site
    }

    fun setSite(site: String?): UserDetails {
        this.site = site
        return this
    }

    fun getAlcohol(): Int {
        return alcohol
    }

    fun setAlcohol(alcohol: Int): UserDetails {
        this.alcohol = alcohol
        return this
    }

    fun getLifeMain(): Int {
        return lifeMain
    }

    fun setLifeMain(lifeMain: Int): UserDetails {
        this.lifeMain = lifeMain
        return this
    }

    fun getPeopleMain(): Int {
        return peopleMain
    }

    fun setPeopleMain(peopleMain: Int): UserDetails {
        this.peopleMain = peopleMain
        return this
    }

    fun getPolitical(): Int {
        return political
    }

    fun setPolitical(political: Int): UserDetails {
        this.political = political
        return this
    }

    fun getSmoking(): Int {
        return smoking
    }

    fun setSmoking(smoking: Int): UserDetails {
        this.smoking = smoking
        return this
    }

    fun getInspiredBy(): String? {
        return inspiredBy
    }

    fun setInspiredBy(inspiredBy: String?): UserDetails {
        this.inspiredBy = inspiredBy
        return this
    }

    fun getReligion(): String? {
        return religion
    }

    fun setReligion(religion: String?): UserDetails {
        this.religion = religion
        return this
    }

    fun getLanguages(): Array<String>? {
        return languages
    }

    fun setLanguages(languages: Array<String>?): UserDetails {
        this.languages = languages
        return this
    }

    fun getRelation(): Int {
        return relation
    }

    fun setRelation(relation: Int): UserDetails {
        this.relation = relation
        return this
    }

    fun getRelationPartner(): Owner? {
        return relationPartner
    }

    fun setRelationPartner(relationPartner: Owner?): UserDetails {
        this.relationPartner = relationPartner
        return this
    }

    fun getRelatives(): List<Relative>? {
        return relatives
    }

    fun setRelatives(relatives: List<Relative>?): UserDetails {
        this.relatives = relatives
        return this
    }

    fun getSchools(): List<School>? {
        return schools
    }

    fun setSchools(schools: List<School>?): UserDetails {
        this.schools = schools
        return this
    }

    fun getUniversities(): List<University>? {
        return universities
    }

    fun setUniversities(universities: List<University>?): UserDetails {
        this.universities = universities
        return this
    }

    fun getMilitaries(): List<Military>? {
        return militaries
    }

    fun setMilitaries(militaries: List<Military>?): UserDetails {
        this.militaries = militaries
        return this
    }

    fun getCareers(): List<Career>? {
        return careers
    }

    fun setCareers(careers: List<Career>?): UserDetails {
        this.careers = careers
        return this
    }

    fun getSkype(): String? {
        return skype
    }

    fun setSkype(skype: String?): UserDetails {
        this.skype = skype
        return this
    }

    fun getInstagram(): String? {
        return instagram
    }

    fun setInstagram(instagram: String?): UserDetails {
        this.instagram = instagram
        return this
    }

    fun getTwitter(): String? {
        return twitter
    }

    fun setTwitter(twitter: String?): UserDetails {
        this.twitter = twitter
        return this
    }

    fun getFacebook(): String? {
        return facebook
    }

    fun setFacebook(facebook: String?): UserDetails {
        this.facebook = facebook
        return this
    }

    fun getHomePhone(): String? {
        return homePhone
    }

    fun setHomePhone(homePhone: String?): UserDetails {
        this.homePhone = homePhone
        return this
    }

    fun getPhone(): String? {
        return phone
    }

    fun setPhone(phone: String?): UserDetails {
        this.phone = phone
        return this
    }

    fun getHometown(): String? {
        return hometown
    }

    fun setHometown(hometown: String?): UserDetails {
        this.hometown = hometown
        return this
    }

    fun getCountry(): Country? {
        return country
    }

    fun setCountry(country: Country?): UserDetails {
        this.country = country
        return this
    }

    fun getCity(): City? {
        return city
    }

    fun setCity(city: City?): UserDetails {
        this.city = city
        return this
    }

    fun isClosed(): Boolean {
        return isClosed
    }

    fun setClosed(closed: Boolean): UserDetails {
        this.isClosed = closed
        return this
    }

    fun getPhotoId(): IdPair? {
        return photoId
    }

    fun setPhotoId(photoId: IdPair?): UserDetails {
        this.photoId = photoId
        return this
    }

    fun getStatusAudio(): Audio? {
        return statusAudio
    }

    fun setStatusAudio(statusAudio: Audio?): UserDetails {
        this.statusAudio = statusAudio
        return this
    }

    fun getFriendsCount(): Int {
        return friendsCount
    }

    fun setFriendsCount(friendsCount: Int): UserDetails {
        this.friendsCount = friendsCount
        return this
    }

    fun getOnlineFriendsCount(): Int {
        return onlineFriendsCount
    }

    fun setOnlineFriendsCount(onlineFriendsCount: Int): UserDetails {
        this.onlineFriendsCount = onlineFriendsCount
        return this
    }

    fun getMutualFriendsCount(): Int {
        return mutualFriendsCount
    }

    fun setMutualFriendsCount(mutualFriendsCount: Int): UserDetails {
        this.mutualFriendsCount = mutualFriendsCount
        return this
    }

    fun getFollowersCount(): Int {
        return followersCount
    }

    fun setFollowersCount(followersCount: Int): UserDetails {
        this.followersCount = followersCount
        return this
    }

    fun getGroupsCount(): Int {
        return groupsCount
    }

    fun setGroupsCount(groupsCount: Int): UserDetails {
        this.groupsCount = groupsCount
        return this
    }

    fun getPhotosCount(): Int {
        return photosCount
    }

    fun setPhotosCount(photosCount: Int): UserDetails {
        this.photosCount = photosCount
        return this
    }

    fun getAudiosCount(): Int {
        return audiosCount
    }

    fun setAudiosCount(audiosCount: Int): UserDetails {
        this.audiosCount = audiosCount
        return this
    }

    fun getArticlesCount(): Int {
        return articlesCount
    }

    fun setArticlesCount(articlesCount: Int): UserDetails {
        this.articlesCount = articlesCount
        return this
    }

    fun getProductsCount(): Int {
        return productsCount
    }

    fun setProductsCount(productsCount: Int): UserDetails {
        this.productsCount = productsCount
        return this
    }

    fun getVideosCount(): Int {
        return videosCount
    }

    fun setVideosCount(videosCount: Int): UserDetails {
        this.videosCount = videosCount
        return this
    }

    fun getAllWallCount(): Int {
        return allWallCount
    }

    fun setAllWallCount(allWallCount: Int): UserDetails {
        this.allWallCount = allWallCount
        return this
    }

    fun getOwnWallCount(): Int {
        return ownWallCount
    }

    fun setOwnWallCount(ownWallCount: Int): UserDetails {
        this.ownWallCount = ownWallCount
        return this
    }

    fun getPostponedWallCount(): Int {
        return postponedWallCount
    }

    fun setPostponedWallCount(postponedWallCount: Int): UserDetails {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun getGiftCount(): Int {
        return giftCount
    }

    fun setGiftCount(giftCount: Int): UserDetails {
        this.giftCount = giftCount
        return this
    }

    class Relative() : Parcelable {
        private var user: User? = null
        private var type: String? = null
        private var name: String? = null

        constructor(parcel: Parcel) : this() {
            user = parcel.readTypedObjectCompat(User.CREATOR)
            type = parcel.readString()
            name = parcel.readString()
        }

        fun getName(): String? {
            return name
        }

        fun setName(name: String?): Relative {
            this.name = name
            return this
        }

        fun getUser(): User? {
            return user
        }

        fun setUser(user: User?): Relative {
            this.user = user
            return this
        }

        fun getType(): String? {
            return type
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
        private var enabled = false
        private var images: ArrayList<CoverImage>? = null

        constructor(parcel: Parcel) : this() {
            enabled = parcel.getBoolean()
            images = parcel.createTypedArrayList(CoverImage.CREATOR)
        }

        fun getImages(): ArrayList<CoverImage>? {
            return images
        }

        fun setImages(images: ArrayList<CoverImage>?): Cover {
            this.images = images
            return this
        }

        fun isEnabled(): Boolean {
            return enabled
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

    class CoverImage(private val url: String?, private val height: Int, private val width: Int) :
        Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt()
        )

        fun getHeight(): Int {
            return height
        }

        fun getWidth(): Int {
            return width
        }

        fun getUrl(): String? {
            return url
        }

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
        parcel.writeTypedObjectCompat(ParcelableOwnerWrapper(relationPartner), flags)
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