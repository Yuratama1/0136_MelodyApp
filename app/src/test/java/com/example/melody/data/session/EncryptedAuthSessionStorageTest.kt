package com.example.melody.data.session

import java.io.File
import java.nio.file.Files
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedAuthSessionStorageTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val keyProvider = TestKeyProvider()
    private val codec = AuthSessionCodec(SessionCrypto(keyProvider), Json)

    @Test
    fun storesOnlyCiphertextAndRestoresAcrossStorageInstances() = runBlocking {
        val sessionFile = File(temporaryFolder.root, "auth-session.enc")
        val storage = EncryptedAuthSessionStorage(sessionFile, codec)
        val session = testSession()

        storage.write(session)

        val contents = sessionFile.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(contents.contains(session.accessToken))
        assertFalse(contents.contains(session.refreshToken))
        assertEquals(session, EncryptedAuthSessionStorage(sessionFile, codec).read())
        assertFalse(File(temporaryFolder.root, "auth-session.enc.tmp").exists())
    }

    @Test
    fun atomicallyReplacesAndClearsStoredSession() = runBlocking {
        val sessionFile = File(temporaryFolder.root, "auth-session.enc")
        val storage = EncryptedAuthSessionStorage(sessionFile, codec)

        storage.write(testSession())
        val updated = testSession().copy(accessToken = "access-new")
        storage.write(updated)
        assertEquals(updated, storage.read())

        storage.clear()
        assertNull(storage.read())
    }

    @Test
    fun corruptedCiphertextIsReportedForSafeRestorationHandling() = runBlocking {
        val sessionFile = File(temporaryFolder.root, "auth-session.enc")
        val storage = EncryptedAuthSessionStorage(sessionFile, codec)
        storage.write(testSession())
        val bytes = sessionFile.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        sessionFile.writeBytes(bytes)

        assertThrows(SessionDataCorruptionException::class.java) {
            runBlocking { storage.read() }
        }
        Unit
    }

    private fun testSession() = AuthSession(
        accessToken = "access-test-secret",
        refreshToken = "refresh-test-secret",
        tokenType = "bearer",
        userId = "user-test",
        email = "user@example.com",
        displayName = "Melody",
        expiresAtEpochSeconds = 1_800_000_600,
    )

    private class TestKeyProvider : AuthSessionKeyProvider {
        private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
        override fun getOrCreateKey(): SecretKey = key
        override fun getExistingKey(): SecretKey = key
    }
}
