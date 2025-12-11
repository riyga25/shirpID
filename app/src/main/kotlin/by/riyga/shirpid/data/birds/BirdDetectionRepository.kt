package by.riyga.shirpid.data.birds

import by.riyga.shirpid.data.database.RecordDao
import by.riyga.shirpid.data.models.Record
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun getAllRecords(): Flow<List<Record>>
    suspend fun getRecordByTimestamp(timestamp: Long): Record?
    suspend fun getRecordCount(): Int
    suspend fun insertRecord(record: Record): Long
    suspend fun insertRecords(records: List<Record>)
    suspend fun updateRecord(record: Record)
    suspend fun deleteRecord(record: Record)
    suspend fun deleteRecordByTimestamp(timestamp: Long)
    suspend fun deleteAllRecords()
}

class RecordRepositoryImpl(
    private val recordDao: RecordDao
) : RecordRepository {
    
    override fun getAllRecords(): Flow<List<Record>> =
        recordDao.getAllRecords()
    
    override suspend fun getRecordByTimestamp(timestamp: Long): Record? =
        recordDao.getRecordByTimestamp(timestamp)
    
    override suspend fun getRecordCount(): Int =
        recordDao.getRecordCount()
    
    override suspend fun insertRecord(record: Record): Long =
        recordDao.insertRecord(record)
    
    override suspend fun insertRecords(records: List<Record>) =
        recordDao.insertRecords(records)
    
    override suspend fun updateRecord(record: Record) =
        recordDao.updateRecord(record)
    
    override suspend fun deleteRecord(record: Record) =
        recordDao.deleteRecord(record)
    
    override suspend fun deleteRecordByTimestamp(timestamp: Long) =
        recordDao.deleteRecordByTimestamp(timestamp)
    
    override suspend fun deleteAllRecords() =
        recordDao.deleteAllRecords()
}