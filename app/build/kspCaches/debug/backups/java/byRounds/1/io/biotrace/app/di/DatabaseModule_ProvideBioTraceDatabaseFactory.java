package io.biotrace.app.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.database.BioTraceDatabase;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideBioTraceDatabaseFactory implements Factory<BioTraceDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideBioTraceDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BioTraceDatabase get() {
    return provideBioTraceDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideBioTraceDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideBioTraceDatabaseFactory(contextProvider);
  }

  public static BioTraceDatabase provideBioTraceDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBioTraceDatabase(context));
  }
}
