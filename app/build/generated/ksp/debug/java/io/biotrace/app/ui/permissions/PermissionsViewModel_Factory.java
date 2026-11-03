package io.biotrace.app.ui.permissions;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.healthconnect.HealthConnectManager;
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
public final class PermissionsViewModel_Factory implements Factory<PermissionsViewModel> {
  private final Provider<HealthConnectManager> healthConnectManagerProvider;

  public PermissionsViewModel_Factory(Provider<HealthConnectManager> healthConnectManagerProvider) {
    this.healthConnectManagerProvider = healthConnectManagerProvider;
  }

  @Override
  public PermissionsViewModel get() {
    return newInstance(healthConnectManagerProvider.get());
  }

  public static PermissionsViewModel_Factory create(
      Provider<HealthConnectManager> healthConnectManagerProvider) {
    return new PermissionsViewModel_Factory(healthConnectManagerProvider);
  }

  public static PermissionsViewModel newInstance(HealthConnectManager healthConnectManager) {
    return new PermissionsViewModel(healthConnectManager);
  }
}
