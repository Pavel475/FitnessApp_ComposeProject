package com.example.fitnessapp_composeproject.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PinVault(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "FitnessAppKeyStoreAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Создание ключа шифрования в AndroidKeyStore, требующего аутентификации (биометрия/PIN устройства)
     */
    fun getOrCreateKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            // Ключ остается открытым 30 секунд после успешной аутентификации
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        30,
                        KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
            }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Создание Cipher для BiometricPrompt (CryptoObject)
     */
    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return cipher
    }

    /**
     * Шифрование refresh-токена с помощью KeyStore ключа
     */
    val encryptedBlobFile = java.io.File(context.filesDir, "secure_refresh.bin")

    fun saveEncryptedRefresh(cipher: Cipher, refreshToken: String): Result<Unit> = runCatching {
        val ciphertext = cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        // Сохраняем IV (12 байт) + Зашифрованные данные
        val combined = iv + ciphertext
        encryptedBlobFile.writeBytes(combined)
    }

    fun decryptRefresh(biometricCipher: Cipher): Result<String> = runCatching {
        val combined = encryptedBlobFile.readBytes()
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        // Используем переданный от BiometricPrompt / KeyStore готовый cipher
        val decryptedBytes = biometricCipher.doFinal(ciphertext)
        String(decryptedBytes, Charsets.UTF_8)
    }

    // --- Локальный PIN-код (хранится только хэш с солью, сам PIN никогда не сохраняется) ---
    private val pinPrefs = context.getSharedPreferences("secure_pin_prefs", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = pinPrefs.contains("pin_hash")

    fun savePin(pin: String) {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = hashPin(pin, salt)
        pinPrefs.edit()
            .putString("pin_hash", hash)
            .putString("pin_salt", android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .apply()
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = pinPrefs.getString("pin_hash", null) ?: return false
        val saltStr = pinPrefs.getString("pin_salt", null) ?: return false
        val salt = android.util.Base64.decode(saltStr, android.util.Base64.DEFAULT)
        val inputHash = hashPin(inputPin, salt)
        return storedHash == inputHash
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}