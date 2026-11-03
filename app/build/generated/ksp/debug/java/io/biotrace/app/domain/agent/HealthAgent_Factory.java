package io.biotrace.app.domain.agent;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.domain.agent.tools.GetActiveAnomaliesTool;
import io.biotrace.app.domain.agent.tools.GetNutritionIntakeTool;
import io.biotrace.app.domain.agent.tools.QueryLabBiomarkersTool;
import io.biotrace.app.domain.agent.tools.QueryVitalsTool;
import io.biotrace.app.domain.llm.LlmClient;
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
public final class HealthAgent_Factory implements Factory<HealthAgent> {
  private final Provider<LlmClient> llmClientProvider;

  private final Provider<QueryVitalsTool> queryVitalsToolProvider;

  private final Provider<QueryLabBiomarkersTool> queryLabBiomarkersToolProvider;

  private final Provider<GetActiveAnomaliesTool> getActiveAnomaliesToolProvider;

  private final Provider<GetNutritionIntakeTool> getNutritionIntakeToolProvider;

  public HealthAgent_Factory(Provider<LlmClient> llmClientProvider,
      Provider<QueryVitalsTool> queryVitalsToolProvider,
      Provider<QueryLabBiomarkersTool> queryLabBiomarkersToolProvider,
      Provider<GetActiveAnomaliesTool> getActiveAnomaliesToolProvider,
      Provider<GetNutritionIntakeTool> getNutritionIntakeToolProvider) {
    this.llmClientProvider = llmClientProvider;
    this.queryVitalsToolProvider = queryVitalsToolProvider;
    this.queryLabBiomarkersToolProvider = queryLabBiomarkersToolProvider;
    this.getActiveAnomaliesToolProvider = getActiveAnomaliesToolProvider;
    this.getNutritionIntakeToolProvider = getNutritionIntakeToolProvider;
  }

  @Override
  public HealthAgent get() {
    return newInstance(llmClientProvider.get(), queryVitalsToolProvider.get(), queryLabBiomarkersToolProvider.get(), getActiveAnomaliesToolProvider.get(), getNutritionIntakeToolProvider.get());
  }

  public static HealthAgent_Factory create(Provider<LlmClient> llmClientProvider,
      Provider<QueryVitalsTool> queryVitalsToolProvider,
      Provider<QueryLabBiomarkersTool> queryLabBiomarkersToolProvider,
      Provider<GetActiveAnomaliesTool> getActiveAnomaliesToolProvider,
      Provider<GetNutritionIntakeTool> getNutritionIntakeToolProvider) {
    return new HealthAgent_Factory(llmClientProvider, queryVitalsToolProvider, queryLabBiomarkersToolProvider, getActiveAnomaliesToolProvider, getNutritionIntakeToolProvider);
  }

  public static HealthAgent newInstance(LlmClient llmClient, QueryVitalsTool queryVitalsTool,
      QueryLabBiomarkersTool queryLabBiomarkersTool, GetActiveAnomaliesTool getActiveAnomaliesTool,
      GetNutritionIntakeTool getNutritionIntakeTool) {
    return new HealthAgent(llmClient, queryVitalsTool, queryLabBiomarkersTool, getActiveAnomaliesTool, getNutritionIntakeTool);
  }
}
