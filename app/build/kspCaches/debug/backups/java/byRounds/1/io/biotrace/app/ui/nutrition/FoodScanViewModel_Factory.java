package io.biotrace.app.ui.nutrition;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.MealDao;
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
public final class FoodScanViewModel_Factory implements Factory<FoodScanViewModel> {
  private final Provider<MealDao> mealDaoProvider;

  private final Provider<LlmClient> llmClientProvider;

  public FoodScanViewModel_Factory(Provider<MealDao> mealDaoProvider,
      Provider<LlmClient> llmClientProvider) {
    this.mealDaoProvider = mealDaoProvider;
    this.llmClientProvider = llmClientProvider;
  }

  @Override
  public FoodScanViewModel get() {
    return newInstance(mealDaoProvider.get(), llmClientProvider.get());
  }

  public static FoodScanViewModel_Factory create(Provider<MealDao> mealDaoProvider,
      Provider<LlmClient> llmClientProvider) {
    return new FoodScanViewModel_Factory(mealDaoProvider, llmClientProvider);
  }

  public static FoodScanViewModel newInstance(MealDao mealDao, LlmClient llmClient) {
    return new FoodScanViewModel(mealDao, llmClient);
  }
}
