package io.biotrace.app.domain.agent.tools;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.MealDao;
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
public final class GetNutritionIntakeTool_Factory implements Factory<GetNutritionIntakeTool> {
  private final Provider<MealDao> mealDaoProvider;

  public GetNutritionIntakeTool_Factory(Provider<MealDao> mealDaoProvider) {
    this.mealDaoProvider = mealDaoProvider;
  }

  @Override
  public GetNutritionIntakeTool get() {
    return newInstance(mealDaoProvider.get());
  }

  public static GetNutritionIntakeTool_Factory create(Provider<MealDao> mealDaoProvider) {
    return new GetNutritionIntakeTool_Factory(mealDaoProvider);
  }

  public static GetNutritionIntakeTool newInstance(MealDao mealDao) {
    return new GetNutritionIntakeTool(mealDao);
  }
}
