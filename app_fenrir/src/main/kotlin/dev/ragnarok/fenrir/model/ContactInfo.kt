package dev.ragnarok.fenrir.model

class ContactInfo(private val userId: Int) {
    private var description: String? = null
    private var phone: String? = null
    private var email: String? = null
    fun getUserId(): Int {
        return userId
    }

    fun getEmail(): String? {
        return email
    }

    fun setEmail(email: String?): ContactInfo {
        this.email = email
        return this
    }

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?): ContactInfo {
        this.description = description
        return this
    }

    fun getPhone(): String? {
        return phone
    }

    fun setPhone(phone: String?): ContactInfo {
        this.phone = phone
        return this
    }
}