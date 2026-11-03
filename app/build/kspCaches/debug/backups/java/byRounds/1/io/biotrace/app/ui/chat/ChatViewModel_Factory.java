package io.biotrace.app.ui.chat;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.domain.agent.HealthAgent;
import io.biotrace.app.domain.llm.LlmClient;
import io.biotrace.app.domain.repository.ChatRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<HealthAgent> healthAgentProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<LlmClient> llmClientProvider;

  public ChatViewModel_Factory(Provider<HealthAgent> healthAgentProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<LlmClient> llmClientProvider) {
    this.healthAgentProvider = healthAgentProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.llmClientProvider = llmClientProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(healthAgentProvider.get(), chatRepositoryProvider.get(), llmClientProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<HealthAgent> healthAgentProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<LlmClient> llmClientProvider) {
    return new ChatViewModel_Factory(healthAgentProvider, chatRepositoryProvider, llmClientProvider);
  }

  public static ChatViewModel newInstance(HealthAgent healthAgent, ChatRepository chatRepository,
      LlmClient llmClient) {
    return new ChatViewModel(healthAgent, chatRepository, llmClient);
  }
}
