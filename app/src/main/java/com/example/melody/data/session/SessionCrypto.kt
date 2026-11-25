package com.example.melody.data.session

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionCrypto(
    private val keyProvider: AuthSessionKeyProvider,
) {
    fun encrypt(plaintext: ByteArray): ByteArray = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateKey())
        cipher.updateAAD(ASSOCIATED_DATA)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = requireNotNull(cipher.iv) { "AES-GCM did not provide an initialization vector." }
        require(iv.size == GCM_IV_LENGTH_BYTES) { "Unexpected AES-GCM initialization vector length." }
        ByteBuffer.allocate(1 + iv.size + ciphertext.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
    } catch (exception: Exception) {
        throw SessionCryptoException("Could not encrypt the local session.", exception)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray = try {
        val buffer = ByteBuffer.wrap(encryptedData)
        require(buffer.remaining() > 1) { "Encrypted session is incomplete." }
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength == GCM_IV_LENGTH_BYTES && buffer.remaining() > ivLength + GCM_TAG_LENGTH_BYTES) {
            "Encrypted session envelope is invalid."
        }
        val iv = ByteArray(ivLength).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val key: SecretKey = keyProvider.getExistingKey()
            ?: throw SessionCryptoException("Android Keystore session key is unavailable.")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(ASSOCIATED_DATA)
        cipher.doFinal(ciphertext)
    } catch (exception: SessionCryptoException) {
        throw exception
    } catch (exception: Exception) {
        throw SessionCryptoException("Could not decrypt the local session.", exception)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
        val ASSOCIATED_DATA = "melody:auth-session:v1".toByteArray(Charsets.UTF_8)
    }
}

class SessionCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)
