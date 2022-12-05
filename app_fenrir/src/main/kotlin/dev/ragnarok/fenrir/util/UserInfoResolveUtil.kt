package dev.ragnarok.fenrir.util

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTimeShorted

object UserInfoResolveUtil {
    /*private static final String COLON = ": ";

    public static void fill(Context context, View container, VKApiUser apiUser) {
        BeliefsHolder holder = new BeliefsHolder(container);

        Integer politicalViewsRes = getPolitivalViewRes(apiUser);
        holder.politicalViews.setVisible(politicalViewsRes != null);
        if (politicalViewsRes != null) {
            Spannable text = buildTitledSpanLine(context, R.string.political_views, politicalViewsRes);
            holder.politicalViews.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }

        Integer personalPriorityRes = getPepsonalPriorityRes(apiUser);
        holder.personalPriority.setVisible(personalPriorityRes != null);
        if (personalPriorityRes != null) {
            Spannable text = buildTitledSpanLine(context, R.string.personal_priority, personalPriorityRes);
            holder.personalPriority.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }

        Integer importantInOthersRes = getImportantInOthersRes(apiUser);
        holder.importantInOthers.setVisible(importantInOthersRes != null);
        if (importantInOthersRes != null) {
            Spannable text = buildTitledSpanLine(context, R.string.important_in_others, importantInOthersRes);
            holder.importantInOthers.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }

        Integer smokingRes = getAlcoholOrSmokingViewRes(apiUser.smoking);
        holder.viewsOnSmoking.setVisible(smokingRes != null);
        if (smokingRes != null) {
            Spannable text = buildTitledSpanLine(context, R.string.views_on_smoking, smokingRes);
            holder.viewsOnSmoking.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }

        Integer alcoholRes = getAlcoholOrSmokingViewRes(apiUser.alcohol);
        holder.viewsOnAlcohol.setVisible(smokingRes != null);
        if (alcoholRes != null) {
            Spannable text = buildTitledSpanLine(context, R.string.views_on_alcohol, alcoholRes);
            holder.viewsOnAlcohol.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }

        resolveSimpleInfoLine(context, holder.worldView, apiUser.religion, R.string.world_view);
        resolveSimpleInfoLine(context, holder.inspiredBy, apiUser.inspired_by, R.string.inspired_by);

        boolean rootVisible = holder.politicalViews.isVisible() ||
                holder.worldView.isVisible() ||
                holder.personalPriority.isVisible() ||
                holder.importantInOthers.isVisible() ||
                holder.viewsOnSmoking.isVisible() ||
                holder.viewsOnAlcohol.isVisible() ||
                holder.inspiredBy.isVisible();
        container.setVisibility(rootVisible ? View.VISIBLE : View.GONE);
    }

    private static Spannable buildTitledSpanLine(Context context, int titleRes, int bodyRes) {
        return buildTitledSpanLine(context, context.getString(titleRes), context.getString(bodyRes), false, null);
    }

    private static Spannable buildTitledSpanLine(Context context, String title, String body, boolean bodySupportOwnerLinks, OwnerLinkSpanFactory.ActionListener listener) {
        int endIndex = title.length() + COLON.length();
        String fullLine = title + COLON + body;
        Spannable spannable = bodySupportOwnerLinks ? OwnerLinkSpanFactory.withSpans(fullLine, true, false, listener) :
                Spannable.Factory.getInstance().newSpannable(fullLine);

        if (spannable != null) {
            spannable.setSpan(new ForegroundColorSpan(CurrentTheme.getPrimaryTextColorCode(context)), 0, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    public static void fillPersonalInfo(Context context, View sectionView, VKApiUser apiUser) {
        PersonalHolder holder = new PersonalHolder(sectionView);
        resolveSimpleInfoLine(context, holder.activities, apiUser.activities, R.string.activities);
        resolveSimpleInfoLine(context, holder.interests, apiUser.interests, R.string.interests);
        resolveSimpleInfoLine(context, holder.favoriteMusic, apiUser.music, R.string.favorite_music);
        resolveSimpleInfoLine(context, holder.favoriteMovies, apiUser.movies, R.string.favorite_movies);
        resolveSimpleInfoLine(context, holder.favoriteTvShows, apiUser.tv, R.string.favorite_tv_shows);
        resolveSimpleInfoLine(context, holder.favoriteBooks, apiUser.books, R.string.favorite_books);
        resolveSimpleInfoLine(context, holder.favoriteGames, apiUser.games, R.string.favorite_games);
        resolveSimpleInfoLine(context, holder.favoriteQuotes, apiUser.quotes, R.string.favorite_quotes);
        resolveSimpleInfoLine(context, holder.aboutMe, apiUser.about, R.string.about_me);

        */
    /*resolveDividers(holder.aboutMe,
                holder.favoriteQuotes,
                holder.favoriteGames,
                holder.favoriteBooks,
                holder.favoriteTvShows,
                holder.favoriteMovies,
                holder.favoriteMusic,
                holder.interests,
                holder.activities);*/
    /*

        boolean rootVisible = holder.activities.root.getVisibility() == View.VISIBLE ||
                holder.interests.root.getVisibility() == View.VISIBLE ||
                holder.favoriteMusic.root.getVisibility() == View.VISIBLE ||
                holder.favoriteMovies.root.getVisibility() == View.VISIBLE ||
                holder.favoriteTvShows.root.getVisibility() == View.VISIBLE ||
                holder.favoriteBooks.root.getVisibility() == View.VISIBLE ||
                holder.favoriteGames.root.getVisibility() == View.VISIBLE ||
                holder.favoriteQuotes.root.getVisibility() == View.VISIBLE ||
                holder.aboutMe.root.getVisibility() == View.VISIBLE;
        sectionView.setVisibility(rootVisible ? View.VISIBLE : View.GONE);
    }

    private static void resolveSimpleInfoLine(Context context, InfoLine infoLine, String value, int stringTitleRes) {
        infoLine.setVisible(!TextUtils.isEmpty(value));
        if (!TextUtils.isEmpty(value)) {
            Spannable text = buildTitledSpanLine(context, context.getString(stringTitleRes), value, false, null);
            infoLine.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }
    }

    private static void resolveSimpleInfoLine(Context context, InfoLine infoLine, String value, int stringTitleRes, OwnerLinkSpanFactory.ActionListener listener) {
        infoLine.setVisible(!TextUtils.isEmpty(value));
        if (!TextUtils.isEmpty(value)) {
            Spannable text = buildTitledSpanLine(context, context.getString(stringTitleRes), value, true, listener);
            infoLine.tvText.setText(text, TextView.BufferType.SPANNABLE);
        }
    }

    private static class PersonalHolder {

        InfoLine activities;
        InfoLine interests;
        InfoLine favoriteMusic;
        InfoLine favoriteMovies;
        InfoLine favoriteTvShows;
        InfoLine favoriteBooks;
        InfoLine favoriteGames;
        InfoLine favoriteQuotes;
        InfoLine aboutMe;

        PersonalHolder(View root) {
            activities = new InfoLine(root.findViewById(R.id.activities));
            interests = new InfoLine(root.findViewById(R.id.interests));
            favoriteMusic = new InfoLine(root.findViewById(R.id.favorite_music));
            favoriteMovies = new InfoLine(root.findViewById(R.id.favorite_movies));
            favoriteTvShows = new InfoLine(root.findViewById(R.id.favorite_tv_shows));
            favoriteBooks = new InfoLine(root.findViewById(R.id.favorite_books));
            favoriteGames = new InfoLine(root.findViewById(R.id.favorite_games));
            favoriteQuotes = new InfoLine(root.findViewById(R.id.favorite_quotes));
            aboutMe = new InfoLine(root.findViewById(R.id.about_me));
        }
    }

    private static class InfoLine {

        View root;
        TextView tvText;
        View divider;

        InfoLine(View view) {
            root = view;
            tvText = (TextView) view.findViewById(android.R.id.text1);
            divider = view.findViewById(R.id.divider);
        }

        void setVisible(boolean visible){
            root.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        boolean isVisible() {
            return root.getVisibility() == View.VISIBLE;
        }
    }

    @Nullable
    private static Integer getAlcoholOrSmokingViewRes(int value) {
        switch (value) {
            case 1:
                return R.string.views_very_negative;
            case 2:
                return R.string.views_negative;
            case 3:
                return R.string.views_neutral;
            case 4:
                return R.string.views_compromisable;
            case 5:
                return R.string.views_positive;
            default:
                return null;
        }
    }

    private static Integer getImportantInOthersRes(VKApiUser apiUser) {
        switch (apiUser.people_main) {
            case 1:
                return R.string.important_in_others_intellect_and_creativity;
            case 2:
                return R.string.important_in_others_kindness_and_honesty;
            case 3:
                return R.string.important_in_others_health_and_beauty;
            case 4:
                return R.string.important_in_others_wealth_and_power;
            case 5:
                return R.string.important_in_others_courage_and_persistance;
            case 6:
                return R.string.important_in_others_humor_and_love_for_life;
            default:
                return null;
        }
    }

    private static Integer getPepsonalPriorityRes(VKApiUser apiUser) {
        switch (apiUser.life_main) {
            case 1:
                return R.string.personal_priority_family_and_children;
            case 2:
                return R.string.personal_priority_career_and_money;
            case 3:
                return R.string.personal_priority_entertainment_and_leisure;
            case 4:
                return R.string.personal_priority_science_and_research;
            case 5:
                return R.string.personal_priority_improving_the_world;
            case 6:
                return R.string.personal_priority_personal_development;
            case 7:
                return R.string.personal_priority_beauty_and_art;
            case 8:
                return R.string.personal_priority_fame_and_influence;
            default:
                return null;
        }
    }

    private static Integer getPolitivalViewRes(VKApiUser apiUser) {
        switch (apiUser.political) {
            case 1:
                return R.string.political_views_communist;
            case 2:
                return R.string.political_views_socialist;
            case 3:
                return R.string.political_views_moderate;
            case 4:
                return R.string.political_views_liberal;
            case 5:
                return R.string.political_views_conservative;
            case 6:
                return R.string.political_views_monarchist;
            case 7:
                return R.string.political_views_ultraconservative;
            case 8:
                return R.string.political_views_apathetic;
            case 9:
                return R.string.political_views_libertian;
            default:
                return null;
        }
    }

    private static class BeliefsHolder {

        InfoLine politicalViews;
        InfoLine worldView;
        InfoLine personalPriority;
        InfoLine importantInOthers;
        InfoLine viewsOnSmoking;
        InfoLine viewsOnAlcohol;
        InfoLine inspiredBy;

        BeliefsHolder(View root) {
            politicalViews = new InfoLine(root.findViewById(R.id.political_views));
            worldView = new InfoLine(root.findViewById(R.id.world_view));
            personalPriority = new InfoLine(root.findViewById(R.id.personal_priority));
            importantInOthers = new InfoLine(root.findViewById(R.id.important_in_others));
            viewsOnSmoking = new InfoLine(root.findViewById(R.id.views_on_smoking));
            viewsOnAlcohol = new InfoLine(root.findViewById(R.id.views_on_alcohol));
            inspiredBy = new InfoLine(root.findViewById(R.id.inspired_by));
        }
    }

    */
    /*
     * ОСНОВНАЯ ИНФОРМАЦИЯ ПОЛЬЗОВАТЕЛЯ
     */
    /*
    public static void fillMainUserInfo(Context context, View sectionsView, VKApiUser apiUser, OwnerLinkSpanFactory.ActionListener onOwnerClickListener) {
        MainInfoHolder holder = new MainInfoHolder(sectionsView);

        holder.relationshipStatus.setVisible(apiUser.relation != 0);
        holder.relationshipStatus.tvText.setMovementMethod(LinkMovementMethod.getInstance());
        Integer relationshipStatusRes = getRelationStringRes(apiUser);

        if (relationshipStatusRes != null) {
            String line = context.getString(relationshipStatusRes).toLowerCase();
            if (apiUser.relation_partner != null) {
                String partnerLine = OwnerLinkSpanFactory.genOwnerLink(apiUser.relation_partner.id, apiUser.relation_partner.getFullName());
                line = line + ", " + context.getString(R.string.relationship_partner).toLowerCase() + COLON + partnerLine;
            }

            resolveSimpleInfoLine(context, holder.relationshipStatus, line, R.string.relationship_status, onOwnerClickListener);
        }

        holder.relatives.setVisible(apiUser.relatives != null && !apiUser.relatives.isEmpty());
        holder.relatives.tvText.setMovementMethod(LinkMovementMethod.getInstance());

        String relativesLine = buildRelativesLine(context, apiUser);
        resolveSimpleInfoLine(context, holder.relatives, relativesLine, R.string.relatives, onOwnerClickListener);
        //holder.relatives.tvText.setText(OwnerLinkSpanFactory.withOwnersLinks(relativesLine, onOwnerClickListener), TextView.BufferType.SPANNABLE);

        IUserActivityPoint currentActivity = findCurrentActivityPoint(apiUser);
        holder.studiedAt.setVisible(currentActivity != null);
        holder.studiedAt.tvText.setMovementMethod(LinkMovementMethod.getInstance());

        holder.studiedAt.setVisible(true);
        if (currentActivity instanceof VKApiCareer) {
            VKApiCareer career = ((VKApiCareer) currentActivity);
            String companyStr = career.group_id > 0 ? OwnerLinkSpanFactory.genOwnerLink(-career.group_id, career.company) : career.company;
            resolveSimpleInfoLine(context, holder.studiedAt, companyStr, R.string.company, onOwnerClickListener);
        } else if (currentActivity instanceof VKApiUniversity) {
            resolveSimpleInfoLine(context, holder.studiedAt, ((VKApiUniversity) currentActivity).name, R.string.studied_at);
        } else if (currentActivity instanceof VKApiSchool) {
            resolveSimpleInfoLine(context, holder.studiedAt, ((VKApiSchool) currentActivity).name, R.string.studied_at);
        } else {
            holder.studiedAt.setVisible(false);
        }

        holder.languages.setVisible(apiUser.langs != null && apiUser.langs.length != 0);
        if (apiUser.langs != null && apiUser.langs.length > 0) {
            resolveSimpleInfoLine(context, holder.languages, TextUtils.join(", ", apiUser.langs), R.string.languages);
        }

        resolveSimpleInfoLine(context, holder.city, apiUser.city == null ? null : apiUser.city.title, R.string.city);
        resolveSimpleInfoLine(context, holder.country, apiUser.country == null ? null : apiUser.country.title, R.string.country);
        resolveSimpleInfoLine(context, holder.webSite, apiUser.site, R.string.website);
        resolveSimpleInfoLine(context, holder.primaryPhone, apiUser.mobile_phone, R.string.mobile_phone_number);
        resolveSimpleInfoLine(context, holder.alternativePhone, apiUser.home_phone, R.string.alternative_phone);
        resolveSimpleInfoLine(context, holder.birthday, AppTextUtils.getDateWithZeros(apiUser.bdate), R.string.birthday);

        boolean rootVisible = holder.birthday.isVisible() ||
                holder.relationshipStatus.isVisible() ||
                holder.relatives.isVisible() ||
                holder.studiedAt.isVisible() ||
                holder.languages.isVisible() ||
                holder.city.isVisible() ||
                holder.country.isVisible() ||
                holder.webSite.isVisible() ||
                holder.primaryPhone.isVisible() ||
                holder.alternativePhone.isVisible();

        sectionsView.setVisibility(rootVisible ? View.VISIBLE : View.GONE);
    }

    */
    /**
     * Найти текущее место деятельности пользователя (школа, компания или ВУЗ)
     *
     * @return место активности
     */
    /*
    private static IUserActivityPoint findCurrentActivityPoint(VKApiUser apiUser) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        if (apiUser.careers != null && !apiUser.careers.isEmpty()) {
            for (VKApiCareer career : apiUser.careers) {
                if (career.until <= 0 || career.until > currentYear) {
                    return career;
                }
            }
        }

        if (apiUser.universities != null && !apiUser.universities.isEmpty()) {
            for (VKApiUniversity university : apiUser.universities) {
                if (university.graduation >= currentYear || university.graduation <= 0) {
                    return university;
                }
            }
        }

        if (apiUser.schools != null && !apiUser.schools.isEmpty()) {
            for (VKApiSchool school : apiUser.schools) {
                if (school.year_to >= currentYear || school.year_to <= 0) {
                    return school;
                }
            }
        }

        return null;
    }

    private static String buildRelativesLine(Context context, VKApiUser apiUser) {
        if (Utils.isEmpty(apiUser.relatives)) {
            return null;
        }

        ArrayList<VKApiUser.Relative> grandparents = null;
        ArrayList<VKApiUser.Relative> parents = null;
        ArrayList<VKApiUser.Relative> siblings = null;
        ArrayList<VKApiUser.Relative> children = null;
        ArrayList<VKApiUser.Relative> grandchildren = null;
        ArrayList<VKApiUser.Relative> others = null;

        for (VKApiUser.Relative relative : apiUser.relatives) {
            if (relative.type.equalsIgnoreCase(VKApiUser.RelativeType.SUBLING)) {
                siblings = createIfNull(siblings);
                siblings.add(relative);
            } else if (relative.type.equalsIgnoreCase(VKApiUser.RelativeType.GRANDPARENT)) {
                grandparents = createIfNull(grandparents);
                grandparents.add(relative);
            } else if (relative.type.equalsIgnoreCase(VKApiUser.RelativeType.PARENT)) {
                parents = createIfNull(parents);
                parents.add(relative);
            } else if (relative.type.equalsIgnoreCase(VKApiUser.RelativeType.CHILD)) {
                children = createIfNull(children);
                children.add(relative);
            } else if (relative.type.equalsIgnoreCase(VKApiUser.RelativeType.GRANDCHILD)) {
                grandchildren = createIfNull(grandchildren);
                grandchildren.add(relative);
            } else {
                others = createIfNull(others);
                others.add(relative);
            }
        }

        String line = "";
        if (grandparents != null) {
            line = line + context.getString(R.string.relatives_grandparents) + COLON;
            line = appendRelatives(line, grandparents);
        }

        if (parents != null) {
            line = line + context.getString(R.string.relatives_parents) + COLON;
            line = appendRelatives(line, parents);
        }

        if (siblings != null) {
            line = line + context.getString(R.string.relatives_siblings) + COLON;
            line = appendRelatives(line, siblings);
        }

        if (children != null) {
            line = line + context.getString(R.string.relatives_children) + COLON;
            line = appendRelatives(line, children);
        }

        if (grandchildren != null) {
            line = line + context.getString(R.string.relatives_grandchildren) + COLON;
            line = appendRelatives(line, grandchildren);
        }

        if (others != null) {
            line = line + context.getString(R.string.relatives_others) + COLON;
            line = appendRelatives(line, others);
        }

        line = line.substring(0, line.length() - 2); // убираем запятую в конце

        return line;
    }

    private static String appendRelatives(String line, ArrayList<VKApiUser.Relative> relatives) {
        for (VKApiUser.Relative r : relatives) {
            if (r.id > 0) {
                line = line + OwnerLinkSpanFactory.genOwnerLink(r.id, r.name) + ", ";
            } else {
                line = line + r.name + ", ";
            }
        }
        return line;
    }

    private static <T> ArrayList<T> createIfNull(ArrayList<T> original) {
        if (original == null) {
            original = new ArrayList<>();
        }
        return original;
    }

    public static class MainInfoHolder {
        InfoLine birthday;
        InfoLine relationshipStatus;
        InfoLine relatives;
        InfoLine studiedAt;
        InfoLine languages;
        InfoLine city;
        InfoLine country;
        InfoLine webSite;
        InfoLine primaryPhone;
        InfoLine alternativePhone;

        MainInfoHolder(View root) {
            birthday = new InfoLine(root.findViewById(R.id.birthday));
            relationshipStatus = new InfoLine(root.findViewById(R.id.relationship_status));
            relatives = new InfoLine(root.findViewById(R.id.relatives));
            studiedAt = new InfoLine(root.findViewById(R.id.studied_at));
            languages = new InfoLine(root.findViewById(R.id.languages));
            city = new InfoLine(root.findViewById(R.id.city));
            country = new InfoLine(root.findViewById(R.id.country));
            webSite = new InfoLine(root.findViewById(R.id.website));
            primaryPhone = new InfoLine(root.findViewById(R.id.primary_phone));
            alternativePhone = new InfoLine(root.findViewById(R.id.alternative_phone));
        }
    }

    public static Integer getRelationStringRes(VKApiUser apiUser) {
        switch (apiUser.sex) {
            case VKApiUser.SEX_MAN:
            case VKApiUser.SEX_UNKNOWN:
                switch (apiUser.relation) {
                    case VKApiUser.Relation.SINGLE:
                        return R.string.relationship_man_single;
                    case VKApiUser.Relation.RELATIONSHIP:
                        return R.string.relationship_man_in_relationship;
                    case VKApiUser.Relation.ENGAGED:
                        return R.string.relationship_man_engaged;
                    case VKApiUser.Relation.MARRIED:
                        return R.string.relationship_man_married;
                    case VKApiUser.Relation.COMPLICATED:
                        return R.string.relationship_man_its_complicated;
                    case VKApiUser.Relation.SEARCHING:
                        return R.string.relationship_man_activelly_searching;
                    case VKApiUser.Relation.IN_LOVE:
                        return R.string.relationship_man_in_love;
                    case VKApiUser.Relation.IN_A_CIVIL_UNION:
                        return R.string.in_a_civil_union;

                }
                break;
            case VKApiUser.SEX_WOMAN:
                switch (apiUser.relation) {
                    case VKApiUser.Relation.SINGLE:
                        return R.string.relationship_woman_single;
                    case VKApiUser.Relation.RELATIONSHIP:
                        return R.string.relationship_woman_in_relationship;
                    case VKApiUser.Relation.ENGAGED:
                        return R.string.relationship_woman_engaged;
                    case VKApiUser.Relation.MARRIED:
                        return R.string.relationship_woman_married;
                    case VKApiUser.Relation.COMPLICATED:
                        return R.string.relationship_woman_its_complicated;
                    case VKApiUser.Relation.SEARCHING:
                        return R.string.relationship_woman_activelly_searching;
                    case VKApiUser.Relation.IN_LOVE:
                        return R.string.relationship_woman_in_love;
                    case VKApiUser.Relation.IN_A_CIVIL_UNION:
                        return R.string.in_a_civil_union;
                }
                break;
        }

        return null;
    }



    public static String getUserActivityLine(Context context, VKApiUser apiUser){
        return getUserActivityLine(context, apiUser.last_seen, apiUser.online, apiUser.sex);
    }

    */
    fun getUserActivityLine(context: Context, user: User, force_last_seen: Boolean): String {
        return getUserActivityLine(context, user.lastSeen, user.isOnline, user.sex, force_last_seen)
    }


    fun getUserActivityLine(
        context: Context,
        lastSeen: Long,
        online: Boolean,
        sex: Int,
        force_last_seen: Boolean
    ): String {
        if (!online && lastSeen == 0L) {
            return context.getString(R.string.offline_hidden)
        }
        var activityText: String =
            if (online) context.getString(R.string.online) else context.getString(R.string.offline)
        if (!online || force_last_seen) {
            val activityTime = getDateFromUnixTimeShorted(context, lastSeen)
            activityText += when (sex) {
                VKApiUser.SEX_MAN -> {
                    ", " + context.getString(R.string.last_seen_sex_man, activityTime)
                }
                VKApiUser.SEX_WOMAN -> {
                    ", " + context.getString(R.string.last_seen_sex_woman, activityTime)
                }
                else -> {
                    ", " + context.getString(R.string.last_seen_sex_unknown, activityTime)
                }
            }
        }
        return activityText
    }
}