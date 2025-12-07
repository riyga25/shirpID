package com.riyga.identifier.data.database

import androidx.room.*
import com.riyga.identifier.data.models.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<Record>>
    
    @Query("SELECT * FROM records WHERE timestamp = :timestamp")
    suspend fun getRecordByTimestamp(timestamp: Long): Record?
    
    @Query("SELECT COUNT(*) FROM records")
    suspend fun getRecordCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: Record): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<Record>)
    
    @Update
    suspend fun updateRecord(record: Record)
    
    @Delete
    suspend fun deleteRecord(record: Record)
    
    @Query("DELETE FROM records WHERE timestamp = :timestamp")
    suspend fun deleteRecordByTimestamp(timestamp: Long)
    
    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()
}