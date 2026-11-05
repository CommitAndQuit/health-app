package io.biotrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.biotrace.app.data.local.entity.StressSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<StressSampleEntity>)

    @Query("SELECT * FROM stress_samples ORDER BY timestampMillis DESC")
    suspend fun getAllStressSamples(): List<StressSampleEntity>

    @Query("SELECT * FROM stress_samples ORDER BY timestampMillis DESC")
    fun getAllStressSamplesFlow(): Flow<List<StressSampleEntity>>

    @Query("""
        SELECT * FROM stress_samples 
        WHERE timestampMillis >= :startMillis AND timestampMillis <= :endMillis 
        ORDER BY timestampMillis ASC
    """)
    suspend fun getStressSamplesBetween(startMillis: Long, endMillis: Long): List<StressSampleEntity>

    @Query("""
        SELECT * FROM stress_samples 
        WHERE timestampMillis >= :startMillis AND timestampMillis <= :endMillis 
        ORDER BY timestampMillis ASC
    """)
    fun getStressSamplesBetweenFlow(startMillis: Long, endMillis: Long): Flow<List<StressSampleEntity>>

    @Query("SELECT * FROM stress_samples ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestStressSample(): StressSampleEntity?

    @Query("SELECT * FROM stress_samples ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestStressSampleFlow(): Flow<StressSampleEntity?>

    @Query("""
        SELECT MIN(stress) FROM stress_samples 
        WHERE timestampMillis >= :startMillis AND timestampMillis <= :endMillis AND stress > 0
    """)
    suspend fun getMinStress(startMillis: Long, endMillis: Long): Int?

    @Query("""
        SELECT MAX(stress) FROM stress_samples 
        WHERE timestampMillis >= :startMillis AND timestampMillis <= :endMillis AND stress > 0
    """)
    suspend fun getMaxStress(startMillis: Long, endMillis: Long): Int?

    @Query("""
        SELECT AVG(stress) FROM stress_samples 
        WHERE timestampMillis >= :startMillis AND timestampMillis <= :endMillis AND stress > 0
    """)
    suspend fun getAvgStress(startMillis: Long, endMillis: Long): Double?

    @Query("SELECT COUNT(*) FROM stress_samples")
    suspend fun getStressSampleCount(): Int

    @Query("SELECT COUNT(*) FROM stress_samples")
    fun getStressSampleCountFlow(): Flow<Int>

    @Query("DELETE FROM stress_samples")
    suspend fun deleteAll()
}
