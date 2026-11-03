package io.biotrace.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.MealDao;
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
public final class DatabaseModule_ProvideMealDaoFactory implements Factory<MealDao> {
  private final Provider<BioTraceDatabase> databaseProvider;

  public DatabaseModule_ProvideMealDaoFactory(Provider<BioTraceDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MealDao get() {
    return provideMealDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMealDaoFactory create(
      Provider<BioTraceDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMealDaoFactory(databaseProvider);
  }

  public static MealDao provideMealDao(BioTraceDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMealDao(database));
  }
}
