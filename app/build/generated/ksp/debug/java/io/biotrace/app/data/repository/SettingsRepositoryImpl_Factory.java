package io.biotrace.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.database.BioTraceDatabase;
import io.biotrace.app.domain.security.KeystoreManager;
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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<KeystoreManager> keystoreManagerProvider;

  private final Provider<BioTraceDatabase> databaseProvider;

  public SettingsRepositoryImpl_Factory(Provider<KeystoreManager> keystoreManagerProvider,
      Provider<BioTraceDatabase> databaseProvider) {
    this.keystoreManagerProvider = keystoreManagerProvider;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(keystoreManagerProvider.get(), databaseProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(
      Provider<KeystoreManager> keystoreManagerProvider,
      Provider<BioTraceDatabase> databaseProvider) {
    return new SettingsRepositoryImpl_Factory(keystoreManagerProvider, databaseProvider);
  }

  public static SettingsRepositoryImpl newInstance(KeystoreManager keystoreManager,
      BioTraceDatabase database) {
    return new SettingsRepositoryImpl(keystoreManager, database);
  }
}
