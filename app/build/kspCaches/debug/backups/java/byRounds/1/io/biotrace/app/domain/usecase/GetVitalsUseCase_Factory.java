package io.biotrace.app.domain.usecase;

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
public final class GetVitalsUseCase_Factory implements Factory<GetVitalsUseCase> {
  private final Provider<VitalsRepository> vitalsRepositoryProvider;

  public GetVitalsUseCase_Factory(Provider<VitalsRepository> vitalsRepositoryProvider) {
    this.vitalsRepositoryProvider = vitalsRepositoryProvider;
  }

  @Override
  public GetVitalsUseCase get() {
    return newInstance(vitalsRepositoryProvider.get());
  }

  public static GetVitalsUseCase_Factory create(
      Provider<VitalsRepository> vitalsRepositoryProvider) {
    return new GetVitalsUseCase_Factory(vitalsRepositoryProvider);
  }

  public static GetVitalsUseCase newInstance(VitalsRepository vitalsRepository) {
    return new GetVitalsUseCase(vitalsRepository);
  }
}
