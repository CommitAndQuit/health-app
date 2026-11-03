package io.biotrace.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.biotrace.app.data.security.KeystoreManagerImpl
import io.biotrace.app.domain.security.KeystoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeystoreManager(
        impl: KeystoreManagerImpl
    ): KeystoreManager
}
