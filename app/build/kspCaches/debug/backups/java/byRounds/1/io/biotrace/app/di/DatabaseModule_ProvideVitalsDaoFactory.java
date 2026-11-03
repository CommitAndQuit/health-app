package io.biotrace.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.VitalsDao;
import io.biotrace.app.data.local.database.BioTraceDatabase;
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
public final class DatabaseModule_ProvideVitalsDaoFactory implements Factory<VitalsDao> {
  private final Provider<BioTraceDatabase> databaseProvider;

  public DatabaseModule_ProvideVitalsDaoFactory(Provider<BioTraceDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public VitalsDao get() {
    return provideVitalsDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideVitalsDaoFactory create(
      Provider<BioTraceDatabase> databaseProvider) {
    return new DatabaseModule_ProvideVitalsDaoFactory(databaseProvider);
  }

  public static VitalsDao provideVitalsDao(BioTraceDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideVitalsDao(database));
  }
}
