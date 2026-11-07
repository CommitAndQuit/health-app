package io.biotrace.app.ui.insights

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.biotrace.app.domain.analytics.correlation.MetricPairwiseCorrelator
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InsightsModule {
    @Provides
    @Singleton
    fun provideMetricPairwiseCorrelator(): MetricPairwiseCorrelator = MetricPairwiseCorrelator

    @Provides
    @Singleton
    fun provideCircadianDecompositionService(): CircadianDecompositionService = CircadianDecompositionService
}
