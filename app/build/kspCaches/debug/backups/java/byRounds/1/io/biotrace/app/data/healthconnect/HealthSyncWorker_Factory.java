package io.biotrace.app.data.healthconnect;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class HealthSyncWorker_Factory {
  private final Provider<HealthConnectSyncService> syncServiceProvider;

  public HealthSyncWorker_Factory(Provider<HealthConnectSyncService> syncServiceProvider) {
    this.syncServiceProvider = syncServiceProvider;
  }

  public HealthSyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, syncServiceProvider.get());
  }

  public static HealthSyncWorker_Factory create(
      Provider<HealthConnectSyncService> syncServiceProvider) {
    return new HealthSyncWorker_Factory(syncServiceProvider);
  }

  public static HealthSyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      HealthConnectSyncService syncService) {
    return new HealthSyncWorker(appContext, workerParams, syncService);
  }
}
