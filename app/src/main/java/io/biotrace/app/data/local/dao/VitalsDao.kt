package io.biotrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface VitalsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VitalsRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<VitalsRecordEntity>)

    @Query(
        """
        SELECT * FROM vitals_records 
        WHERE metric_type = :metricType 
          AND timestamp >= :startTime 
          AND timestamp <= :endTime 
        ORDER BY timestamp ASC
        """
    )
    fun getRecordsFlow(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<VitalsRecordEntity>>

    @Query(
        """
        SELECT * FROM vitals_records 
        WHERE metric_type = :metricType 
          AND timestamp >= :startTime 
          AND timestamp <= :endTime 
        ORDER BY timestamp ASC
        """
    )
    suspend fun getRecordsBetween(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): List<VitalsRecordEntity>

    @Query(
        """
        SELECT * FROM vitals_records 
        WHERE metric_type = :metricType 
        ORDER BY timestamp DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestRecord(metricType: String): VitalsRecordEntity?

    @Query(
        """
        SELECT * FROM vitals_records 
        WHERE metric_type = :metricType 
        ORDER BY timestamp DESC 
        LIMIT 1
        """
    )
    fun getLatestRecordFlow(metricType: String): Flow<VitalsRecordEntity?>

    @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<VitalsRecordEntity>

    @Query("DELETE FROM vitals_records WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant): Int

    @Query("DELETE FROM vitals_records")
    suspend fun deleteAll()
}
