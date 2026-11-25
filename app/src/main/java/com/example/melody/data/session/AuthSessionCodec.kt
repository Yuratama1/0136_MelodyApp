package com.example.melody.data.session

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthSessionCodec @Inject constructor(
    private val crypto: SessionCrypto,
    private val json: Json,
) {
    fun encode(session: AuthSession): ByteArray =
        crypto.encrypt(json.encodeToString(session).toByteArray(Charsets.UTF_8))

    fun decode(encryptedData: ByteArray): AuthSession = try {
        json.decodeFromString(crypto.decrypt(encryptedData).toString(Charsets.UTF_8))
    } catch (exception: SessionCryptoException) {
        throw SessionDataCorruptionException(exception)
    } catch (exception: SerializationException) {
        throw SessionDataCorruptionException(exception)
    } catch (exception: IllegalArgumentException) {
        throw SessionDataCorruptionException(exception)
    }
}

class SessionDataCorruptionException(cause: Throwable) : Exception("Stored session cannot be restored safely.", cause)
class SessionStorageException(cause: Throwable) : Exception("Local session storage failed.", cause)
