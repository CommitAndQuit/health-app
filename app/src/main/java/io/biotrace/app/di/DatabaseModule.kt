package io.biotrace.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.biotrace.app.data.local.dao.ChatHistoryDao
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.dao.StressDao
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.database.BioTraceDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBioTraceDatabase(
        @ApplicationContext context: Context
    ): BioTraceDatabase {
        return Room.databaseBuilder(
            context,
            BioTraceDatabase::class.java,
            BioTraceDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideVitalsDao(database: BioTraceDatabase): VitalsDao {
        return database.vitalsDao()
    }

    @Provides
    fun provideStressDao(database: BioTraceDatabase): StressDao {
        return database.stressDao()
    }

    @Provides
    fun provideLabBiomarkerDao(database: BioTraceDatabase): LabBiomarkerDao {
        return database.labBiomarkerDao()
    }

    @Provides
    fun provideMealDao(database: BioTraceDatabase): MealDao {
        return database.mealDao()
    }

    @Provides
    fun provideChatHistoryDao(database: BioTraceDatabase): ChatHistoryDao {
        return database.chatHistoryDao()
    }
}
