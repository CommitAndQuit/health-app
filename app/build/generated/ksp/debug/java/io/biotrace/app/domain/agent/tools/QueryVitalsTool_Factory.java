package io.biotrace.app.domain.agent.tools;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.domain.repository.VitalsRepository;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class QueryVitalsTool_Factory implements Factory<QueryVitalsTool> {
  private final Provider<VitalsRepository> vitalsRepositoryProvider;

  public QueryVitalsTool_Factory(Provider<VitalsRepository> vitalsRepositoryProvider) {
    this.vitalsRepositoryProvider = vitalsRepositoryProvider;
  }

  @Override
  public QueryVitalsTool get() {
    return newInstance(vitalsRepositoryProvider.get());
  }

  public static QueryVitalsTool_Factory create(
      Provider<VitalsRepository> vitalsRepositoryProvider) {
    return new QueryVitalsTool_Factory(vitalsRepositoryProvider);
  }

  public static QueryVitalsTool newInstance(VitalsRepository vitalsRepository) {
    return new QueryVitalsTool(vitalsRepository);
  }
}
