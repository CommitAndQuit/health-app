package io.biotrace.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.healthconnect.HealthConnectSyncService;
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
public final class VitalsRepositoryImpl_Factory implements Factory<VitalsRepositoryImpl> {
  private final Provider<VitalsDao> vitalsDaoProvider;

  private final Provider<HealthConnectSyncService> syncServiceProvider;

  public VitalsRepositoryImpl_Factory(Provider<VitalsDao> vitalsDaoProvider,
      Provider<HealthConnectSyncService> syncServiceProvider) {
    this.vitalsDaoProvider = vitalsDaoProvider;
    this.syncServiceProvider = syncServiceProvider;
  }

  @Override
  public VitalsRepositoryImpl get() {
    return newInstance(vitalsDaoProvider.get(), syncServiceProvider.get());
  }

  public static VitalsRepositoryImpl_Factory create(Provider<VitalsDao> vitalsDaoProvider,
      Provider<HealthConnectSyncService> syncServiceProvider) {
    return new VitalsRepositoryImpl_Factory(vitalsDaoProvider, syncServiceProvider);
  }

  public static VitalsRepositoryImpl newInstance(VitalsDao vitalsDao,
      HealthConnectSyncService syncService) {
    return new VitalsRepositoryImpl(vitalsDao, syncService);
  }
}
