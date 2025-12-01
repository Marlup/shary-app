package com.shary.app.infrastructure.di


import android.content.Context
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.security.Kdf
import com.shary.app.infrastructure.security.derivation.kdf.Pbkdf2Kdf
import com.shary.app.infrastructure.security.derivation.hkdf.HkdfSha256
import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.core.domain.interfaces.security.Ed25519Factory
import com.shary.app.core.domain.interfaces.security.FieldCodec
import com.shary.app.core.domain.security.Box
import com.shary.app.infrastructure.security.local.FieldCodecVault
import com.shary.app.infrastructure.security.local.LocalVault
import com.shary.app.infrastructure.security.manager.CryptographyManagerImpl
import com.shary.app.infrastructure.security.digitalSignature.BcEd25519Factory
import com.shary.app.infrastructure.security.digitalSignature.Ed25519DetachedVerifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//@Qualifier
//annotation class AppId

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides @Singleton fun provideAesGcmBox(
        hkdf: HkdfSha256,
        cipher: AesGcmCipher
    ): Box = AesGcmBox(hkdf, cipher)

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

    @Provides @Singleton fun provideEd25519Factory(
    ): Ed25519Factory = BcEd25519Factory()

    @Provides @Singleton fun provideFieldCodecVault(
        vault: LocalVault,
        authenticationService: AuthenticationService
    ): FieldCodec = FieldCodecVault(vault, authenticationService)
}