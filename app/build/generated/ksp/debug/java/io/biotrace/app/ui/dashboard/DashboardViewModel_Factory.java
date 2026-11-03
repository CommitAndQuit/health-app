package io.biotrace.app.ui.dashboard;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.healthconnect.HealthConnectManager;
import io.biotrace.app.data.local.dao.VitalsDao;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<VitalsRepository> vitalsRepositoryProvider;

  private final Provider<VitalsDao> vitalsDaoProvider;

  private final Provider<HealthConnectManager> healthConnectManagerProvider;

  public DashboardViewModel_Factory(Provider<VitalsRepository> vitalsRepositoryProvider,
      Provider<VitalsDao> vitalsDaoProvider,
      Provider<HealthConnectManager> healthConnectManagerProvider) {
    this.vitalsRepositoryProvider = vitalsRepositoryProvider;
    this.vitalsDaoProvider = vitalsDaoProvider;
    this.healthConnectManagerProvider = healthConnectManagerProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(vitalsRepositoryProvider.get(), vitalsDaoProvider.get(), healthConnectManagerProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<VitalsRepository> vitalsRepositoryProvider, Provider<VitalsDao> vitalsDaoProvider,
      Provider<HealthConnectManager> healthConnectManagerProvider) {
    return new DashboardViewModel_Factory(vitalsRepositoryProvider, vitalsDaoProvider, healthConnectManagerProvider);
  }

  public static DashboardViewModel newInstance(VitalsRepository vitalsRepository,
      VitalsDao vitalsDao, HealthConnectManager healthConnectManager) {
    return new DashboardViewModel(vitalsRepository, vitalsDao, healthConnectManager);
  }
}
