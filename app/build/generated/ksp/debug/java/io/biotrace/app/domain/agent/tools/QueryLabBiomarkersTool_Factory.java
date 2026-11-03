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
public final class QueryLabBiomarkersTool_Factory implements Factory<QueryLabBiomarkersTool> {
  private final Provider<LabBiomarkerDao> labBiomarkerDaoProvider;

  public QueryLabBiomarkersTool_Factory(Provider<LabBiomarkerDao> labBiomarkerDaoProvider) {
    this.labBiomarkerDaoProvider = labBiomarkerDaoProvider;
  }

  @Override
  public QueryLabBiomarkersTool get() {
    return newInstance(labBiomarkerDaoProvider.get());
  }

  public static QueryLabBiomarkersTool_Factory create(
      Provider<LabBiomarkerDao> labBiomarkerDaoProvider) {
    return new QueryLabBiomarkersTool_Factory(labBiomarkerDaoProvider);
  }

  public static QueryLabBiomarkersTool newInstance(LabBiomarkerDao labBiomarkerDao) {
    return new QueryLabBiomarkersTool(labBiomarkerDao);
  }
}
