package com.shary.app.infrastructure.di


import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.FieldList
import com.shary.app.RequestList
import com.shary.app.UserList
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.security.Kdf
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.cipher.AesGcmCipher
import com.shary.app.infrastructure.security.hkdf.HkdfSha256
import com.shary.app.infrastructure.security.kdf.Pbkdf2Kdf
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.persistance.datastore.fieldListDataStore
import com.shary.app.infrastructure.repositories.FieldRepositoryImpl
import com.shary.app.infrastructure.repositories.RequestRepositoryImpl
import com.shary.app.infrastructure.repositories.TagRepositoryImpl
import com.shary.app.infrastructure.repositories.UserRepositoryImpl
import com.shary.app.infrastructure.services.cache.SelectionCacheService
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import com.shary.app.infrastructure.services.email.EmailServiceImpl
import com.shary.app.infrastructure.services.file.FileServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    ): UserRepositoryImpl = UserRepositoryImpl(datastore)

    @Provides @Singleton fun provideRequestRepository(
        datastore: DataStore<RequestList>,
        codec: FieldCodec
    ): RequestRepositoryImpl = RequestRepositoryImpl(datastore, codec)

    @Provides @Singleton fun provideTagRepository(
        @ApplicationContext context: Context
    ): TagRepositoryImpl = TagRepositoryImpl(context)


    // ======== Services ========

    @Provides @Singleton
    fun provideEmailService(
        @ApplicationContext context: Context,
        session: Session
    ): EmailServiceImpl = EmailServiceImpl(context, session)

    @Provides @Singleton fun provideCloudService(
        session: Session,
        crypto: CryptographyManager
    ): CloudServiceImpl = CloudServiceImpl(session, crypto)

    @Provides @Singleton fun provideFileService(
        @ApplicationContext context: Context
    ): FileServiceImpl =
        FileServiceImpl(context)

    @Provides @Singleton fun provideSelectionCacheService(
    ): CacheService = SelectionCacheService()


    // ======== Session ========

    @Provides @Singleton
    fun provideSession(
        selection: CacheService,
        auth: AuthService
    ): Session = Session(selection, auth)

}
