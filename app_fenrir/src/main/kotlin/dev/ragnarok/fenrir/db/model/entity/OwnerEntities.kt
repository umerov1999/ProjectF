package dev.ragnarok.fenrir.db.model.entity

class OwnerEntities(
    val userEntities: List<UserEntity>,
    val communityEntities: List<CommunityEntity>
) {
    fun size(): Int {
        return userEntities.size + communityEntities.size
    }
}