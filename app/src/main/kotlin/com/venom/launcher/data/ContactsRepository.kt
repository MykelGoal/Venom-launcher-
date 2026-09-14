package com.venom.launcher.data

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A contact match from the drawer's universal search. */
data class ContactHit(
    val contactId: Long,
    val name: String,
    val number: String,
)

/**
 * Tiny read-only contact lookup for the drawer search.
 *
 * Only ever called when the user has actually granted READ_CONTACTS — the
 * drawer checks the permission before it asks for anything.
 */
object ContactsRepository {

    private val PROJECTION = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )

    suspend fun search(context: Context, query: String, limit: Int = 6): List<ContactHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()

            val out = LinkedHashMap<Long, ContactHit>()
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PROJECTION,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$q%"),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { c ->
                    val idCol =
                        c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol =
                        c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol =
                        c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (c.moveToNext() && out.size < limit) {
                        val id = c.getLong(idCol)
                        if (out.containsKey(id)) continue
                        val name = c.getString(nameCol) ?: continue
                        out[id] = ContactHit(id, name, c.getString(numCol).orEmpty())
                    }
                }
            }
            out.values.toList()
        }
}
