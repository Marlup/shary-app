package com.shary.app.infrastructure.di


import android.content.Context
import androidx.datastore.core.DataStore
import com.shary.app.FieldList
import com.shary.app.RequestList
import com.shary.app.User
import com.shary.app.UserList
import com.shary.app.core.domain.interfaces.repositories.FieldRepository
import com.shary.app.core.domain.interfaces.repositories.RequestRepository
import com.shary.app.core.domain.interfaces.repositories.TagRepository
import com.shary.app.core.domain.interfaces.repositories.UserRepository
import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.interfaces.services.CacheService
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.interfaces.services.FileService
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.persistance.datastore.fieldListDataStore
import com.shary.app.infrastructure.persistance.datastore.requestListDataStore
import com.shary.app.infrastructure.persistance.datastore.userListDataStore
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
    fun provideUserListDataStore(
        @ApplicationContext context: Context
    ): DataStore<UserList> = context.userListDataStore


    @Provides @Singleton
    fun provideRequestListDataStore(
        @ApplicationContext context: Context
    ): DataStore<RequestList> = context.requestListDataStore

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
    ): FileService =
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
