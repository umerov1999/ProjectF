package dev.ragnarok.fenrir.db.impl

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.ContactsContract
import androidx.annotation.Keep
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlin.math.abs

object ContactsUtils {
    var projection = arrayOf(
        BaseColumns._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.STARRED
    )

    fun stripExceptNumbers(str: String?, includePlus: Boolean): String? {
        if (str == null) {
            return null
        }
        val res = StringBuilder(str)
        var phoneChars = "0123456789"
        if (includePlus) {
            phoneChars += "+"
        }
        for (i in res.length - 1 downTo 0) {
            if (!phoneChars.contains(res.substring(i, i + 1))) {
                res.deleteCharAt(i)
            }
        }
        return res.toString()
    }

    fun stripExceptNumbers(str: String?): String? {
        return stripExceptNumbers(str, false)
    }

    @Keep
    @Serializable
    class ContactData {
        @Suppress("UNUSED")
        constructor()
        constructor(cursor: Cursor) {
            contact_id = cursor.getLong(BaseColumns._ID)
            device_local_id = abs(cursor.getString(ContactsContract.Contacts.LOOKUP_KEY).hashCode())
            name = cursor.getString(ContactsContract.Contacts.DISPLAY_NAME)
            is_favorite = cursor.getBoolean(ContactsContract.Contacts.STARRED)
        }

        @Transient
        var contact_id: Long = 0

        @SerialName("device_local_id")
        var device_local_id: Int = 0

        @SerialName("name")
        var name: String? = null

        @SerialName("is_favorite")
        var is_favorite: Boolean = false

        @SerialName("phones")
        var phones: ArrayList<String> = ArrayList()

        @SerialName("emails")
        var emails: ArrayList<String> = ArrayList()

        fun findMails(cr: ContentResolver) {
            val ce: Cursor? = cr.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contact_id.toString()),
                null
            )
            while (ce?.moveToNext() == true) {
                ce.getString(ContactsContract.CommonDataKinds.Email.DATA)?.let { emails.add(it) }
            }
            ce?.close()
        }

        fun check(): Boolean {
            return phones.nonNullNoEmpty() || emails.nonNullNoEmpty()
        }

        fun findPhones(cr: ContentResolver) {
            val ce: Cursor? = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contact_id.toString()),
                null
            )
            while (ce?.moveToNext() == true) {
                stripExceptNumbers(ce.getString(ContactsContract.CommonDataKinds.Phone.NUMBER))?.let {
                    phones.add(
                        it
                    )
                }
            }
            ce?.close()
        }
    }

    fun getAllContactsJson(context: Context): Single<String> {
        return Single.create {
            val contacts: MutableList<ContactData> = ArrayList()
            val cr = context.contentResolver
            val cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            if (cursor == null) {
                it.tryOnError(Throwable("Can't collect contact list!"))
                return@create
            }
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val stmp = ContactData(cursor)
                    stmp.findPhones(cr)
                    stmp.findMails(cr)
                    if (stmp.check()) {
                        contacts.add(stmp)
                    }
                }
                cursor.close()
            }
            if (contacts.isEmpty()) {
                it.tryOnError(Throwable("Can't collect contact list!"))
            }
            it.onSuccess(
                kJson.encodeToString(ListSerializer(ContactData.serializer()), contacts)
            )
        }
    }
}