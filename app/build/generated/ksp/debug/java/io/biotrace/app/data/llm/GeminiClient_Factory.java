package io.biotrace.app.data.llm;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class GeminiClient_Factory implements Factory<GeminiClient> {
  private final Provider<KeystoreManager> keystoreManagerProvider;

  public GeminiClient_Factory(Provider<KeystoreManager> keystoreManagerProvider) {
    this.keystoreManagerProvider = keystoreManagerProvider;
  }

  @Override
  public GeminiClient get() {
    return newInstance(keystoreManagerProvider.get());
  }

  public static GeminiClient_Factory create(Provider<KeystoreManager> keystoreManagerProvider) {
    return new GeminiClient_Factory(keystoreManagerProvider);
  }

  public static GeminiClient newInstance(KeystoreManager keystoreManager) {
    return new GeminiClient(keystoreManager);
  }
}
