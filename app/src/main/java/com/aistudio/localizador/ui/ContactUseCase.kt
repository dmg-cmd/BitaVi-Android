package com.aistudio.localizador.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class ContactDetails(
        val name: String,
        val phone: String,
        val email: String,
        val address: String
    )

    suspend fun getContactDetails(contactUri: Uri): Result<ContactDetails> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex("display_name")
                    val phoneIdx = cursor.getColumnIndex("data1")
                    val emailIdx = cursor.getColumnIndex("data1")
                    val addrIdx = cursor.getColumnIndex("data1")

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val phone = if (phoneIdx >= 0) cursor.getString(phoneIdx).orEmpty() else ""
                    val email = if (emailIdx >= 0) cursor.getString(emailIdx).orEmpty() else ""
                    val address = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else ""

                    Result.success(ContactDetails(name, phone, email, address))
                } else {
                    Result.failure(IllegalArgumentException("Cursor vacío para la URI provista"))
                }
            } ?: Result.failure(IllegalArgumentException("ContentResolver devolvió cursor nulo"))
        } catch (e: Exception) {
            Log.e("BitaVi_Contact", "Fallo al consultar ContentProvider", e)
            Result.failure(e)
        }
    }

    fun getAllContactPhones(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val contentResolver = context.contentResolver
        val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                val phone = if (phoneIdx >= 0) cursor.getString(phoneIdx).orEmpty() else ""
                if (phone.isNotBlank()) result.add(name to phone)
            }
        }
        return result
    }
}
