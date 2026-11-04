package io.biotrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabBiomarkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(biomarker: LabBiomarkerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(biomarkers: List<LabBiomarkerEntity>)

    @Query("SELECT * FROM lab_biomarkers ORDER BY recorded_at DESC")
    suspend fun getAll(): List<LabBiomarkerEntity>

    @Query("SELECT * FROM lab_biomarkers ORDER BY recorded_at DESC")
    fun getAllFlow(): Flow<List<LabBiomarkerEntity>>

    @Query("SELECT * FROM lab_biomarkers WHERE biomarker_name = :biomarkerName ORDER BY recorded_at DESC")
    fun getByNameFlow(biomarkerName: String): Flow<List<LabBiomarkerEntity>>

    @Query("SELECT * FROM lab_biomarkers WHERE biomarker_name = :biomarkerName ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestByName(biomarkerName: String): LabBiomarkerEntity?

    @Query("SELECT * FROM lab_biomarkers WHERE flag IN ('HIGH', 'LOW') ORDER BY recorded_at DESC")
    suspend fun getAnomalies(): List<LabBiomarkerEntity>

    @Query("DELETE FROM lab_biomarkers")
    suspend fun deleteAll()
}
