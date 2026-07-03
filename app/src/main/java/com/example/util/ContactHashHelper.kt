package com.example.util

import java.security.MessageDigest

fun ByteArray.matches(other: ByteArray): Boolean {
    if (size != other.size) return false
    var result = 0
    for (i in indices) result = result or (this[i].toInt() xor other[i].toInt())
    return result == 0
}

object ContactHashHelper {

    private const val HEX_CHARS = "0123456789ABCDEF"

    fun hashPhoneNumber(phone: String): ByteArray {
        val normalized = phone.filter { it.isDigit() }
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.copyOf(AppConstants.Ble.HASH_BYTES)
    }

    fun hashToHex(hash: ByteArray): String {
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            hex.append(HEX_CHARS[(b.toInt() shr 4) and 0xF])
            hex.append(HEX_CHARS[b.toInt() and 0xF])
        }
        return hex.toString()
    }

}

