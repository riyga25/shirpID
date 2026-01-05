package by.riyga.shirpid.data.database.dao

import androidx.room.*
import by.riyga.shirpid.data.models.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<Record>>
    
    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): Record?
    
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
    
    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordByTimestamp(id: Long)

    @Query("DELETE FROM records WHERE id IN (:ids)")
    suspend fun deleteRecordsByTimestamp(ids: List<Long>)
    
    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()
}