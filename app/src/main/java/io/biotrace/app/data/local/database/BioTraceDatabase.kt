package io.biotrace.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.biotrace.app.data.local.converter.Converters
import io.biotrace.app.data.local.dao.ChatHistoryDao
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.entity.ChatMessageEntity
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.data.local.entity.VitalsRecordEntity

@Database(
    entities = [
        VitalsRecordEntity::class,
        LabBiomarkerEntity::class,
        MealEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BioTraceDatabase : RoomDatabase() {

    abstract fun vitalsDao(): VitalsDao
    abstract fun labBiomarkerDao(): LabBiomarkerDao
    abstract fun mealDao(): MealDao
    abstract fun chatHistoryDao(): ChatHistoryDao

    companion object {
        const val DATABASE_NAME = "biotrace.db"
    }
}
