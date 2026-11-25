package com.example.melody.data.session

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedAuthSessionStorage(
    private val sessionFile: File,
    private val codec: AuthSessionCodec,
) : AuthSessionStorage {
    override suspend fun read(): AuthSession? = withContext(Dispatchers.IO) {
        if (!sessionFile.exists()) return@withContext null
        try {
            codec.decode(sessionFile.readBytes())
        } catch (exception: SessionDataCorruptionException) {
            throw exception
        } catch (exception: IOException) {
            throw SessionStorageException(exception)
        }
    }

    override suspend fun write(session: AuthSession) = withContext(Dispatchers.IO) {
        val directory = sessionFile.parentFile ?: throw SessionStorageException(IllegalStateException("Missing storage directory."))
        if (!directory.exists() && !directory.mkdirs()) {
            throw SessionStorageException(IOException("Could not create internal session directory."))
        }

        val temporaryFile = File(directory, "${sessionFile.name}.tmp")
        try {
            val encryptedData = codec.encode(session)
            FileOutputStream(temporaryFile).use { output ->
                output.write(encryptedData)
                output.fd.sync()
            }
            if (!temporaryFile.renameTo(sessionFile)) {
                throw IOException("Could not atomically replace encrypted session file.")
            }
        } catch (exception: SessionCryptoException) {
            throw SessionStorageException(exception)
        } catch (exception: IOException) {
            throw SessionStorageException(exception)
        } finally {
            if (temporaryFile.exists()) temporaryFile.delete()
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        val temporaryFile = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
        if (sessionFile.exists() && !sessionFile.delete()) {
            throw SessionStorageException(IOException("Could not remove encrypted session file."))
        }
        if (temporaryFile.exists() && !temporaryFile.delete()) {
            throw SessionStorageException(IOException("Could not remove temporary session file."))
        }
    }
}
