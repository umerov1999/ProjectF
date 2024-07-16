package dev.ragnarok.fenrir.fragment.wall.userdetails

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Sex
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.model.menu.Section
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.AppTextUtils.getDateWithZeros
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.joinNonEmptyStrings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class UserDetailsPresenter(
    accountId: Long,
    private val user: User,
    private val details: UserDetails,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IUserDetailsView>(accountId, savedInstanceState) {
    private val photos_profile = ArrayList<Photo>()
    private var current_select = 0
    private val userDetailsList = ArrayList<AdvancedItem>()
    fun fireChatClick() {
        val peer = Peer(
            Peer.fromUserId(
                user.getOwnerObjectId()
            )
        )
            .setAvaUrl(user.maxSquareAvatar)
            .setTitle(user.fullName)
        view?.openChatWith(
            accountId,
            accountId,
            peer
        )
    }

    fun firePhotoClick() {
        if (photos_profile.isEmpty() || current_select < 0 || current_select > photos_profile.size - 1) {
            view?.openPhotoUser(
                user
            )
            return
        }
        view?.openPhotoAlbum(
            accountId,
            user.ownerId,
            -6,
            photos_profile,
            current_select
        )
    }

    private fun displayUserProfileAlbum(photos: List<Photo>) {
        if (photos.isEmpty()) {
            return
        }
        val currentAvatarPhotoId = details.photoId?.id
        val currentAvatarOwner_id = details.photoId?.ownerId
        var sel = 0
        if (currentAvatarPhotoId != null && currentAvatarOwner_id != null) {
            var ut = 0
            for (i in photos) {
                if (i.ownerId == currentAvatarOwner_id && i.getObjectId() == currentAvatarPhotoId) {
                    sel = ut
                    break
                }
                ut++
            }
        }
        current_select = sel
        photos_profile.clear()
        photos_profile.addAll(photos)
        val finalSel = sel
        view?.onPhotosLoaded(photos[finalSel])
    }

    private fun createData() {
        val mainSection = Section(Text(R.string.mail_information))
        val domain =
            if (user.domain.nonNullNoEmpty()) "@" + user.domain else "@" + user.getOwnerObjectId()
        userDetailsList.add(
            AdvancedItem(
                1, Text(R.string.id),
                AdvancedItem.TYPE_COPY_DETAILS_ONLY,
                autolink = false
            )
                .setSubtitle(Text(domain))
                .setIcon(Icon.fromResources(R.drawable.person))
                .setSection(mainSection)
        )
        userDetailsList.add(
            AdvancedItem(2, Text(R.string.sex))
                .setSubtitle(
                    Text(
                        when (user.sex) {
                            Sex.MAN -> R.string.gender_man
                            Sex.WOMAN -> R.string.gender_woman
                            else -> R.string.role_unknown
                        }
                    )
                )
                .setIcon(
                    Icon.fromResources(
                        when (user.sex) {
                            Sex.MAN -> R.drawable.gender_male
                            Sex.WOMAN -> R.drawable.gender_female
                            else -> R.drawable.gender
                        }
                    )
                )
                .setSection(mainSection)
        )
        if (user.bdate.nonNullNoEmpty()) {
            val formatted = getDateWithZeros(user.bdate)
            userDetailsList.add(
                AdvancedItem(3, Text(R.string.birthday), autolink = false)
                    .setSubtitle(Text(formatted))
                    .setIcon(Icon.fromResources(R.drawable.cake))
                    .setSection(mainSection)
            )
        }
        details.city.requireNonNull {
            userDetailsList.add(
                AdvancedItem(4, Text(R.string.city))
                    .setSubtitle(Text(it.title))
                    .setIcon(Icon.fromResources(R.drawable.ic_city))
                    .setSection(mainSection)
            )
        }
        details.country.requireNonNull {
            userDetailsList.add(
                AdvancedItem(5, Text(R.string.country))
                    .setSubtitle(Text(it.title))
                    .setIcon(Icon.fromResources(R.drawable.ic_country))
                    .setSection(mainSection)
            )
        }
        details.hometown.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(6, Text(R.string.hometown))
                    .setSubtitle(Text(it))
                    .setIcon(Icon.fromResources(R.drawable.ic_city))
                    .setSection(mainSection)
            )
        }
        details.phone.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(7, Text(R.string.mobile_phone_number))
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.cellphone)
                    .setSection(mainSection)
            )
        }
        details.homePhone.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(8, Text(R.string.home_phone_number))
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.cellphone)
                    .setSection(mainSection)
            )
        }
        details.skype.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(9, Text(R.string.skype), AdvancedItem.TYPE_COPY_DETAILS_ONLY)
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.ic_skype)
                    .setSection(mainSection)
            )
        }
        details.instagram.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(10, Text(R.string.instagram), AdvancedItem.TYPE_OPEN_URL)
                    .setSubtitle(Text(it))
                    .setUrlPrefix("https://www.instagram.com")
                    .setIcon(R.drawable.instagram)
                    .setSection(mainSection)
            )
        }
        details.twitter.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(11, Text(R.string.twitter), AdvancedItem.TYPE_OPEN_URL)
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.twitter)
                    .setUrlPrefix("https://mobile.twitter.com")
                    .setSection(mainSection)
            )
        }
        details.facebook.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(12, Text(R.string.facebook), AdvancedItem.TYPE_OPEN_URL)
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.facebook)
                    .setUrlPrefix("https://m.facebook.com")
                    .setSection(mainSection)
            )
        }
        user.status.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(13, Text(R.string.status))
                    .setSubtitle(Text(it))
                    .setIcon(R.drawable.ic_profile_status)
                    .setSection(mainSection)
            )
        }
        details.languages.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(14, Text(R.string.languages))
                    .setIcon(R.drawable.ic_language)
                    .setSubtitle(
                        Text(
                            join(
                                it,
                                ", "
                            ) { orig ->
                                orig
                            }
                        )
                    )
                    .setSection(mainSection)
            )
        }
        details.site.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(15, Text(R.string.website))
                    .setIcon(R.drawable.ic_site)
                    .setSection(mainSection)
                    .setSubtitle(Text(it))
            )
        }
        userDetailsList.add(
            AdvancedItem(16, Text(R.string.profile))
                .setSubtitle(Text((if (details.isClosed) R.string.closed else R.string.opened)))
                .setIcon(R.drawable.lock_outline)
                .setSection(mainSection)
        )
        val pesonal = Section(Text(R.string.personal_information))
        addPersonalInfo(
            userDetailsList,
            R.drawable.star,
            17,
            pesonal,
            R.string.interests,
            details.interests
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.star,
            18,
            pesonal,
            R.string.activities,
            details.activities
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.music,
            19,
            pesonal,
            R.string.favorite_music,
            details.music
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.movie,
            20,
            pesonal,
            R.string.favorite_movies,
            details.movies
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.ic_favorite_tv,
            21,
            pesonal,
            R.string.favorite_tv_shows,
            details.tv
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.ic_favorite_quotes,
            22,
            pesonal,
            R.string.favorite_quotes,
            details.quotes
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.ic_favorite_game,
            23,
            pesonal,
            R.string.favorite_games,
            details.games
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.ic_about_me,
            24,
            pesonal,
            R.string.about_me,
            details.about
        )
        addPersonalInfo(
            userDetailsList,
            R.drawable.book,
            25,
            pesonal,
            R.string.favorite_books,
            details.books
        )
        val beliefs = Section(Text(R.string.beliefs))
        getPoliticalViewRes(details.political).requireNonNull {
            userDetailsList.add(
                AdvancedItem(26, Text(R.string.political_views))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getPoliticalViewRes(
                                details.political
                            )
                        )
                    )
            )
        }
        getLifeMainRes(details.lifeMain).requireNonNull {
            userDetailsList.add(
                AdvancedItem(27, Text(R.string.personal_priority))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getLifeMainRes(
                                details.lifeMain
                            )
                        )
                    )
            )
        }
        getPeopleMainRes(details.peopleMain).requireNonNull {
            userDetailsList.add(
                AdvancedItem(28, Text(R.string.important_in_others))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getPeopleMainRes(
                                details.peopleMain
                            )
                        )
                    )
            )
        }
        getAlcoholOrSmokingViewRes(details.smoking).requireNonNull {
            userDetailsList.add(
                AdvancedItem(29, Text(R.string.views_on_smoking))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getAlcoholOrSmokingViewRes(
                                details.smoking
                            )
                        )
                    )
            )
        }
        getAlcoholOrSmokingViewRes(details.alcohol).requireNonNull {
            userDetailsList.add(
                AdvancedItem(30, Text(R.string.views_on_alcohol))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getAlcoholOrSmokingViewRes(
                                details.alcohol
                            )
                        )
                    )
            )
        }
        details.inspiredBy.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(31, Text(R.string.inspired_by))
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSection(beliefs)
                    .setSubtitle(Text(it))
            )
        }
        details.religion.nonNullNoEmpty {
            userDetailsList.add(
                AdvancedItem(32, Text(R.string.world_view))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(Text(it))
            )
        }
        details.careers.nonNullNoEmpty {
            val career = Section(Text(R.string.career))
            for (c in it) {
                val icon =
                    if (c.group == null) Icon.fromResources(R.drawable.ic_career) else Icon.fromUrl(
                        c.group?.get100photoOrSmaller()
                    )
                val term =
                    c.from.toString() + " - " + if (c.until == 0) getString(R.string.activity_until_now) else c.until.toString()
                val company = if (c.group == null) c.company else c.group?.fullName
                val title = if (c.position.isNullOrEmpty()) company else c.position + ", " + company
                userDetailsList.add(
                    AdvancedItem(33, Text(title))
                        .setSubtitle(Text(term))
                        .setIcon(icon)
                        .setSection(career)
                        .setTag(c.group)
                )
            }
        }
        details.militaries.nonNullNoEmpty {
            val section = Section(Text(R.string.military_service))
            for (m in it) {
                val term =
                    m.from.toString() + " - " + if (m.until == 0) getString(R.string.activity_until_now) else m.until.toString()
                userDetailsList.add(
                    AdvancedItem(34, Text(m.unit))
                        .setSubtitle(Text(term))
                        .setIcon(R.drawable.ic_military)
                        .setSection(section)
                )
            }
        }
        if (details.universities.nonNullNoEmpty() || details.schools.nonNullNoEmpty()) {
            val section = Section(Text(R.string.education))
            if (details.universities.nonNullNoEmpty()) {
                for (u in details.universities.orEmpty()) {
                    val title = u.name
                    val subtitle =
                        joinNonEmptyStrings(
                            "\n",
                            u.facultyName,
                            u.chairName,
                            u.form,
                            u.status
                        )
                    userDetailsList.add(
                        AdvancedItem(35, Text(title))
                            .setSection(section)
                            .setSubtitle(if (subtitle.isNullOrEmpty()) null else Text(subtitle))
                            .setIcon(R.drawable.ic_university)
                    )
                }
            }
            details.schools.nonNullNoEmpty {
                for (s in it) {
                    val title = joinNonEmptyStrings(", ", s.name, s.clazz)
                    val term: Text? = if (s.from > 0) {
                        Text(s.from.toString() + " - " + if (s.to == 0) getString(R.string.activity_until_now) else s.to.toString())
                    } else {
                        null
                    }
                    userDetailsList.add(
                        AdvancedItem(36, Text(title))
                            .setSection(section)
                            .setSubtitle(term)
                            .setIcon(R.drawable.ic_school)
                    )
                }
            }
        }
        if (details.relation > 0 || details.relatives
                .nonNullNoEmpty() || details.relationPartner != null
        ) {
            val section = Section(Text(R.string.family))
            if (details.relation > 0 || details.relationPartner != null) {
                val icon: Icon
                val subtitle: Text
                @StringRes val relationRes = getRelationStringByType(
                    details.relation
                )
                if (details.relationPartner != null) {
                    icon = Icon.fromUrl(details.relationPartner?.get100photoOrSmaller())
                    subtitle = Text(
                        getString(relationRes) + details.relationPartner?.fullName.nonNullNoEmpty(
                            { " $it" },
                            { "" })
                    )
                } else {
                    subtitle = Text(relationRes)
                    icon = Icon.fromResources(R.drawable.ic_relation)
                }
                userDetailsList.add(
                    AdvancedItem(37, Text(R.string.relationship))
                        .setSection(section)
                        .setSubtitle(subtitle)
                        .setIcon(icon)
                        .setTag(details.relationPartner)
                )
            }
            details.relatives.requireNonNull {
                for (r in it) {
                    val icon =
                        if (r.user == null) Icon.fromResources(R.drawable.ic_relative_user) else Icon.fromUrl(
                            r.user?.get100photoOrSmaller()
                        )
                    val subtitle = if (r.user == null) r.name else r.user?.fullName
                    userDetailsList.add(
                        AdvancedItem(38, Text(getRelativeStringByType(r.type)))
                            .setIcon(icon)
                            .setSubtitle(Text(subtitle))
                            .setSection(section)
                            .setTag(r.user)
                    )
                }
            }
        }
    }

    @StringRes
    private fun getRelationStringByType(relation: Int): Int {
        when (user.sex) {
            Sex.MAN, Sex.UNKNOWN -> when (relation) {
                VKApiUser.Relation.SINGLE -> return R.string.relationship_man_single
                VKApiUser.Relation.RELATIONSHIP -> return R.string.relationship_man_in_relationship
                VKApiUser.Relation.ENGAGED -> return R.string.relationship_man_engaged
                VKApiUser.Relation.MARRIED -> return R.string.relationship_man_married
                VKApiUser.Relation.COMPLICATED -> return R.string.relationship_man_its_complicated
                VKApiUser.Relation.SEARCHING -> return R.string.relationship_man_activelly_searching
                VKApiUser.Relation.IN_LOVE -> return R.string.relationship_man_in_love
                VKApiUser.Relation.IN_A_CIVIL_UNION -> return R.string.in_a_civil_union
            }

            Sex.WOMAN -> when (relation) {
                VKApiUser.Relation.SINGLE -> return R.string.relationship_woman_single
                VKApiUser.Relation.RELATIONSHIP -> return R.string.relationship_woman_in_relationship
                VKApiUser.Relation.ENGAGED -> return R.string.relationship_woman_engaged
                VKApiUser.Relation.MARRIED -> return R.string.relationship_woman_married
                VKApiUser.Relation.COMPLICATED -> return R.string.relationship_woman_its_complicated
                VKApiUser.Relation.SEARCHING -> return R.string.relationship_woman_activelly_searching
                VKApiUser.Relation.IN_LOVE -> return R.string.relationship_woman_in_love
                VKApiUser.Relation.IN_A_CIVIL_UNION -> return R.string.in_a_civil_union
            }
        }
        return R.string.relatives_others
    }

    @StringRes
    private fun getRelativeStringByType(type: String?): Int {
        return if (type == null) {
            R.string.relatives_others
        } else when (type) {
            VKApiUser.RelativeType.CHILD -> R.string.relatives_children
            VKApiUser.RelativeType.GRANDCHILD -> R.string.relatives_grandchildren
            VKApiUser.RelativeType.PARENT -> R.string.relatives_parents
            VKApiUser.RelativeType.SUBLING -> R.string.relatives_siblings
            else -> R.string.relatives_others
        }
    }

    override fun onGuiCreated(viewHost: IUserDetailsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayToolbarTitle(user)
        viewHost.displayData(userDetailsList)
        if (photos_profile.isNotEmpty() && current_select >= 0 && current_select < photos_profile.size - 1) {
            viewHost.onPhotosLoaded(photos_profile[current_select])
        }
    }

    fun fireItemClick(item: AdvancedItem) {
        val tag = item.tag
        if (tag is Owner) {
            view?.openOwnerProfile(
                accountId,
                tag.ownerId,
                tag
            )
        }
    }

    companion object {
        internal fun addPersonalInfo(
            items: MutableList<AdvancedItem>,
            @DrawableRes icon: Int,
            key: Long,
            section: Section,
            @StringRes title: Int,
            v: String?
        ) {
            if (v.nonNullNoEmpty()) {
                items.add(
                    AdvancedItem(key, Text(title))
                        .setIcon(icon)
                        .setSection(section)
                        .setSubtitle(Text(v))
                )
            }
        }

        internal fun getPoliticalViewRes(political: Int): Int? {
            return when (political) {
                1 -> R.string.political_views_communist
                2 -> R.string.political_views_socialist
                3 -> R.string.political_views_moderate
                4 -> R.string.political_views_liberal
                5 -> R.string.political_views_conservative
                6 -> R.string.political_views_monarchist
                7 -> R.string.political_views_ultraconservative
                8 -> R.string.political_views_apathetic
                9 -> R.string.political_views_libertian
                else -> null
            }
        }

        internal fun getPeopleMainRes(peopleMain: Int): Int? {
            return when (peopleMain) {
                1 -> R.string.important_in_others_intellect_and_creativity
                2 -> R.string.important_in_others_kindness_and_honesty
                3 -> R.string.important_in_others_health_and_beauty
                4 -> R.string.important_in_others_wealth_and_power
                5 -> R.string.important_in_others_courage_and_persistance
                6 -> R.string.important_in_others_humor_and_love_for_life
                else -> null
            }
        }

        internal fun getLifeMainRes(lifeMain: Int): Int? {
            return when (lifeMain) {
                1 -> R.string.personal_priority_family_and_children
                2 -> R.string.personal_priority_career_and_money
                3 -> R.string.personal_priority_entertainment_and_leisure
                4 -> R.string.personal_priority_science_and_research
                5 -> R.string.personal_priority_improving_the_world
                6 -> R.string.personal_priority_personal_development
                7 -> R.string.personal_priority_beauty_and_art
                8 -> R.string.personal_priority_fame_and_influence
                else -> null
            }
        }

        internal fun getAlcoholOrSmokingViewRes(value: Int): Int? {
            return when (value) {
                1 -> R.string.views_very_negative
                2 -> R.string.views_negative
                3 -> R.string.views_neutral
                4 -> R.string.views_compromisable
                5 -> R.string.views_positive
                else -> null
            }
        }
    }

    init {
        createData()
        view?.notifyChanges()
        appendJob(
            InteractorFactory.createPhotosInteractor()[accountId, user.ownerId, -6, 50, 0, true]
                .fromIOToMain { displayUserProfileAlbum(it) }
        )
    }
}