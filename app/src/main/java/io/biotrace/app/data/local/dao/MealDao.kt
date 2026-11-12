package io.biotrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.biotrace.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealEntity>)

    @Query(
        """
        SELECT * FROM meals 
        WHERE consumed_at >= :startTime AND consumed_at <= :endTime 
        ORDER BY consumed_at DESC
        """
    )
    fun getMealsBetweenFlow(startTime: Instant, endTime: Instant): Flow<List<MealEntity>>

    @Query(
        """
        SELECT * FROM meals 
        WHERE consumed_at >= :startTime AND consumed_at <= :endTime 
        ORDER BY consumed_at DESC
        """
    )
    suspend fun getMealsBetween(startTime: Instant, endTime: Instant): List<MealEntity>

    @Query("SELECT * FROM meals ORDER BY consumed_at DESC LIMIT 1")
    suspend fun getLatestMeal(): MealEntity?

    @Query("SELECT * FROM meals ORDER BY consumed_at DESC LIMIT 1")
    fun getLatestMealFlow(): Flow<MealEntity?>

    @Query("DELETE FROM meals")
    suspend fun deleteAll()
}
