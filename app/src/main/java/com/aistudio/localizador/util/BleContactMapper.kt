package com.aistudio.localizador.util

class BleContactMapper {

    private val hexToName = mutableMapOf<String, String>()

    fun rebuild(contacts: List<Pair<String, String>>) {
        hexToName.clear()
        for ((name, phone) in contacts) {
            val hash = ContactHashHelper.hashPhoneNumber(phone)
            val key = ContactHashHelper.hashToHex(hash)
            hexToName[key] = name
        }
    }

    fun lookup(hash: ByteArray): String? {
        val key = ContactHashHelper.hashToHex(hash)
        return hexToName[key]
    }

    val knownHexHashes: Set<String> get() = hexToName.keys
}
