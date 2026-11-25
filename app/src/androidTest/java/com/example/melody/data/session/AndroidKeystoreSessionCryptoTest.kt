package com.example.melody.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSessionCryptoTest {
    @Test
    fun androidKeystoreAesGcmKeyEncryptsAndDecryptsSessionData() {
        val provider = AndroidKeystoreSessionKeyProvider()
        val crypto = SessionCrypto(provider)
        val plaintext = "emulator-keystore-session-test".toByteArray(Charsets.UTF_8)

        val encrypted = crypto.encrypt(plaintext)

        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains("emulator-keystore-session-test"))
        assertEquals(plaintext.toList(), crypto.decrypt(encrypted).toList())
        assertNotNull(provider.getExistingKey())
        assertEquals(
            plaintext.toList(),
            SessionCrypto(AndroidKeystoreSessionKeyProvider()).decrypt(encrypted).toList(),
        )
    }

    @Test
    fun ciphertextCannotBeReadAfterAuthenticationTagChanges() {
        val crypto = SessionCrypto(AndroidKeystoreSessionKeyProvider())
        val encrypted = crypto.encrypt("session payload".toByteArray(Charsets.UTF_8))
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        try {
            crypto.decrypt(encrypted)
            throw AssertionError("Tampered ciphertext must not decrypt.")
        } catch (_: SessionCryptoException) {
            // AES-GCM rejects a modified authentication tag.
        }
    }
}
