package dev.ragnarok.fenrir.model

class ContactInfo(val userId: Long) {
    var description: String? = null
        private set
    var phone: String? = null
        private set
    var email: String? = null
        private set

    fun setEmail(email: String?): ContactInfo {
        this.email = email
        return this
    }

    fun setDescription(description: String?): ContactInfo {
        this.description = description
        return this
    }

    fun setPhone(phone: String?): ContactInfo {
        this.phone = phone
        return this
    }
}