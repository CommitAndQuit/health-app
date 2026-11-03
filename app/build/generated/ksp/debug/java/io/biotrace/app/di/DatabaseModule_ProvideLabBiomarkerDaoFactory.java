package io.biotrace.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.LabBiomarkerDao;
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
public final class DatabaseModule_ProvideLabBiomarkerDaoFactory implements Factory<LabBiomarkerDao> {
  private final Provider<BioTraceDatabase> databaseProvider;

  public DatabaseModule_ProvideLabBiomarkerDaoFactory(Provider<BioTraceDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LabBiomarkerDao get() {
    return provideLabBiomarkerDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideLabBiomarkerDaoFactory create(
      Provider<BioTraceDatabase> databaseProvider) {
    return new DatabaseModule_ProvideLabBiomarkerDaoFactory(databaseProvider);
  }

  public static LabBiomarkerDao provideLabBiomarkerDao(BioTraceDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLabBiomarkerDao(database));
  }
}
