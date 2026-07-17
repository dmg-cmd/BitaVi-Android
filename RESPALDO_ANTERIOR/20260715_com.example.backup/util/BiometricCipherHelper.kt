package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Clase utilitaria para gestionar cifrado simétrico AES/GCM/NoPadding
 * vinculado estrictamente al hardware biométrico de Android Keystore.
 */
object BiometricCipherHelper {

    private const val KEY_NAME = "biometric_encryption_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

    /**
     * Obtiene o genera la clave simétrica en el Keystore.
     * setUserAuthenticationRequired(true) obliga a que se use Biometría para acceder a ella.
     */
    @Suppress("DEPRECATION")
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true) // Requisito crítico para biometría real
            .setUserAuthenticationValidityDurationSeconds(5) // Ventana de gracia para evitar re-autenticación inmediata
            .setInvalidatedByBiometricEnrollment(true) // Seguridad adicional si cambian huellas
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Inicializa un Cipher para ENCRIPTAR. 
     * Nota: En este paso, si la clave requiere autenticación, podría lanzar una excepción
     * si no se hace dentro de un flujo biométrico, pero usualmente el cifrado se permite
     * sin auth inmediata dependiendo del nivel de API y configuración.
     */
    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher
    }

    /**
     * Inicializa un Cipher para DESENCRIPTAR usando el vector de inicialización (IV).
     */
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher
    }

    /**
     * Utilidad para convertir datos cifrados a Strings legibles para almacenamiento.
     */
    fun encryptText(plaintext: String, cipher: Cipher): Pair<String, String> {
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.DEFAULT) to Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    /**
     * Descifra el texto una vez que el Cipher ha sido autenticado mediante CryptoObject.
     */
    fun decryptText(cipher: Cipher, encryptedText: String): String {
        val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        return String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
    }

    /**
     * Elimina la clave maestra del Keystore para forzar su recreación.
     * Necesario cuando KeyPermanentlyInvalidatedException indica que la
     * configuración biométrica del dispositivo cambió (nuevas huellas, PIN, etc.)
     */
    fun regenerateMasterKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.deleteEntry(KEY_NAME)
    }
}
