package io.biotrace.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.biotrace.app.data.llm.GeminiClient
import io.biotrace.app.domain.llm.LlmClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlmClient(
        impl: GeminiClient
    ): LlmClient
}
