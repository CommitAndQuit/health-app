package io.biotrace.app.data.healthconnect;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.VitalsDao;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HealthConnectSyncService_Factory implements Factory<HealthConnectSyncService> {
  private final Provider<HealthConnectManager> healthConnectManagerProvider;

  private final Provider<VitalsDao> vitalsDaoProvider;

  public HealthConnectSyncService_Factory(
      Provider<HealthConnectManager> healthConnectManagerProvider,
      Provider<VitalsDao> vitalsDaoProvider) {
    this.healthConnectManagerProvider = healthConnectManagerProvider;
    this.vitalsDaoProvider = vitalsDaoProvider;
  }

  @Override
  public HealthConnectSyncService get() {
    return newInstance(healthConnectManagerProvider.get(), vitalsDaoProvider.get());
  }

  public static HealthConnectSyncService_Factory create(
      Provider<HealthConnectManager> healthConnectManagerProvider,
      Provider<VitalsDao> vitalsDaoProvider) {
    return new HealthConnectSyncService_Factory(healthConnectManagerProvider, vitalsDaoProvider);
  }

  public static HealthConnectSyncService newInstance(HealthConnectManager healthConnectManager,
      VitalsDao vitalsDao) {
    return new HealthConnectSyncService(healthConnectManager, vitalsDao);
  }
}
