package by.riyga.shirpid.data.database

import by.riyga.shirpid.data.database.dao.RecordDao
import by.riyga.shirpid.data.models.Record
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun getAllRecords(): Flow<List<Record>>
    suspend fun getRecordById(id: Long): Record?
    suspend fun getRecordCount(): Int
    suspend fun insertRecord(record: Record): Long
    suspend fun insertRecords(records: List<Record>)
    suspend fun updateRecord(record: Record)
    suspend fun deleteRecord(record: Record)
    suspend fun deleteRecordById(id: Long)
    suspend fun deleteRecordsById(ids: List<Long>)
    suspend fun deleteAllRecords()
}

class RecordRepositoryImpl(
    private val recordDao: RecordDao
) : RecordRepository {
    
    override fun getAllRecords(): Flow<List<Record>> =
        recordDao.getAllRecords()
    
    override suspend fun getRecordById(id: Long): Record? =
        recordDao.getRecordById(id)
    
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
    
    override suspend fun deleteRecordById(id: Long) =
        recordDao.deleteRecordByTimestamp(id)

    override suspend fun deleteRecordsById(ids: List<Long>) {
        recordDao.deleteRecordsByTimestamp(ids)
    }

    override suspend fun deleteAllRecords() =
        recordDao.deleteAllRecords()
}