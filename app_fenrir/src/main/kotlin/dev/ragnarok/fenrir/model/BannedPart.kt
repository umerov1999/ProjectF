package dev.ragnarok.fenrir.model

class BannedPart(val owners: List<Owner>) {
    fun getTotalCount(): Int {
        return owners.size
    }
}