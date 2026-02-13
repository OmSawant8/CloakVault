package com.example.cloakvault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val TRANSFORMATION = "AES/GCM/NoPadding"

    // --- HELPER: GET OR CREATE KEY ---
    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getKey("secret_vault_key", null) as? SecretKey
        return existingKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                "secret_vault_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    // --- TEXT ENCRYPTION (FOR NOTES) ---
    fun encrypt(data: String): String? {
        return try {
            val bytes = data.toByteArray()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(bytes)

            val ivString = android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)
            val dataString = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)

            "$ivString:$dataString" // Format: IV:Data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decrypt(encryptedData: String): String? {
        return try {
            val parts = encryptedData.split(":")
            if (parts.size != 2) return null

            val iv = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
            val encryptedBytes = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decodedBytes = cipher.doFinal(encryptedBytes)
            String(decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- PHOTO ENCRYPTION (FOR IMAGES) ---
    fun encryptToBytes(bytes: ByteArray): Pair<ByteArray, ByteArray>? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(bytes)

            Pair(iv, encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptFromBytes(iv: ByteArray, encryptedBytes: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}