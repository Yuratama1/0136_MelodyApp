package com.example.melody.core.di

import android.content.Context
import com.example.melody.data.session.AndroidKeystoreSessionKeyProvider
import com.example.melody.data.session.AuthSessionManager
import com.example.melody.data.session.AuthSessionManagerImpl
import com.example.melody.data.session.AuthSessionCodec
import com.example.melody.data.session.AuthSessionStorage
import com.example.melody.data.session.AuthSessionKeyProvider
import com.example.melody.data.session.EncryptedAuthSessionStorage
import com.example.melody.data.session.EpochClock
import com.example.melody.data.session.SessionCrypto
import com.example.melody.data.session.SystemEpochClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthSessionBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSessionManager(manager: AuthSessionManagerImpl): AuthSessionManager

    @Binds
    @Singleton
    abstract fun bindSessionKeyProvider(provider: AndroidKeystoreSessionKeyProvider): AuthSessionKeyProvider

    @Binds
    @Singleton
    abstract fun bindEpochClock(clock: SystemEpochClock): EpochClock
}

@Module
@InstallIn(SingletonComponent::class)
object AuthSessionProvidesModule {
    @Provides
    @Singleton
    fun provideSessionCrypto(keyProvider: AuthSessionKeyProvider): SessionCrypto = SessionCrypto(keyProvider)

    @Provides
    @Singleton
    fun provideAuthSessionStorage(
        @ApplicationContext context: Context,
        codec: AuthSessionCodec,
    ): AuthSessionStorage = EncryptedAuthSessionStorage(
        sessionFile = File(context.noBackupFilesDir, AUTH_SESSION_FILE_NAME),
        codec = codec,
    )

    private const val AUTH_SESSION_FILE_NAME = "auth-session.enc"
}
