package com.shary.app.infrastructure.di


import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.FieldList
import com.shary.app.RequestList
import com.shary.app.UserList
import com.shary.app.application.security.LoginUseCase
import com.shary.app.application.security.RegisterUseCase
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.ports.AuthPort
import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import com.shary.app.core.domain.interfaces.ports.ConnectivityPort
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.security.Kdf
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.cipher.AesGcmCipher
import com.shary.app.infrastructure.security.hkdf.HkdfSha256
import com.shary.app.infrastructure.security.kdf.Pbkdf2Kdf
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.core.domain.interfaces.security.Ed25519Factory
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.interfaces.services.FileService
import com.shary.app.core.domain.security.CredentialsProvider
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.persistance.credentials.FileCredentialsStore
import com.shary.app.infrastructure.persistance.datastore.fieldListDataStore
import com.shary.app.infrastructure.repositories.FieldRepositoryImpl
import com.shary.app.infrastructure.repositories.RequestRepositoryImpl
import com.shary.app.infrastructure.repositories.TagRepositoryImpl
import com.shary.app.infrastructure.repositories.UserRepositoryImpl
import com.shary.app.infrastructure.security.auth.AuthServiceImpl
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.infrastructure.security.local.FieldCodecVault
import com.shary.app.infrastructure.security.local.InMemoryCredentialsProvider
import com.shary.app.infrastructure.security.local.LocalVault
import com.shary.app.infrastructure.security.manager.CryptographyManagerImpl
import com.shary.app.infrastructure.security.sign.BcEd25519Factory
import com.shary.app.infrastructure.security.sign.Ed25519DetachedVerifier
import com.shary.app.infrastructure.services.cache.SelectionCacheService
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import com.shary.app.infrastructure.services.connectivity.ConnectivityPortImpl
import com.shary.app.infrastructure.services.email.EmailServiceImpl
import com.shary.app.infrastructure.services.file.FileServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DependenciesContainer {

    // ======== Repositories ========

    @Provides @Singleton
    fun provideFieldListDataStore(
        @ApplicationContext context: Context
    ): DataStore<FieldList> = context.fieldListDataStore

    @Provides @Singleton
    fun provideFieldRepository(
        datastore: DataStore<FieldList>,
        codec: FieldCodec
    ): FieldRepository = FieldRepositoryImpl(datastore, codec)

    @Provides @Singleton fun provideUserRepository(
        datastore: DataStore<UserList>,
        //codec: FieldCodec
    ): UserRepository = UserRepositoryImpl(datastore)

    @Provides @Singleton fun provideRequestRepository(
        datastore: DataStore<RequestList>,
        codec: FieldCodec
    ): RequestRepository = RequestRepositoryImpl(datastore, codec)

    @Provides @Singleton fun provideTagRepository(
        @ApplicationContext context: Context
    ): TagRepository = TagRepositoryImpl(context)


    // ======== Services ========

    @Provides @Singleton
    fun provideEmailService(
        @ApplicationContext context: Context,
        session: Session
    ): EmailService = EmailServiceImpl(context, session)

    @Provides @Singleton fun provideCloudService(
        session: Session,
        crypto: CryptographyManager
    ): CloudService = CloudServiceImpl(session, crypto)

    @Provides @Singleton fun provideFileService(
        @ApplicationContext context: Context
    ): FileService = FileServiceImpl(context)

    @Provides @Singleton fun provideSelectionCacheService(
    ): CacheService = SelectionCacheService()


    // ======== Session ========

    @Provides @Singleton
    fun provideSession(
        selection: CacheService,
        auth: AuthService
    ): Session = Session(selection, auth)


    @Provides @Singleton
    fun provideConnectivity(
        cloudService: CloudService,
        statusDataSource: CloudStatusDataSource
    ): ConnectivityPort = ConnectivityPortImpl(
        cloudService,
        statusDataSource,
        providePingPeriodMillis()
    )

    @Provides
    //fun providePingPeriod(): Duration = 30.seconds
    fun providePingPeriodMillis(): Long = 30_000L   // 30s en ms, evita kotlin.time

    @Provides @Singleton fun provideRegisterUC(
        crypto: CryptographyManager,
        auth: AuthService
    ) = RegisterUseCase(crypto, auth)


    @Provides @Singleton fun provideLoginUC(
        crypto: CryptographyManager,
        credentialsStore: CredentialsStore
    ) = LoginUseCase(crypto, AuthServiceImpl(crypto, credentialsStore))


    @Provides @Singleton fun provideFileCredentialsStore(
        @ApplicationContext context: Context
    ): CredentialsStore = FileCredentialsStore(context)


    @Provides @Singleton fun provideAuthService(
        crypto: CryptographyManager,
        fileCredentialsStore: FileCredentialsStore
    ): AuthService = AuthServiceImpl(crypto, fileCredentialsStore)

    @Provides @Singleton
    fun provideConnectivityPort(
        cloudService: CloudService,
        statusDataSource: CloudStatusDataSource,
        pingPeriodMillis: Long
    ): ConnectivityPort =
        ConnectivityPortImpl(cloudService, statusDataSource, pingPeriodMillis)

    /*
    @Provides @Singleton
    fun provideAuthPort(
        authPort: AuthPort
    ): AuthPort =
    */
    
    @Provides @Singleton fun provideAesGcmBox(
        hkdf: HkdfSha256,
        cipher: AesGcmCipher
    ): AesGcmBox = AesGcmBox(hkdf, cipher)

    @Provides @Singleton fun provideKeyDerivation(
        kdf: Kdf,
        hkdf: HkdfSha256
    ): KeyDerivation = KeyDerivation(kdf, hkdf)

    @Provides @Singleton fun provideAppId(
    ): String = "com.shary.app"
    @Provides @Singleton fun provideKdf(
    ): Kdf = Pbkdf2Kdf()
    @Provides @Singleton fun provideHkdf(
    ): HkdfSha256 = HkdfSha256()
    @Provides @Singleton fun provideCipher(
    ): AesGcmCipher = AesGcmCipher()
    @Provides @Singleton fun provideEd25519Factory(
    ): Ed25519Factory = BcEd25519Factory()
    @Provides @Singleton fun provideContext(
        @ApplicationContext context: Context
    ): Context = context
    @Provides @Singleton fun provideFieldCodec(
        localVault: LocalVault,
        credentials: CredentialsProvider
    ): FieldCodec = FieldCodecVault(localVault, credentials)
    @Provides @Singleton fun provideCredentialsProvider(
    ): CredentialsProvider = InMemoryCredentialsProvider()

    @Provides @Singleton fun provideLocalVault(
        kd: KeyDerivation,
        cipher: AesGcmCipher
    ): LocalVault = LocalVault(kd, cipher)

    @Provides @Singleton fun provideCryptographyManager(
        kd: KeyDerivation,
        box: AesGcmBox,
        cipher: AesGcmCipher,
        factory: Ed25519Factory,
        localVault: LocalVault,
        verifier: DetachedVerifier,
        @ApplicationContext context: Context
    ): CryptographyManager =
        CryptographyManagerImpl(kd, box, cipher, factory, localVault, verifier, context)

    @Provides @Singleton fun provideDetachedVerifier(
    ): DetachedVerifier = Ed25519DetachedVerifier()

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ConnectivityPingPeriod

}
