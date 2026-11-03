package io.biotrace.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.biotrace.app.data.repository.ChatRepositoryImpl
import io.biotrace.app.data.repository.SettingsRepositoryImpl
import io.biotrace.app.data.repository.VitalsRepositoryImpl
import io.biotrace.app.domain.repository.ChatRepository
import io.biotrace.app.domain.repository.SettingsRepository
import io.biotrace.app.domain.repository.VitalsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVitalsRepository(
        impl: VitalsRepositoryImpl
    ): VitalsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
