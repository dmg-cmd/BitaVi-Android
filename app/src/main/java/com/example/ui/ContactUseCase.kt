package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

class ContactUseCase(private val context: Context) {

    data class ContactDetails(
        val name: String,
        val phone: String,
        val email: String,
        val address: String
    )

    suspend fun getContactDetails(contactUri: Uri): ContactDetails {
        var name = ""
        var phone = ""
        var email = ""
        var address = ""

        val contentResolver = context.contentResolver
        contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID))
                name = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME))

                if (cursor.getInt(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            phone = pCursor.getString(pCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }
                }

                contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { eCursor ->
                    if (eCursor.moveToFirst()) {
                        email = eCursor.getString(eCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS))
                    }
                }

                contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                    null,
                    "${android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { aCursor ->
                    if (aCursor.moveToFirst()) {
                        address = aCursor.getString(aCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
                    }
                }
            }
        }
        return ContactDetails(name, phone, email, address)
    }

    fun getAllContactPhones(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                val phone = if (phoneIdx >= 0) cursor.getString(phoneIdx).orEmpty() else ""
                if (phone.isNotBlank()) result.add(name to phone)
            }
        }
        return result
    }
}
