package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.IdPairEntity
import kotlinx.serialization.Serializable

@Keep
@Serializable
class UserDetailsEntity {
    var photoId: IdPairEntity? = null
        private set
    var statusAudio: AudioDboEntity? = null
        private set
    var isSetFavorite = false
        private set
    var isSetSubscribed = false
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
    var productServicesCount = 0
        private set
    var narrativesCount = 0
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
    var city: CityEntity? = null
        private set
    var country: CountryDboEntity? = null
        private set
    var homeTown: String? = null
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
    var careers: List<CareerEntity>? = null
        private set
    var militaries: List<MilitaryEntity>? = null
        private set
    var universities: List<UniversityEntity>? = null
        private set
    var schools: List<SchoolEntity>? = null
        private set
    var relatives: List<RelativeEntity>? = null
        private set
    var relation = 0
        private set
    var relationPartnerId = 0
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

    fun setClosed(closed: Boolean): UserDetailsEntity {
        this.isClosed = closed
        return this
    }

    fun setProductServicesCount(productServicesCount: Int): UserDetailsEntity {
        this.productServicesCount = productServicesCount
        return this
    }

    fun setNarrativesCount(narrativesCount: Int): UserDetailsEntity {
        this.narrativesCount = narrativesCount
        return this
    }

    fun setInterests(interests: String?): UserDetailsEntity {
        this.interests = interests
        return this
    }

    fun setMusic(music: String?): UserDetailsEntity {
        this.music = music
        return this
    }

    fun setActivities(activities: String?): UserDetailsEntity {
        this.activities = activities
        return this
    }

    fun setFavorite(isFavorite: Boolean): UserDetailsEntity {
        isSetFavorite = isFavorite
        return this
    }

    fun setSubscribed(isSubscribed: Boolean): UserDetailsEntity {
        isSetSubscribed = isSubscribed
        return this
    }

    fun setMovies(movies: String?): UserDetailsEntity {
        this.movies = movies
        return this
    }

    fun setTv(tv: String?): UserDetailsEntity {
        this.tv = tv
        return this
    }

    fun setGames(games: String?): UserDetailsEntity {
        this.games = games
        return this
    }

    fun setQuotes(quotes: String?): UserDetailsEntity {
        this.quotes = quotes
        return this
    }

    fun setAbout(about: String?): UserDetailsEntity {
        this.about = about
        return this
    }

    fun setBooks(books: String?): UserDetailsEntity {
        this.books = books
        return this
    }

    fun setSite(site: String?): UserDetailsEntity {
        this.site = site
        return this
    }

    fun setAlcohol(alcohol: Int): UserDetailsEntity {
        this.alcohol = alcohol
        return this
    }

    fun setLifeMain(lifeMain: Int): UserDetailsEntity {
        this.lifeMain = lifeMain
        return this
    }

    fun setPeopleMain(peopleMain: Int): UserDetailsEntity {
        this.peopleMain = peopleMain
        return this
    }

    fun setPolitical(political: Int): UserDetailsEntity {
        this.political = political
        return this
    }

    fun setSmoking(smoking: Int): UserDetailsEntity {
        this.smoking = smoking
        return this
    }

    fun setInspiredBy(inspiredBy: String?): UserDetailsEntity {
        this.inspiredBy = inspiredBy
        return this
    }

    fun setReligion(religion: String?): UserDetailsEntity {
        this.religion = religion
        return this
    }

    fun setLanguages(languages: Array<String>?): UserDetailsEntity {
        this.languages = languages
        return this
    }

    fun setRelation(relation: Int): UserDetailsEntity {
        this.relation = relation
        return this
    }

    fun setRelationPartnerId(relationPartnerId: Int): UserDetailsEntity {
        this.relationPartnerId = relationPartnerId
        return this
    }

    fun setRelatives(relatives: List<RelativeEntity>?): UserDetailsEntity {
        this.relatives = relatives
        return this
    }

    fun setSchools(schools: List<SchoolEntity>?): UserDetailsEntity {
        this.schools = schools
        return this
    }

    fun setUniversities(universities: List<UniversityEntity>?): UserDetailsEntity {
        this.universities = universities
        return this
    }

    fun setMilitaries(militaries: List<MilitaryEntity>?): UserDetailsEntity {
        this.militaries = militaries
        return this
    }

    fun setCareers(careers: List<CareerEntity>?): UserDetailsEntity {
        this.careers = careers
        return this
    }

    fun setSkype(skype: String?): UserDetailsEntity {
        this.skype = skype
        return this
    }

    fun setTwitter(twitter: String?): UserDetailsEntity {
        this.twitter = twitter
        return this
    }

    fun setInstagram(instagram: String?): UserDetailsEntity {
        this.instagram = instagram
        return this
    }

    fun setFacebook(facebook: String?): UserDetailsEntity {
        this.facebook = facebook
        return this
    }

    fun setHomePhone(homePhone: String?): UserDetailsEntity {
        this.homePhone = homePhone
        return this
    }

    fun setPhone(phone: String?): UserDetailsEntity {
        this.phone = phone
        return this
    }

    fun setHomeTown(homeTown: String?): UserDetailsEntity {
        this.homeTown = homeTown
        return this
    }

    fun setCountry(country: CountryDboEntity?): UserDetailsEntity {
        this.country = country
        return this
    }

    fun setCity(city: CityEntity?): UserDetailsEntity {
        this.city = city
        return this
    }

    fun setPhotoId(photoId: IdPairEntity?): UserDetailsEntity {
        this.photoId = photoId
        return this
    }

    fun setStatusAudio(statusAudio: AudioDboEntity?): UserDetailsEntity {
        this.statusAudio = statusAudio
        return this
    }

    fun setFriendsCount(friendsCount: Int): UserDetailsEntity {
        this.friendsCount = friendsCount
        return this
    }

    fun setOnlineFriendsCount(onlineFriendsCount: Int): UserDetailsEntity {
        this.onlineFriendsCount = onlineFriendsCount
        return this
    }

    fun setMutualFriendsCount(mutualFriendsCount: Int): UserDetailsEntity {
        this.mutualFriendsCount = mutualFriendsCount
        return this
    }

    fun setFollowersCount(followersCount: Int): UserDetailsEntity {
        this.followersCount = followersCount
        return this
    }

    fun setGroupsCount(groupsCount: Int): UserDetailsEntity {
        this.groupsCount = groupsCount
        return this
    }

    fun setPhotosCount(photosCount: Int): UserDetailsEntity {
        this.photosCount = photosCount
        return this
    }

    fun setAudiosCount(audiosCount: Int): UserDetailsEntity {
        this.audiosCount = audiosCount
        return this
    }

    fun setArticlesCount(articlesCount: Int): UserDetailsEntity {
        this.articlesCount = articlesCount
        return this
    }

    fun setProductsCount(productsCount: Int): UserDetailsEntity {
        this.productsCount = productsCount
        return this
    }

    fun setVideosCount(videosCount: Int): UserDetailsEntity {
        this.videosCount = videosCount
        return this
    }

    fun setAllWallCount(allWallCount: Int): UserDetailsEntity {
        this.allWallCount = allWallCount
        return this
    }

    fun setOwnWallCount(ownWallCount: Int): UserDetailsEntity {
        this.ownWallCount = ownWallCount
        return this
    }

    fun setPostponedWallCount(postponedWallCount: Int): UserDetailsEntity {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun setGiftCount(GiftCount: Int): UserDetailsEntity {
        giftCount = GiftCount
        return this
    }

    @Keep
    @Serializable
    class RelativeEntity {
        var id = 0
            private set
        var type: String? = null
            private set
        var name: String? = null
            private set

        fun setName(name: String?): RelativeEntity {
            this.name = name
            return this
        }

        fun setId(id: Int): RelativeEntity {
            this.id = id
            return this
        }

        fun setType(type: String?): RelativeEntity {
            this.type = type
            return this
        }
    }
}