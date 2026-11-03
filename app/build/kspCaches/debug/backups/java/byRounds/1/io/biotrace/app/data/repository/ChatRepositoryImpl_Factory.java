package io.biotrace.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.ChatHistoryDao;
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<ChatHistoryDao> chatHistoryDaoProvider;

  public ChatRepositoryImpl_Factory(Provider<ChatHistoryDao> chatHistoryDaoProvider) {
    this.chatHistoryDaoProvider = chatHistoryDaoProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(chatHistoryDaoProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<ChatHistoryDao> chatHistoryDaoProvider) {
    return new ChatRepositoryImpl_Factory(chatHistoryDaoProvider);
  }

  public static ChatRepositoryImpl newInstance(ChatHistoryDao chatHistoryDao) {
    return new ChatRepositoryImpl(chatHistoryDao);
  }
}
