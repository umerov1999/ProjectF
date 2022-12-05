package dev.ragnarok.fenrir.link.types

class CatalogV2SectionLink @JvmOverloads constructor(val section: String? = null) :
    AbsLink(CATALOG_V2_SECTION_LINK) {
    override fun toString(): String {
        return "CatalogV2SectionLink{" +
                "section='" + section + '\'' +
                '}'
    }
}