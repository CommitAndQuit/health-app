package io.biotrace.app.domain.agent.tools;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.biotrace.app.data.local.dao.LabBiomarkerDao;
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
public final class GetActiveAnomaliesTool_Factory implements Factory<GetActiveAnomaliesTool> {
  private final Provider<LabBiomarkerDao> labBiomarkerDaoProvider;

  public GetActiveAnomaliesTool_Factory(Provider<LabBiomarkerDao> labBiomarkerDaoProvider) {
    this.labBiomarkerDaoProvider = labBiomarkerDaoProvider;
  }

  @Override
  public GetActiveAnomaliesTool get() {
    return newInstance(labBiomarkerDaoProvider.get());
  }

  public static GetActiveAnomaliesTool_Factory create(
      Provider<LabBiomarkerDao> labBiomarkerDaoProvider) {
    return new GetActiveAnomaliesTool_Factory(labBiomarkerDaoProvider);
  }

  public static GetActiveAnomaliesTool newInstance(LabBiomarkerDao labBiomarkerDao) {
    return new GetActiveAnomaliesTool(labBiomarkerDao);
  }
}
