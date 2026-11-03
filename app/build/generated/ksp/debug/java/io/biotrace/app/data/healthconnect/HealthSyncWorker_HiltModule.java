package io.biotrace.app.data.healthconnect;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = HealthSyncWorker.class
)
public interface HealthSyncWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("io.biotrace.app.data.healthconnect.HealthSyncWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(HealthSyncWorker_AssistedFactory factory);
}
