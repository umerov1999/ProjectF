package dev.ragnarok.fenrir.fragment.userdetails

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.model.menu.Section
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.AppTextUtils.getDateWithZeros
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.joinNonEmptyStrings
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore

class UserDetailsPresenter(
    accountId: Long,
    private val user: User,
    details: UserDetails,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IUserDetailsView>(accountId, savedInstanceState) {
    private var details: UserDetails = UserDetails()
    private var photos_profile: List<Photo> = ArrayList(1)
    private var current_select = 0
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
            ArrayList(photos_profile),
            current_select
        )
    }

    private fun displayUserProfileAlbum(photos: List<Photo>) {
        if (photos.isEmpty()) {
            return
        }
        val currentAvatarPhotoId = details.getPhotoId()?.id
        val currentAvatarOwner_id = details.getPhotoId()?.ownerId
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
        photos_profile = photos
        val finalSel = sel
        view?.onPhotosLoaded(photos[finalSel])
    }

    private fun createData(): List<AdvancedItem> {
        val items: MutableList<AdvancedItem> = ArrayList()
        val mainSection = Section(Text(R.string.mail_information))
        val domain =
            if (user.domain.nonNullNoEmpty()) "@" + user.domain else "@" + user.getOwnerObjectId()
        items.add(
            AdvancedItem(1, AdvancedItem.TYPE_COPY_DETAILS_ONLY, Text(R.string.id))
                .setSubtitle(Text(domain))
                .setIcon(Icon.fromResources(R.drawable.person))
                .setSection(mainSection)
        )
        items.add(
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
            items.add(
                AdvancedItem(3, Text(R.string.birthday))
                    .setSubtitle(Text(formatted))
                    .setIcon(Icon.fromResources(R.drawable.cake))
                    .setSection(mainSection)
            )
        }
        details.getCity().requireNonNull {
            items.add(
                AdvancedItem(4, Text(R.string.city))
                    .setSubtitle(Text(it.title))
                    .setIcon(Icon.fromResources(R.drawable.ic_city))
                    .setSection(mainSection)
            )
        }
        details.getCountry().requireNonNull {
            items.add(
                AdvancedItem(5, Text(R.string.country))
                    .setSubtitle(Text(it.title))
                    .setIcon(Icon.fromResources(R.drawable.ic_country))
                    .setSection(mainSection)
            )
        }
        if (details.getHometown().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(6, Text(R.string.hometown))
                    .setSubtitle(Text(details.getHometown()))
                    .setIcon(Icon.fromResources(R.drawable.ic_city))
                    .setSection(mainSection)
            )
        }
        if (details.getPhone().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(7, Text(R.string.mobile_phone_number))
                    .setSubtitle(Text(details.getPhone()))
                    .setIcon(R.drawable.cellphone)
                    .setSection(mainSection)
            )
        }
        if (details.getHomePhone().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(8, Text(R.string.home_phone_number))
                    .setSubtitle(Text(details.getHomePhone()))
                    .setIcon(R.drawable.cellphone)
                    .setSection(mainSection)
            )
        }
        if (details.getSkype().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(9, AdvancedItem.TYPE_COPY_DETAILS_ONLY, Text(R.string.skype))
                    .setSubtitle(Text(details.getSkype()))
                    .setIcon(R.drawable.ic_skype)
                    .setSection(mainSection)
            )
        }
        if (details.getInstagram().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(10, AdvancedItem.TYPE_OPEN_URL, Text(R.string.instagram))
                    .setSubtitle(Text(details.getInstagram()))
                    .setUrlPrefix("https://www.instagram.com")
                    .setIcon(R.drawable.instagram)
                    .setSection(mainSection)
            )
        }
        if (details.getTwitter().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(11, AdvancedItem.TYPE_OPEN_URL, Text(R.string.twitter))
                    .setSubtitle(Text(details.getTwitter()))
                    .setIcon(R.drawable.twitter)
                    .setUrlPrefix("https://mobile.twitter.com")
                    .setSection(mainSection)
            )
        }
        if (details.getFacebook().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(12, AdvancedItem.TYPE_OPEN_URL, Text(R.string.facebook))
                    .setSubtitle(Text(details.getFacebook()))
                    .setIcon(R.drawable.facebook)
                    .setUrlPrefix("https://m.facebook.com")
                    .setSection(mainSection)
            )
        }
        if (user.status.nonNullNoEmpty()) {
            items.add(
                AdvancedItem(13, Text(R.string.status))
                    .setSubtitle(Text(user.status))
                    .setIcon(R.drawable.ic_profile_status)
                    .setSection(mainSection)
            )
        }
        details.getLanguages().nonNullNoEmpty {
            items.add(
                AdvancedItem(14, Text(R.string.languages))
                    .setIcon(R.drawable.ic_language)
                    .setSubtitle(
                        Text(
                            join(
                                it,
                                ", ",
                                object : Utils.SimpleFunction<String, String> {
                                    override fun apply(orig: String): String {
                                        return orig
                                    }
                                })
                        )
                    )
                    .setSection(mainSection)
            )
        }
        if (details.getSite().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(15, Text(R.string.website))
                    .setIcon(R.drawable.ic_site)
                    .setSection(mainSection)
                    .setSubtitle(Text(details.getSite()))
            )
        }
        items.add(
            AdvancedItem(16, Text(R.string.profile))
                .setSubtitle(Text((if (details.isClosed()) R.string.closed else R.string.opened)))
                .setIcon(R.drawable.lock_outline)
                .setSection(mainSection)
        )
        val pesonal = Section(Text(R.string.personal_information))
        addPersonalInfo(
            items,
            R.drawable.star,
            17,
            pesonal,
            R.string.interests,
            details.getInterests()
        )
        addPersonalInfo(
            items,
            R.drawable.star,
            18,
            pesonal,
            R.string.activities,
            details.getActivities()
        )
        addPersonalInfo(
            items,
            R.drawable.music,
            19,
            pesonal,
            R.string.favorite_music,
            details.getMusic()
        )
        addPersonalInfo(
            items,
            R.drawable.movie,
            20,
            pesonal,
            R.string.favorite_movies,
            details.getMovies()
        )
        addPersonalInfo(
            items,
            R.drawable.ic_favorite_tv,
            21,
            pesonal,
            R.string.favorite_tv_shows,
            details.getTv()
        )
        addPersonalInfo(
            items,
            R.drawable.ic_favorite_quotes,
            22,
            pesonal,
            R.string.favorite_quotes,
            details.getQuotes()
        )
        addPersonalInfo(
            items,
            R.drawable.ic_favorite_game,
            23,
            pesonal,
            R.string.favorite_games,
            details.getGames()
        )
        addPersonalInfo(
            items,
            R.drawable.ic_about_me,
            24,
            pesonal,
            R.string.about_me,
            details.getAbout()
        )
        addPersonalInfo(
            items,
            R.drawable.book,
            25,
            pesonal,
            R.string.favorite_books,
            details.getBooks()
        )
        val beliefs = Section(Text(R.string.beliefs))
        if (getPoliticalViewRes(details.getPolitical()) != null) {
            items.add(
                AdvancedItem(26, Text(R.string.political_views))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getPoliticalViewRes(
                                details.getPolitical()
                            )
                        )
                    )
            )
        }
        if (getLifeMainRes(details.getLifeMain()) != null) {
            items.add(
                AdvancedItem(27, Text(R.string.personal_priority))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getLifeMainRes(
                                details.getLifeMain()
                            )
                        )
                    )
            )
        }
        if (getPeopleMainRes(details.getPeopleMain()) != null) {
            items.add(
                AdvancedItem(28, Text(R.string.important_in_others))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getPeopleMainRes(
                                details.getPeopleMain()
                            )
                        )
                    )
            )
        }
        if (getAlcoholOrSmokingViewRes(details.getSmoking()) != null) {
            items.add(
                AdvancedItem(29, Text(R.string.views_on_smoking))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getAlcoholOrSmokingViewRes(
                                details.getSmoking()
                            )
                        )
                    )
            )
        }
        if (getAlcoholOrSmokingViewRes(details.getAlcohol()) != null) {
            items.add(
                AdvancedItem(30, Text(R.string.views_on_alcohol))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(
                        Text(
                            getAlcoholOrSmokingViewRes(
                                details.getAlcohol()
                            )
                        )
                    )
            )
        }
        if (details.getInspiredBy().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(31, Text(R.string.inspired_by))
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSection(beliefs)
                    .setSubtitle(Text(details.getInspiredBy()))
            )
        }
        if (details.getReligion().nonNullNoEmpty()) {
            items.add(
                AdvancedItem(32, Text(R.string.world_view))
                    .setSection(beliefs)
                    .setIcon(R.drawable.ic_profile_personal)
                    .setSubtitle(Text(details.getReligion()))
            )
        }
        details.getCareers().nonNullNoEmpty {
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
                items.add(
                    AdvancedItem(33, Text(title))
                        .setSubtitle(Text(term))
                        .setIcon(icon)
                        .setSection(career)
                        .setTag(c.group)
                )
            }
        }
        details.getMilitaries().nonNullNoEmpty {
            val section = Section(Text(R.string.military_service))
            for (m in it) {
                val term =
                    m.from.toString() + " - " + if (m.until == 0) getString(R.string.activity_until_now) else m.until.toString()
                items.add(
                    AdvancedItem(34, Text(m.unit))
                        .setSubtitle(Text(term))
                        .setIcon(R.drawable.ic_military)
                        .setSection(section)
                )
            }
        }
        if (details.getUniversities().nonNullNoEmpty() || details.getSchools().nonNullNoEmpty()) {
            val section = Section(Text(R.string.education))
            if (details.getUniversities().nonNullNoEmpty()) {
                for (u in details.getUniversities().orEmpty()) {
                    val title = u.getName()
                    val subtitle =
                        joinNonEmptyStrings(
                            "\n",
                            u.getFacultyName(),
                            u.getChairName(),
                            u.getForm(),
                            u.getStatus()
                        )
                    items.add(
                        AdvancedItem(35, Text(title))
                            .setSection(section)
                            .setSubtitle(if (subtitle.isNullOrEmpty()) null else Text(subtitle))
                            .setIcon(R.drawable.ic_university)
                    )
                }
            }
            details.getSchools().nonNullNoEmpty {
                for (s in it) {
                    val title = joinNonEmptyStrings(", ", s.name, s.clazz)
                    val term: Text? = if (s.from > 0) {
                        Text(s.from.toString() + " - " + if (s.to == 0) getString(R.string.activity_until_now) else s.to.toString())
                    } else {
                        null
                    }
                    items.add(
                        AdvancedItem(36, Text(title))
                            .setSection(section)
                            .setSubtitle(term)
                            .setIcon(R.drawable.ic_school)
                    )
                }
            }
        }
        if (details.getRelation() > 0 || details.getRelatives()
                .nonNullNoEmpty() || details.getRelationPartner() != null
        ) {
            val section = Section(Text(R.string.family))
            if (details.getRelation() > 0 || details.getRelationPartner() != null) {
                val icon: Icon
                val subtitle: Text
                @StringRes val relationRes = getRelationStringByType(
                    details.getRelation()
                )
                if (details.getRelationPartner() != null) {
                    icon = Icon.fromUrl(details.getRelationPartner()?.get100photoOrSmaller())
                    subtitle = Text(
                        getString(relationRes) + details.getRelationPartner()?.fullName.nonNullNoEmpty(
                            { " $it" },
                            { "" })
                    )
                } else {
                    subtitle = Text(relationRes)
                    icon = Icon.fromResources(R.drawable.ic_relation)
                }
                items.add(
                    AdvancedItem(37, Text(R.string.relationship))
                        .setSection(section)
                        .setSubtitle(subtitle)
                        .setIcon(icon)
                        .setTag(details.getRelationPartner())
                )
            }
            details.getRelatives().requireNonNull {
                for (r in it) {
                    val icon =
                        if (r.getUser() == null) Icon.fromResources(R.drawable.ic_relative_user) else Icon.fromUrl(
                            r.getUser()?.get100photoOrSmaller()
                        )
                    val subtitle = if (r.getUser() == null) r.getName() else r.getUser()?.fullName
                    items.add(
                        AdvancedItem(38, Text(getRelativeStringByType(r.getType())))
                            .setIcon(icon)
                            .setSubtitle(Text(subtitle))
                            .setSection(section)
                            .setTag(r.getUser())
                    )
                }
            }
        }
        return items
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
        viewHost.displayData(createData())
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
        this.details = details
        appendDisposable(
            InteractorFactory.createPhotosInteractor()[accountId, user.ownerId, -6, 50, 0, true]
                .fromIOToMain()
                .subscribe({ displayUserProfileAlbum(it) }, ignore())
        )
    }
}