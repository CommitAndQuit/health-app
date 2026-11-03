package io.biotrace.app.ui.labs;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.LabBiomarkerDao;
import io.biotrace.app.domain.llm.LlmClient;
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
public final class LabsViewModel_Factory implements Factory<LabsViewModel> {
  private final Provider<LabBiomarkerDao> labBiomarkerDaoProvider;

  private final Provider<LlmClient> llmClientProvider;

  public LabsViewModel_Factory(Provider<LabBiomarkerDao> labBiomarkerDaoProvider,
      Provider<LlmClient> llmClientProvider) {
    this.labBiomarkerDaoProvider = labBiomarkerDaoProvider;
    this.llmClientProvider = llmClientProvider;
  }

  @Override
  public LabsViewModel get() {
    return newInstance(labBiomarkerDaoProvider.get(), llmClientProvider.get());
  }

  public static LabsViewModel_Factory create(Provider<LabBiomarkerDao> labBiomarkerDaoProvider,
      Provider<LlmClient> llmClientProvider) {
    return new LabsViewModel_Factory(labBiomarkerDaoProvider, llmClientProvider);
  }

  public static LabsViewModel newInstance(LabBiomarkerDao labBiomarkerDao, LlmClient llmClient) {
    return new LabsViewModel(labBiomarkerDao, llmClient);
  }
}
