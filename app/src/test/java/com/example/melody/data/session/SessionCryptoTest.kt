package com.example.melody.data.session

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionCryptoTest {
    private val keyProvider = TestKeyProvider()
    private val crypto = SessionCrypto(keyProvider)

    @Test
    fun aesGcmEncryptsAndDecryptsWithRandomIv() {
        val plaintext = "token material for test only".toByteArray()

        val encryptedFirst = crypto.encrypt(plaintext)
        val encryptedSecond = crypto.encrypt(plaintext)

        assertFalse(encryptedFirst.contentEquals(plaintext))
        assertNotEquals(encryptedFirst.toList(), encryptedSecond.toList())
        assertArrayEquals(plaintext, crypto.decrypt(encryptedFirst))
    }

    @Test
    fun tamperedCiphertextAndMissingKeystoreKeyAreRejected() {
        val encrypted = crypto.encrypt("test session".toByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(SessionCryptoException::class.java) { crypto.decrypt(encrypted) }

        val encryptedWithKey = crypto.encrypt("value".toByteArray())
        keyProvider.key = null
        assertThrows(SessionCryptoException::class.java) { crypto.decrypt(encryptedWithKey) }
    }

    private class TestKeyProvider : AuthSessionKeyProvider {
        var key: SecretKey? = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

        override fun getOrCreateKey(): SecretKey = requireNotNull(key)

        override fun getExistingKey(): SecretKey? = key
    }
}
