package io.biotrace.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.ChatHistoryDao;
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
public final class DatabaseModule_ProvideChatHistoryDaoFactory implements Factory<ChatHistoryDao> {
  private final Provider<BioTraceDatabase> databaseProvider;

  public DatabaseModule_ProvideChatHistoryDaoFactory(Provider<BioTraceDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatHistoryDao get() {
    return provideChatHistoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideChatHistoryDaoFactory create(
      Provider<BioTraceDatabase> databaseProvider) {
    return new DatabaseModule_ProvideChatHistoryDaoFactory(databaseProvider);
  }

  public static ChatHistoryDao provideChatHistoryDao(BioTraceDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChatHistoryDao(database));
  }
}
